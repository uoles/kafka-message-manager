package ru.uoles.kafka.sender.kafka.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.uoles.kafka.sender.enums.ConsumerStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Репозиторий SQLite для конфигурации консьюмеров и журнала сообщений. */
@Repository
@RequiredArgsConstructor
public class ConsumersRepository {

    private final JdbcTemplate jdbcTemplate;

    /** SQL-запрос для добавления или обновления состояния потребителя. */
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

    /** SQL-запрос для загрузки всех сохранённых потребителей. */
    private static final String SQL_FIND_ALL_CONSUMERS = """
            SELECT id, bootstrap_address, topic, group_id, created_at, status, last_error, dropped_count,
                next_sequence
            FROM consumers
            ORDER BY created_at, id
            """;

    /** SQL-запрос для удаления потребителя и каскадного удаления его сообщений. */
    private static final String SQL_DELETE_CONSUMER_BY_ID = """
            DELETE FROM consumers WHERE id = ?
            """;

    private static final String SQL_SAVE_CONSUMERS = """
            UPDATE consumers
            SET dropped_count = ?, next_sequence = ?
            WHERE id = ?
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

    /** Сохраняет новое состояние последовательности консьюмера. */
    public void saveConsumer(UUID consumerId, long droppedCount, long nextSequence) {
        jdbcTemplate.update(SQL_SAVE_CONSUMERS, droppedCount, nextSequence, consumerId.toString());
    }

    private ConsumerRecord mapConsumer(ResultSet resultSet, int row) throws SQLException {
        return new ConsumerRecord(UUID.fromString(resultSet.getString("id")),
                resultSet.getString("bootstrap_address"), resultSet.getString("topic"),
                resultSet.getString("group_id"), Instant.ofEpochMilli(resultSet.getLong("created_at")),
                ConsumerStatus.valueOf(resultSet.getString("status")), resultSet.getString("last_error"),
                resultSet.getLong("dropped_count"), resultSet.getLong("next_sequence"));
    }
}
