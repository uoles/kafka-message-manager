package ru.uoles.kafka.sender.kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.uoles.kafka.sender.config.KafkaClientFactory;

@ExtendWith(MockitoExtension.class)
class KafkaMessageServiceImplTest {

    @Mock
    private KafkaClientFactory kafkaClientFactory;

    @Mock
    private DefaultKafkaProducerFactory<String, String> producerFactory;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaMessageServiceImpl service;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void sendMessage_sendsRecordWithHeadersAndExpectedProducerProperties() throws Exception {
        CompletableFuture<SendResult<String, String>> future = successfulFuture();
        when(kafkaClientFactory.create(any())).thenReturn(new KafkaClientFactory.KafkaClient(producerFactory, kafkaTemplate));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        service.sendMessage("events", "localhost:29092", "payload", "trace-id=abc,source=ui");

        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaClientFactory).create(propertiesCaptor.capture());
        Map<String, Object> properties = propertiesCaptor.getValue();
        assertThat(properties).containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")
                .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all")
                .containsEntry(ProducerConfig.RETRIES_CONFIG, 3)
                .containsEntry(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000)
                .containsEntry(ProducerConfig.LINGER_MS_CONFIG, 100)
                .containsEntry(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000)
                .containsEntry(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();
        assertThat(record.topic()).isEqualTo("events");
        assertThat(record.value()).isEqualTo("payload");
        assertThat(record.key()).isNull();
        assertThat(record.headers()).extracting(Header::key).containsExactly("trace-id", "source");
        assertThat(record.headers().lastHeader("trace-id").value()).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
        verify(kafkaTemplate).destroy();
        verify(producerFactory).destroy();
    }

    @Test
    void sendMessage_withNullHeaders_sendsWithoutHeaders() throws Exception {
        when(kafkaClientFactory.create(any())).thenReturn(new KafkaClientFactory.KafkaClient(producerFactory, kafkaTemplate));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(successfulFuture());

        service.sendMessage("events", "localhost:29092", "payload", null);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().headers()).isEmpty();
    }

    @Test
    void sendMessage_withInvalidHeaders_failsBeforeProducerCreation() {
        assertThatThrownBy(() -> service.sendMessage("events", "localhost:29092", "payload", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Kafka header: expected name=value");
    }

    @Test
    void sendMessage_whenKafkaReportsExecutionFailure_propagatesAndCleansUp() throws Exception {
        ExecutionException failure = new ExecutionException(new IllegalStateException("broker failure"));
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        when(kafkaClientFactory.create(any())).thenReturn(new KafkaClientFactory.KafkaClient(producerFactory, kafkaTemplate));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        assertThatThrownBy(() -> service.sendMessage("events", "localhost:29092", "payload", ""))
                .isInstanceOf(ExecutionException.class);
        verify(kafkaTemplate).destroy();
        verify(producerFactory).destroy();
    }

    @Test
    void sendMessage_whenTimedOut_wrapsTimeoutAndCleansUp() throws Exception {
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaClientFactory.create(any())).thenReturn(new KafkaClientFactory.KafkaClient(producerFactory, kafkaTemplate));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new TimeoutException("timeout"));

        assertThatThrownBy(() -> service.sendMessage("events", "localhost:29092", "payload", ""))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timed out while sending message to Kafka")
                .hasCauseInstanceOf(TimeoutException.class);
        verify(kafkaTemplate).destroy();
        verify(producerFactory).destroy();
    }

    @Test
    void sendMessage_whenInterrupted_restoresFlagAndWrapsException() throws Exception {
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(kafkaClientFactory.create(any())).thenReturn(new KafkaClientFactory.KafkaClient(producerFactory, kafkaTemplate));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        try {
            assertThatThrownBy(() -> service.sendMessage("events", "localhost:29092", "payload", ""))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Interrupted while sending message to Kafka")
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        verify(kafkaTemplate).destroy();
        verify(producerFactory).destroy();
    }

    private static CompletableFuture<SendResult<String, String>> successfulFuture() {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("events", 0), 1L, 0, 0L, 0, 0);
        SendResult<String, String> result = new SendResult<>(null, metadata);
        return CompletableFuture.completedFuture(result);
    }
}
