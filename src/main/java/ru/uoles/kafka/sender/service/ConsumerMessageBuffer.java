package ru.uoles.kafka.sender.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Потокобезопасный ограниченный буфер сообщений потребителя. */
final class ConsumerMessageBuffer {
    private final int capacity;
    private final ArrayDeque<ConsumerMessageResponse> messages = new ArrayDeque<>();
    private long nextSequence = 1;
    private long droppedCount;

    ConsumerMessageBuffer(int capacity) {
        this.capacity = capacity;
    }

    synchronized void add(ConsumerRecord<String, String> record) {
        List<ConsumerMessageResponse.ConsumerHeaderResponse> headers = new ArrayList<>();
        record.headers().forEach(header -> headers.add(new ConsumerMessageResponse.ConsumerHeaderResponse(
                header.key(), header.value() == null ? "" : new String(header.value(), StandardCharsets.UTF_8))));
        if (messages.size() == capacity) {
            messages.removeFirst();
            droppedCount++;
        }
        messages.addLast(new ConsumerMessageResponse(nextSequence++, record.key(), record.value(), record.topic(),
                record.partition(), record.offset(), record.timestamp() == 0 ? Instant.now() : Instant.ofEpochMilli(record.timestamp()), headers));
    }

    synchronized List<ConsumerMessageResponse> after(long sequence, int limit) {
        return messages.stream().filter(message -> message.sequence() > sequence).limit(limit).toList();
    }

    synchronized long oldestSequence() { return messages.isEmpty() ? nextSequence : messages.peekFirst().sequence(); }
    synchronized long nextSequence() { return nextSequence; }
    synchronized long droppedCount() { return droppedCount; }
    synchronized long size() { return messages.size(); }
}
