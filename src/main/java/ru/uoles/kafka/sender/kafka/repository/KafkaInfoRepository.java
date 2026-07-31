package ru.uoles.kafka.sender.kafka.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;
import ru.uoles.kafka.sender.enums.ConsumerStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Репозиторий SQLite для конфигурации консьюмеров и журнала сообщений. */
@Repository
@RequiredArgsConstructor
public class KafkaInfoRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String SQL_SAVE_CONSUMER = """
            INSERT INTO consumers(id, bootstrap_address, topic, group_id, created_at, status, last_error,
                dropped_count, next_sequence)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE
                SET status=excluded.status,
                    last_error=excluded.last_error,
                    dropped_count=excluded.dropped_count,
                    next_sequence=excluded.next_sequence
            """;

    private static final String SQL_FIND_ALL_CONSUMERS = """
            SELECT id, bootstrap_address, topic, group_id, created_at, status, last_error, dropped_count,
                next_sequence
            FROM consumers
            ORDER BY created_at, id
            """;

    private static final String SQL_DELETE_CONSUMER_BY_ID = """
            DELETE FROM consumers WHERE id = ?
            """;

    private static final String SQL_SAVE_MESSAGE = """
            INSERT OR REPLACE INTO messages(consumer_id, sequence, message_key, message_value, topic,
                partition_number, message_offset, timestamp, headers_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_SAVE_CONSUMERS = """
            UPDATE consumers
            SET dropped_count = ?, next_sequence = ?
            WHERE id = ?
            """;

    private static final String SQL_DELETE_MESSAGES = """
            DELETE FROM messages
            WHERE consumer_id = ? AND sequence = ?
            """;

    private static final String SQL_FIND_MESSAGES_BY_CONSUMER_ID = """
            SELECT sequence, message_key, message_value, topic, partition_number, message_offset, timestamp,
                headers_json
            FROM messages
            WHERE consumer_id = ?
            ORDER BY sequence
            """;

    /** Снимок сохранённого консьюмера. */
    public record ConsumerRecord(UUID id, String bootstrapAddress, String topic, String groupId,
                                 Instant createdAt, ConsumerStatus status, String lastError,
                                 long droppedCount, long nextSequence) {
    }

    /** Сохраняет новый консьюмер или обновляет его состояние. */
    public void saveConsumer(ConsumerRecord consumer) {
        jdbcTemplate.update(SQL_SAVE_CONSUMER, consumer.id().toString(), consumer.bootstrapAddress(),
                consumer.topic(), consumer.groupId(), consumer.createdAt().toEpochMilli(),
                consumer.status().name(), consumer.lastError(), consumer.droppedCount(),
                consumer.nextSequence());
    }

    /** Загружает все сохранённые определения консьюмеров. */
    public List<ConsumerRecord> findAllConsumers() {
        return jdbcTemplate.query(SQL_FIND_ALL_CONSUMERS, this::mapConsumer);
    }

    /** Удаляет консьюмера и связанные сообщения каскадно. */
    public void deleteConsumer(UUID id) {
        jdbcTemplate.update(SQL_DELETE_CONSUMER_BY_ID, id.toString());
    }

    /** Сохраняет сообщение и новое состояние последовательности консьюмера. */
    public void saveMessage(UUID consumerId, ConsumerMessageResponse message, long droppedCount, long nextSequence) {
        jdbcTemplate.update(SQL_SAVE_MESSAGE, consumerId.toString(), message.sequence(), message.key(),
                message.value(), message.topic(), message.partition(), message.offset(),
                message.timestamp().toEpochMilli(), serializeHeaders(message.headers()));

        jdbcTemplate.update(SQL_SAVE_CONSUMERS, droppedCount, nextSequence, consumerId.toString());
    }

    /** Удаляет из базы сообщение, вытесненное ограниченным буфером. */
    public void deleteMessage(UUID consumerId, long sequence, long droppedCount, long nextSequence) {
        jdbcTemplate.update(SQL_DELETE_MESSAGES, consumerId.toString(), sequence);
        jdbcTemplate.update(SQL_SAVE_CONSUMERS, droppedCount, nextSequence, consumerId.toString());
    }

    /** Загружает сохранённые сообщения консьюмера в порядке sequence. */
    public List<ConsumerMessageResponse> findMessages(UUID consumerId) {
        return jdbcTemplate.query(SQL_FIND_MESSAGES_BY_CONSUMER_ID, this::mapMessage, consumerId.toString());
    }

    private ConsumerRecord mapConsumer(ResultSet resultSet, int row) throws SQLException {
        return new ConsumerRecord(UUID.fromString(resultSet.getString("id")),
                resultSet.getString("bootstrap_address"), resultSet.getString("topic"),
                resultSet.getString("group_id"), Instant.ofEpochMilli(resultSet.getLong("created_at")),
                ConsumerStatus.valueOf(resultSet.getString("status")), resultSet.getString("last_error"),
                resultSet.getLong("dropped_count"), resultSet.getLong("next_sequence"));
    }

    private ConsumerMessageResponse mapMessage(ResultSet resultSet, int row) throws SQLException {
        return new ConsumerMessageResponse(resultSet.getLong("sequence"), resultSet.getString("message_key"),
                resultSet.getString("message_value"), resultSet.getString("topic"),
                resultSet.getInt("partition_number"), resultSet.getLong("message_offset"),
                Instant.ofEpochMilli(resultSet.getLong("timestamp")),
                deserializeHeaders(resultSet.getString("headers_json")));
    }

    private String serializeHeaders(List<ConsumerMessageResponse.ConsumerHeaderResponse> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Kafka headers", exception);
        }
    }

    private List<ConsumerMessageResponse.ConsumerHeaderResponse> deserializeHeaders(String headers) {
        try {
            return objectMapper.readValue(headers, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize Kafka headers", exception);
        }
    }
}
