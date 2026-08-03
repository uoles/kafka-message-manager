package ru.uoles.kafka.sender.kafka.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.uoles.kafka.sender.enums.ConsumerStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumersRepository JDBC operations")
class ConsumersRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ConsumersRepository repository;

    @Test
    @DisplayName("saves consumer definition and state using the upsert parameters")
    void savesConsumer() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochMilli(1_234_567L);
        ConsumersRepository.ConsumerRecord consumer = new ConsumersRepository.ConsumerRecord(
                id, "localhost:9092", "orders", "group-1", createdAt, ConsumerStatus.RUNNING,
                "last failure", 3L, 8L);

        repository.saveConsumer(consumer);

        verify(jdbcTemplate).update(
                eqSql("INSERT INTO consumers", "ON CONFLICT(id) DO UPDATE"),
                eq(id.toString()), eq("localhost:9092"), eq("orders"), eq("group-1"),
                eq(1_234_567L), eq("RUNNING"), eq("last failure"), eq(3L), eq(8L));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("loads all consumers and maps database columns to a consumer record")
    void findsAndMapsConsumers() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochMilli(987_654L);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("id")).thenReturn(id.toString());
        when(resultSet.getString("bootstrap_address")).thenReturn("localhost:29092");
        when(resultSet.getString("topic")).thenReturn("events");
        when(resultSet.getString("group_id")).thenReturn("group-2");
        when(resultSet.getLong("created_at")).thenReturn(createdAt.toEpochMilli());
        when(resultSet.getString("status")).thenReturn("ERROR");
        when(resultSet.getString("last_error")).thenReturn(null);
        when(resultSet.getLong("dropped_count")).thenReturn(4L);
        when(resultSet.getLong("next_sequence")).thenReturn(12L);

        List<ConsumersRepository.ConsumerRecord> expected = List.of();
        doAnswer(invocation -> {
            RowMapper<ConsumersRepository.ConsumerRecord> mapper = invocation.getArgument(1);
            assertThat(mapper.mapRow(resultSet, 0)).isEqualTo(new ConsumersRepository.ConsumerRecord(
                    id, "localhost:29092", "events", "group-2", createdAt, ConsumerStatus.ERROR,
                    null, 4L, 12L));
            return expected;
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class));

        assertThat(repository.findAllConsumers()).isSameAs(expected);
        verify(jdbcTemplate).query(eqSql("SELECT id, bootstrap_address", "ORDER BY created_at, id"),
                any(RowMapper.class));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("deletes a consumer by its string UUID")
    void deletesConsumer() {
        UUID id = UUID.randomUUID();

        repository.deleteConsumer(id);

        verify(jdbcTemplate).update(eqSql("DELETE FROM consumers WHERE id = ?"), eq(id.toString()));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("updates dropped count and next sequence in parameter order")
    void updatesConsumerState() {
        UUID id = UUID.randomUUID();

        repository.saveConsumer(id, 6L, 19L);

        verify(jdbcTemplate).update(eqSql("UPDATE consumers", "SET dropped_count = ?, next_sequence = ?"),
                eq(6L), eq(19L), eq(id.toString()));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    private static String eqSql(String... fragments) {
        return org.mockito.ArgumentMatchers.argThat(sql -> {
            String normalized = sql.replaceAll("\\s+", " ").trim();
            for (String fragment : fragments) {
                if (!normalized.contains(fragment.replaceAll("\\s+", " ").trim())) {
                    return false;
                }
            }
            return true;
        });
    }
}
