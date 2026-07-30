package ru.uoles.kafka.sender.model;

import java.util.List;
import java.util.UUID;

/** Порция сообщений Kafka-потребителя для браузерного опроса. */
public record ConsumerMessagesResponse(
        UUID consumerId,
        List<ConsumerMessageResponse> messages,
        long oldestSequence,
        long nextSequence,
        long droppedCount) {
}
