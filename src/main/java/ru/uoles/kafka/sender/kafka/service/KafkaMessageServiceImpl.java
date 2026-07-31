package ru.uoles.kafka.sender.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.uoles.kafka.sender.kafka.utils.HeaderUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Сервис формирования Kafka-продюсера и отправки сообщений.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageServiceImpl implements KafkaMessageService {

    /**
     * Отправляет сообщение в указанный топик с необязательными заголовками.
     *
     * @param topic название Kafka-топика
     * @param kafkaAddress адрес Kafka-брокера
     * @param messageText текст сообщения
     * @param headers заголовки в формате name=value,name2=value2
     * @throws ExecutionException если Kafka сообщает об ошибке выполнения отправки
     */
    @Override
    public void sendMessage(String topic, String kafkaAddress, String messageText, String headers) throws ExecutionException {
        List<Header> parsedHeaders = HeaderUtils.parse(headers);
        log.info("Creating producer for topic: {}, kafka address: {}", topic, kafkaAddress);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        props.put(ProducerConfig.LINGER_MS_CONFIG, 100);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(props);
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, messageText);
            parsedHeaders.forEach(header -> record.headers().add(header));
            var result = kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
            log.info("Message sent successfully to topic: {}, partition: {}, offset: {}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending message to Kafka", exception);
        } catch (TimeoutException exception) {
            throw new RuntimeException("Timed out while sending message to Kafka", exception);
        } finally {
            kafkaTemplate.destroy();
            producerFactory.destroy();
        }
    }
}
