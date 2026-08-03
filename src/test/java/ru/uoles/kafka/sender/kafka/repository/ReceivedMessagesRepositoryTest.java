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

import ru.uoles.kafka.sender.kafka.utils.HeaderUtils;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceivedMessagesRepository JDBC operations")
class ReceivedMessagesRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ReceivedMessagesRepository repository;

    @Test
    @DisplayName("saves a message with timestamp and serialized headers")
    void savesMessage() {
        UUID consumerId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(2_345_678L);
        List<ConsumerMessageResponse.ConsumerHeaderResponse> headers = List.of(
                new ConsumerMessageResponse.ConsumerHeaderResponse("content-type", "application/json"));
        ConsumerMessageResponse message = new ConsumerMessageResponse(
                5L, "key", "value", "orders", 2, 31L, timestamp, headers);

        repository.saveMessage(consumerId, message);

        verify(jdbcTemplate).update(eqSql("INSERT OR REPLACE INTO received_messages"),
                eq(consumerId.toString()), eq(5L), eq("key"), eq("value"), eq("orders"), eq(2), eq(31L),
                eq(2_345_678L), eq(HeaderUtils.serializeHeaders(headers)));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("deletes one message by consumer UUID and sequence")
    void deletesMessage() {
        UUID consumerId = UUID.randomUUID();

        repository.deleteMessage(consumerId, 7L);

        verify(jdbcTemplate).update(eqSql("DELETE FROM received_messages", "consumer_id = ? AND sequence = ?"),
                eq(consumerId.toString()), eq(7L));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("loads messages and restores fields, timestamp, and headers from database columns")
    void findsAndMapsMessages() throws Exception {
        UUID consumerId = UUID.randomUUID();
        Instant timestamp = Instant.ofEpochMilli(3_456_789L);
        List<ConsumerMessageResponse.ConsumerHeaderResponse> headers = List.of(
                new ConsumerMessageResponse.ConsumerHeaderResponse("trace-id", "abc-123"));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("sequence")).thenReturn(9L);
        when(resultSet.getString("message_key")).thenReturn(null);
        when(resultSet.getString("message_value")).thenReturn("payload");
        when(resultSet.getString("topic")).thenReturn("events");
        when(resultSet.getInt("partition_number")).thenReturn(1);
        when(resultSet.getLong("message_offset")).thenReturn(44L);
        when(resultSet.getLong("timestamp")).thenReturn(timestamp.toEpochMilli());
        when(resultSet.getString("headers_json")).thenReturn(HeaderUtils.serializeHeaders(headers));

        List<ConsumerMessageResponse> expected = List.of();
        doAnswer(invocation -> {
            RowMapper<ConsumerMessageResponse> mapper = invocation.getArgument(1);
            assertThat(mapper.mapRow(resultSet, 0)).isEqualTo(new ConsumerMessageResponse(
                    9L, null, "payload", "events", 1, 44L, timestamp, headers));
            return expected;
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());

        assertThat(repository.findMessages(consumerId)).isSameAs(expected);
        verify(jdbcTemplate).query(eqSql("SELECT sequence, message_key", "WHERE consumer_id = ?", "ORDER BY sequence"),
                any(RowMapper.class), eq(consumerId.toString()));
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("restores an empty header list from persisted JSON")
    void mapsEmptyHeaders() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("sequence")).thenReturn(1L);
        when(resultSet.getString("message_key")).thenReturn(null);
        when(resultSet.getString("message_value")).thenReturn(null);
        when(resultSet.getString("topic")).thenReturn(null);
        when(resultSet.getInt("partition_number")).thenReturn(0);
        when(resultSet.getLong("message_offset")).thenReturn(0L);
        when(resultSet.getLong("timestamp")).thenReturn(0L);
        when(resultSet.getString("headers_json")).thenReturn("[]");

        doAnswer(invocation -> {
            RowMapper<ConsumerMessageResponse> mapper = invocation.getArgument(1);
            ConsumerMessageResponse mapped = mapper.mapRow(resultSet, 0);
            assertThat(mapped.headers()).isEmpty();
            return List.of(mapped);
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString());

        assertThat(repository.findMessages(UUID.randomUUID()))
                .singleElement()
                .satisfies(message -> assertThat(message.headers()).isEmpty());
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
