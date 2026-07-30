package ru.uoles.kafka.sender.model;

import java.time.Instant;
import java.util.List;

/** Сообщение, полученное Kafka-потребителем. */
public record ConsumerMessageResponse(
        long sequence,
        String key,
        String value,
        String topic,
        int partition,
        long offset,
        Instant timestamp,
        List<ConsumerHeaderResponse> headers) {

    /** Заголовок полученного Kafka-сообщения. */
    public record ConsumerHeaderResponse(String name, String value) {
    }
}
