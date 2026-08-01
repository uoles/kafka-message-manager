package ru.uoles.kafka.sender.kafka.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Component;
import ru.uoles.kafka.sender.enums.ConsumerStatus;
import ru.uoles.kafka.sender.kafka.repository.ConsumersRepository;
import ru.uoles.kafka.sender.kafka.service.ConsumersInfoService;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;
import ru.uoles.kafka.sender.model.ConsumerMessagesResponse;
import ru.uoles.kafka.sender.model.ConsumerResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Управляет динамическими Kafka-потребителями и их сохранением в SQLite. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerManager {

    /** Максимальное количество одновременно запущенных потребителей. */
    private static final int MAX_CONSUMERS = 5;

    /** Максимальный размер одной страницы при чтении сообщений. */
    private static final int MAX_PAGE_SIZE = 200;

    /** Максимальное количество сообщений в оперативном буфере потребителя. */
    private final int bufferCapacity = 500;

    /** Сервис сохранения конфигураций потребителей и полученных сообщений. */
    private final ConsumersInfoService consumersInfoService;

    /** Запущенные потребители, индексированные по идентификатору. */
    private final Map<UUID, ManagedConsumer> consumers = new ConcurrentHashMap<>();

    /** Восстанавливает сохранённых потребителей после создания Spring-контекста. */
    @PostConstruct
    public synchronized void restoreConsumers() {
        consumersInfoService.findAllConsumers().forEach(record -> {
            try {
                ConsumerMessageBuffer buffer = new ConsumerMessageBuffer(bufferCapacity, consumersInfoService.findMessages(record.id()), record.nextSequence(), record.droppedCount());
                ManagedConsumer managed = startManaged(record.id(), record.bootstrapAddress(), record.topic(), record.groupId(), record.createdAt(), buffer);
                consumers.put(record.id(), managed);
                managed.status = ConsumerStatus.RUNNING;
                consumersInfoService.saveConsumer(managed.record());
            } catch (RuntimeException exception) {
                log.error("Failed to restore consumer {}", record.id(), exception);
                consumersInfoService.saveConsumer(new ConsumersRepository.ConsumerRecord(record.id(), record.bootstrapAddress(), record.topic(), record.groupId(), record.createdAt(), ConsumerStatus.ERROR, safeError(exception), record.droppedCount(), record.nextSequence()));
            }
        });
    }

    /** Создаёт и запускает нового потребителя, сохраняя его описание. */
    public synchronized ConsumerResponse create(String bootstrapAddress, String topic) {
        if (consumers.size() >= MAX_CONSUMERS) throw new ConsumerLimitException();
        UUID id = UUID.randomUUID();
        String groupId = "kafka-message-manager-" + id;
        Instant createdAt = Instant.now();
        ConsumerMessageBuffer buffer = new ConsumerMessageBuffer(bufferCapacity);
        ConsumersRepository.ConsumerRecord initial = new ConsumersRepository.ConsumerRecord(id, bootstrapAddress, topic, groupId, createdAt, ConsumerStatus.STARTING, null, 0, 1);
        consumersInfoService.saveConsumer(initial);
        try {
            ManagedConsumer managed = startManaged(id, bootstrapAddress, topic, groupId, createdAt, buffer);
            consumers.put(id, managed);
            managed.status = ConsumerStatus.RUNNING;
            consumersInfoService.saveConsumer(managed.record());
            return managed.response();
        } catch (RuntimeException exception) {
            consumersInfoService.deleteConsumer(id);
            throw new ConsumerStartException("Failed to start Kafka consumer", exception);
        }
    }

    public List<ConsumerResponse> list() { return consumers.values().stream().map(ManagedConsumer::response).toList(); }
    public ConsumerResponse get(UUID id) { return require(id).response(); }

    public ConsumerMessagesResponse messages(UUID id, long after, int limit) {
        if (after < 0) throw new IllegalArgumentException("after must not be negative");
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        ManagedConsumer consumer = require(id);
        List<ConsumerMessageResponse> messages = consumer.buffer.after(after, Math.min(limit, MAX_PAGE_SIZE));
        return new ConsumerMessagesResponse(id, messages, consumer.buffer.oldestSequence(), consumer.buffer.nextSequence(), consumer.buffer.droppedCount());
    }

    /** Останавливает потребителя и удаляет его журнал из базы. */
    public synchronized void delete(UUID id) {
        ManagedConsumer consumer = require(id);
        consumers.remove(id);
        consumer.status = ConsumerStatus.STOPPED;
        destroy(consumer);
        consumersInfoService.deleteConsumer(id);
    }

    /** Останавливает контейнеры, оставляя данные для следующего запуска. */
    @PreDestroy
    public synchronized void shutdown() {
        new ArrayList<>(consumers.values()).forEach(consumer -> {
            consumers.remove(consumer.id);
            consumer.status = ConsumerStatus.STOPPED;
            destroy(consumer);
        });
    }

    private ManagedConsumer startManaged(UUID id, String bootstrapAddress, String topic, String groupId,
                                         Instant createdAt, ConsumerMessageBuffer buffer) {
        Map<String, Object> props = Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
                ConsumerConfig.GROUP_ID_CONFIG, groupId, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        DefaultKafkaConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);
        ManagedConsumer managed = new ManagedConsumer(id, bootstrapAddress, topic, groupId, createdAt, buffer, factory);
        ContainerProperties properties = new ContainerProperties(topic);
        properties.setMessageListener((org.springframework.kafka.listener.MessageListener<String, String>) record -> {
            ConsumerMessageBuffer.AddResult result = managed.buffer.add(record);
            try {
                // Журнал в SQLite сохраняется полностью, даже если запись вытеснена из оперативного буфера.
                consumersInfoService.saveMessage(id, result.added(), result.droppedCount(), result.nextSequence());
            } catch (RuntimeException exception) {
                managed.lastError = safeError(exception);
                managed.status = ConsumerStatus.ERROR;
                log.error("Failed to persist message for consumer {}", id, exception);
            }
        });
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(factory, properties);
        managed.container = container;
        try {
            container.start();
            return managed;
        } catch (RuntimeException exception) {
            destroy(managed);
            throw exception;
        }
    }

    private void destroy(ManagedConsumer consumer) {
        try {
            if (consumer.container != null) consumer.container.stop();
        } catch (RuntimeException exception) {
            log.warn("Failed to stop consumer {}", consumer.id, exception);
        } finally {
            consumer.factory.getListeners().clear();
        }
    }

    private ManagedConsumer require(UUID id) {
        ManagedConsumer consumer = consumers.get(id);
        if (consumer == null) throw new ConsumerNotFoundException(id);
        return consumer;
    }

    private static String safeError(Throwable exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    public static class ConsumerNotFoundException extends RuntimeException { public ConsumerNotFoundException(UUID id) { super("Consumer not found: " + id); } }
    public static class ConsumerLimitException extends RuntimeException { public ConsumerLimitException() { super("Maximum number of consumers reached"); } }
    public static class ConsumerStartException extends RuntimeException { public ConsumerStartException(String message, Throwable cause) { super(message, cause); } }

    private final class ManagedConsumer {
        private final UUID id;
        private final String bootstrapAddress;
        private final String topic;
        private final String groupId;
        private final Instant createdAt;
        private final ConsumerMessageBuffer buffer;
        private final DefaultKafkaConsumerFactory<String, String> factory;
        private KafkaMessageListenerContainer<String, String> container;
        private volatile ConsumerStatus status = ConsumerStatus.STARTING;
        private volatile String lastError;

        private ManagedConsumer(UUID id, String bootstrapAddress, String topic, String groupId, Instant createdAt,
                                ConsumerMessageBuffer buffer, DefaultKafkaConsumerFactory<String, String> factory) {
            this.id = id; this.bootstrapAddress = bootstrapAddress; this.topic = topic; this.groupId = groupId;
            this.createdAt = createdAt; this.buffer = buffer; this.factory = factory;
        }

        private ConsumersRepository.ConsumerRecord record() {
            return new ConsumersRepository.ConsumerRecord(id, bootstrapAddress, topic, groupId, createdAt, status, lastError, buffer.droppedCount(), buffer.nextSequence());
        }

        private ConsumerResponse response() {
            return new ConsumerResponse(id, bootstrapAddress, topic, groupId, status, createdAt, buffer.size(), buffer.droppedCount(), lastError);
        }
    }
}
