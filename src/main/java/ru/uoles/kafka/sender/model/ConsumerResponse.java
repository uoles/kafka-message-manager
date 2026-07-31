package ru.uoles.kafka.sender.model;

import ru.uoles.kafka.sender.enums.ConsumerStatus;

import java.time.Instant;
import java.util.UUID;

/** Сведения о созданном Kafka-потребителе. */
public record ConsumerResponse(
        UUID id,
        String bootstrapAddress,
        String topic,
        String groupId,
        ConsumerStatus status,
        Instant createdAt,
        long bufferedCount,
        long droppedCount,
        String lastError) {
}
