package ru.uoles.kafka.sender.kafka.consumer;

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
    private long nextSequence;
    private long droppedCount;

    /** Результат добавления сообщения, включая вытесленную запись. */
    record AddResult(ConsumerMessageResponse added, ConsumerMessageResponse evicted, long droppedCount, long nextSequence) {
    }

    ConsumerMessageBuffer(int capacity) {
        this(capacity, List.of(), 1, 0);
    }

    ConsumerMessageBuffer(int capacity, List<ConsumerMessageResponse> restoredMessages,
                          long nextSequence, long droppedCount) {
        this.capacity = capacity;
        this.droppedCount = Math.max(0, droppedCount);
        this.nextSequence = Math.max(1, nextSequence);
        restoredMessages.stream().skip(Math.max(0, restoredMessages.size() - capacity)).forEach(messages::addLast);
        if (!messages.isEmpty()) this.nextSequence = Math.max(this.nextSequence, messages.peekLast().sequence() + 1);
    }

    /** Добавляет запись, назначает локальную последовательность и учитывает переполнение. */
    synchronized AddResult add(ConsumerRecord<String, String> record) {
        List<ConsumerMessageResponse.ConsumerHeaderResponse> headers = new ArrayList<>();
        record.headers().forEach(header -> headers.add(new ConsumerMessageResponse.ConsumerHeaderResponse(
                header.key(), header.value() == null ? "" : new String(header.value(), StandardCharsets.UTF_8))));
        ConsumerMessageResponse message = new ConsumerMessageResponse(nextSequence++, record.key(), record.value(),
                record.topic(), record.partition(), record.offset(),
                record.timestamp() == 0 ? Instant.now() : Instant.ofEpochMilli(record.timestamp()), headers);
        ConsumerMessageResponse evicted = messages.size() == capacity ? messages.removeFirst() : null;
        if (evicted != null) droppedCount++;
        messages.addLast(message);
        return new AddResult(message, evicted, droppedCount, nextSequence);
    }

    synchronized List<ConsumerMessageResponse> after(long sequence, int limit) {
        return messages.stream().filter(message -> message.sequence() > sequence).limit(limit).toList();
    }

    synchronized long oldestSequence() { return messages.isEmpty() ? nextSequence : messages.peekFirst().sequence(); }
    synchronized long nextSequence() { return nextSequence; }
    synchronized long droppedCount() { return droppedCount; }
    synchronized long size() { return messages.size(); }
}
