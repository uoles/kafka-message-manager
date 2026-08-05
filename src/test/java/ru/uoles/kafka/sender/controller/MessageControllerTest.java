package ru.uoles.kafka.sender.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.uoles.kafka.sender.enums.ConsumerStatus;
import ru.uoles.kafka.sender.kafka.consumer.ConsumerManager;
import ru.uoles.kafka.sender.kafka.service.KafkaMessageService;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;
import ru.uoles.kafka.sender.model.ConsumerMessagesResponse;
import ru.uoles.kafka.sender.model.ConsumerResponse;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static RequestPostProcessor userJwt() {
        return request -> {
            Jwt token = Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .subject(TEST_USER_ID.toString())
                    .claim("roles", List.of("ROLE_USER"))
                    .build();
            request.setUserPrincipal(new JwtAuthenticationToken(token,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }

    private static final String SEND_BODY = """
            {
              "topic": "events",
              "kafkaAddress": "localhost:9092",
              "messageText": "hello",
              "headers": "trace-id=abc"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaMessageService kafkaMessageService;

    @MockBean
    private ConsumerManager consumerManager;

    @Test
    void sendMessage_validRequest_returnsSuccessAndForwardsArguments() throws Exception {
        long before = System.currentTimeMillis();

        mockMvc.perform(post("/api/kafka/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SEND_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Message sent successfully to topic: events"))
                .andExpect(jsonPath("$.topic").value("events"))
                .andExpect(jsonPath("$.kafkaAddress").value("localhost:9092"))
                .andExpect(jsonPath("$.timestamp").isNumber());

        long after = System.currentTimeMillis();
        verify(kafkaMessageService).sendMessage("events", "localhost:9092", "hello", "trace-id=abc");
        verifyNoInteractions(consumerManager);
        // The response timestamp is generated during request handling.
        assertThat(before).isLessThanOrEqualTo(after);
    }

    @Test
    void sendMessage_serviceFailure_returnsInternalServerError() throws Exception {
        doThrow(new ExecutionException(new RuntimeException("broker unavailable")))
                .when(kafkaMessageService)
                .sendMessage("events", "localhost:9092", "hello", "trace-id=abc");

        mockMvc.perform(post("/api/kafka/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SEND_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Failed to send message: java.lang.RuntimeException: broker unavailable"))
                .andExpect(jsonPath("$.topic").value("events"))
                .andExpect(jsonPath("$.kafkaAddress").value("localhost:9092"))
                .andExpect(jsonPath("$.timestamp").isNumber());

        verify(kafkaMessageService).sendMessage("events", "localhost:9092", "hello", "trace-id=abc");
    }

    @Test
    void sendMessage_invalidHeadersException_isReturnedAsInternalServerError() throws Exception {
        doThrow(new IllegalArgumentException("Invalid header"))
                .when(kafkaMessageService)
                .sendMessage("events", "localhost:9092", "hello", "trace-id=abc");

        mockMvc.perform(post("/api/kafka/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SEND_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to send message: Invalid header"));
    }

    @Test
    void sendMessage_invalidRequest_returnsBadRequestWithoutSending() throws Exception {
        mockMvc.perform(post("/api/kafka/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\" \",\"kafkaAddress\":\"localhost:9092\",\"messageText\":\"hello\"}"))
                .andExpect(status().isBadRequest());

        verify(kafkaMessageService, never()).sendMessage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendMessage_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/kafka/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(kafkaMessageService);
    }

    @Test
    void handleInvalidHeaders_returnsBadRequest() {
        MessageController controller = new MessageController(kafkaMessageService, consumerManager);

        var response = controller.handleInvalidHeaders(new IllegalArgumentException("Invalid header"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody())
                .extracting("status", "message", "topic", "kafkaAddress")
                .containsExactly("error", "Invalid header", null, null);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void healthCheck_returnsHealthyResponse() throws Exception {
        mockMvc.perform(get("/api/kafka/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Kafka Producer Service is running"))
                .andExpect(jsonPath("$.topic").doesNotExist())
                .andExpect(jsonPath("$.kafkaAddress").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNumber());

        verifyNoInteractions(kafkaMessageService, consumerManager);
    }

    @Test
    void createConsumer_validRequest_returnsCreatedConsumer() throws Exception {
        UUID id = UUID.randomUUID();
        ConsumerResponse response = new ConsumerResponse(id, "localhost:9092", "events", "group", ConsumerStatus.RUNNING,
                Instant.parse("2026-01-01T00:00:00Z"), 0, 0, null);
        when(consumerManager.create(TEST_USER_ID, "localhost:9092", "events")).thenReturn(response);

        mockMvc.perform(post("/api/kafka/consumers")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bootstrapAddress\":\"localhost:9092\",\"topic\":\"events\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.topic").value("events"))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(consumerManager).create(TEST_USER_ID, "localhost:9092", "events");
    }

    @Test
    void createConsumer_invalidRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/kafka/consumers")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bootstrapAddress\":\"bad\",\"topic\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(consumerManager, never()).create(any(UUID.class), eq("bad"), eq(""));
    }

    @Test
    void listConsumers_returnsManagerResult() throws Exception {
        when(consumerManager.list(TEST_USER_ID, false)).thenReturn(List.of());

        mockMvc.perform(get("/api/kafka/consumers").with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(consumerManager).list(TEST_USER_ID, false);
    }

    @Test
    void getConsumer_returnsConsumer() throws Exception {
        UUID id = UUID.randomUUID();
        when(consumerManager.get(id, TEST_USER_ID, false)).thenReturn(new ConsumerResponse(id, "localhost:9092", "events", "group",
                ConsumerStatus.RUNNING, Instant.parse("2026-01-01T00:00:00Z"), 2, 1, null));

        mockMvc.perform(get("/api/kafka/consumers/{id}", id).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.bufferedCount").value(2));

        verify(consumerManager).get(id, TEST_USER_ID, false);
    }

    @Test
    void getConsumerMessages_usesDefaultCursorAndLimit() throws Exception {
        UUID id = UUID.randomUUID();
        ConsumerMessagesResponse response = new ConsumerMessagesResponse(id, List.<ConsumerMessageResponse>of(), 1, 1, 0);
        when(consumerManager.messages(id, TEST_USER_ID, false, 0, 100)).thenReturn(response);

        mockMvc.perform(get("/api/kafka/consumers/{id}/messages", id).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumerId").value(id.toString()))
                .andExpect(jsonPath("$.messages").isEmpty());

        verify(consumerManager).messages(id, TEST_USER_ID, false, 0, 100);
    }

    @Test
    void getConsumerMessages_forwardsExplicitCursorAndLimit() throws Exception {
        UUID id = UUID.randomUUID();
        when(consumerManager.messages(id, TEST_USER_ID, false, 25, 10))
                .thenReturn(new ConsumerMessagesResponse(id, List.of(), 26, 26, 0));

        mockMvc.perform(get("/api/kafka/consumers/{id}/messages?after=25&limit=10", id).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextSequence").value(26));

        verify(consumerManager).messages(id, TEST_USER_ID, false, 25, 10);
    }

    @Test
    void deleteConsumer_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/kafka/consumers/{id}", id).with(userJwt()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(consumerManager).delete(id, TEST_USER_ID, false);
    }

    @Test
    void getConsumer_notFound_returnsNotFoundResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(consumerManager.get(id, TEST_USER_ID, false)).thenThrow(new ConsumerManager.ConsumerNotFoundException(id));

        mockMvc.perform(get("/api/kafka/consumers/{id}", id).with(userJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Consumer not found: " + id))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void createConsumer_limitReached_returnsConflictResponse() throws Exception {
        when(consumerManager.create(TEST_USER_ID, "localhost:9092", "events"))
                .thenThrow(new ConsumerManager.ConsumerLimitException());

        mockMvc.perform(post("/api/kafka/consumers")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bootstrapAddress\":\"localhost:9092\",\"topic\":\"events\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Maximum number of consumers reached"));
    }
}
