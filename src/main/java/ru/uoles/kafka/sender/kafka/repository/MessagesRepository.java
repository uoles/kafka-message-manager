package ru.uoles.kafka.sender.kafka.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ru.uoles.kafka.sender.kafka.utils.HeaderUtils.deserializeHeaders;
import static ru.uoles.kafka.sender.kafka.utils.HeaderUtils.serializeHeaders;

/** Репозиторий SQLite для журнала сообщений консьюмеров. */
@Repository
@RequiredArgsConstructor
public class MessagesRepository {

    private final JdbcTemplate jdbcTemplate;

    /** SQL-запрос для сохранения полученного сообщения. */
    private static final String SQL_SAVE_MESSAGE = """
            INSERT OR REPLACE INTO messages(consumer_id, sequence, message_key, message_value, topic,
                partition_number, message_offset, timestamp, headers_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** SQL-запрос для удаления сообщения из журнала потребителя. */
    private static final String SQL_DELETE_MESSAGES = """
            DELETE FROM messages
            WHERE consumer_id = ? AND sequence = ?
            """;

    /** SQL-запрос для загрузки сообщений конкретного потребителя. */
    private static final String SQL_FIND_MESSAGES_BY_CONSUMER_ID = """
            SELECT sequence, message_key, message_value, topic, partition_number, message_offset, timestamp,
                headers_json
            FROM messages
            WHERE consumer_id = ?
            ORDER BY sequence
            """;

    /** Сохраняет сообщение и новое состояние последовательности консьюмера. */
    public void saveMessage(UUID consumerId, ConsumerMessageResponse message) {
        jdbcTemplate.update(SQL_SAVE_MESSAGE, consumerId.toString(), message.sequence(), message.key(),
                message.value(), message.topic(), message.partition(), message.offset(),
                message.timestamp().toEpochMilli(), serializeHeaders(message.headers()));
    }

    /** Удаляет из базы сообщение, вытесненное ограниченным буфером. */
    public void deleteMessage(UUID consumerId, long sequence) {
        jdbcTemplate.update(SQL_DELETE_MESSAGES, consumerId.toString(), sequence);
    }

    /** Загружает сохранённые сообщения консьюмера в порядке sequence. */
    public List<ConsumerMessageResponse> findMessages(UUID consumerId) {
        return jdbcTemplate.query(SQL_FIND_MESSAGES_BY_CONSUMER_ID, this::mapMessage, consumerId.toString());
    }

    private ConsumerMessageResponse mapMessage(ResultSet resultSet, int row) throws SQLException {
        return new ConsumerMessageResponse(resultSet.getLong("sequence"), resultSet.getString("message_key"),
                resultSet.getString("message_value"), resultSet.getString("topic"),
                resultSet.getInt("partition_number"), resultSet.getLong("message_offset"),
                Instant.ofEpochMilli(resultSet.getLong("timestamp")),
                deserializeHeaders(resultSet.getString("headers_json")));
    }
}
