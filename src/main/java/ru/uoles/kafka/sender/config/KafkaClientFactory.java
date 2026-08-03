package ru.uoles.kafka.sender.config;

import java.util.Map;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Создает ресурсы продюсера Kafka, ограниченные областью действия запроса и используемые службой сообщений. */
@Component
public class KafkaClientFactory {

    /** Создает фабрику продюсеров и шаблон, используя предоставленные свойства Kafka. */
    public KafkaClient create(Map<String, Object> properties) {
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(properties);
        return new KafkaClient(producerFactory, new KafkaTemplate<>(producerFactory));
    }

    /** Пара ресурсов, которые необходимо уничтожить после попытки отправки. */
    public record KafkaClient(DefaultKafkaProducerFactory<String, String> producerFactory,
                              KafkaTemplate<String, String> kafkaTemplate) {
    }
}
