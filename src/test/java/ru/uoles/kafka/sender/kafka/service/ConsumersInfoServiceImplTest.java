package ru.uoles.kafka.sender.kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.uoles.kafka.sender.enums.ConsumerStatus;
import ru.uoles.kafka.sender.kafka.repository.ConsumersRepository;
import ru.uoles.kafka.sender.kafka.repository.ReceivedMessagesRepository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

@ExtendWith(MockitoExtension.class)
class ConsumersInfoServiceImplTest {

    @Mock
    private ConsumersRepository consumersRepository;

    @Mock
    private ReceivedMessagesRepository receivedMessagesRepository;

    @InjectMocks
    private ConsumersInfoServiceImpl service;

    @Test
    void saveConsumer_delegatesToConsumersRepository() {
        ConsumersRepository.ConsumerRecord consumer = consumerRecord();

        service.saveConsumer(consumer);

        verify(consumersRepository).saveConsumer(consumer);
        verifyNoInteractions(receivedMessagesRepository);
    }

    @Test
    void findAllConsumers_returnsRepositoryResult() {
        List<ConsumersRepository.ConsumerRecord> consumers = List.of(consumerRecord());
        when(consumersRepository.findAllConsumers()).thenReturn(consumers);

        assertThat(service.findAllConsumers()).isSameAs(consumers);
        verify(consumersRepository).findAllConsumers();
        verifyNoInteractions(receivedMessagesRepository);
    }

    @Test
    void deleteConsumer_delegatesToConsumersRepository() {
        UUID id = UUID.randomUUID();

        service.deleteConsumer(id);

        verify(consumersRepository).deleteConsumer(id);
        verifyNoInteractions(receivedMessagesRepository);
    }

    @Test
    void saveMessage_persistsMessageBeforeUpdatingConsumerState() {
        UUID id = UUID.randomUUID();
        ConsumerMessageResponse message = message();

        service.saveMessage(id, message, 4L, 12L);

        InOrder order = inOrder(receivedMessagesRepository, consumersRepository);
        order.verify(receivedMessagesRepository).saveMessage(id, message);
        order.verify(consumersRepository).saveConsumer(id, 4L, 12L);
    }

    @Test
    void deleteMessage_deletesMessageBeforeUpdatingConsumerState() {
        UUID id = UUID.randomUUID();

        service.deleteMessage(id, 7L, 5L, 14L);

        InOrder order = inOrder(receivedMessagesRepository, consumersRepository);
        order.verify(receivedMessagesRepository).deleteMessage(id, 7L);
        order.verify(consumersRepository).saveConsumer(id, 5L, 14L);
    }

    @Test
    void findMessages_returnsRepositoryResult() {
        UUID id = UUID.randomUUID();
        List<ConsumerMessageResponse> messages = List.of(message());
        when(receivedMessagesRepository.findMessages(id)).thenReturn(messages);

        assertThat(service.findMessages(id)).isSameAs(messages);
        verify(receivedMessagesRepository).findMessages(id);
        verifyNoInteractions(consumersRepository);
    }

    @Test
    void saveMessage_whenMessagePersistenceFails_doesNotUpdateConsumer() {
        UUID id = UUID.randomUUID();
        ConsumerMessageResponse message = message();
        IllegalStateException failure = new IllegalStateException("save failed");
        doThrow(failure).when(receivedMessagesRepository).saveMessage(id, message);

        assertThatThrownBy(() -> service.saveMessage(id, message, 1L, 2L))
                .isSameAs(failure);
        verify(consumersRepository, never()).saveConsumer(id, 1L, 2L);
    }

    @Test
    void deleteMessage_whenMessageDeletionFails_doesNotUpdateConsumer() {
        UUID id = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("delete failed");
        doThrow(failure).when(receivedMessagesRepository).deleteMessage(id, 3L);

        assertThatThrownBy(() -> service.deleteMessage(id, 3L, 1L, 4L))
                .isSameAs(failure);
        verify(consumersRepository, never()).saveConsumer(id, 1L, 4L);
    }

    @Test
    void repositoryFailures_propagateUnchanged() {
        ConsumersRepository.ConsumerRecord consumer = consumerRecord();
        IllegalStateException failure = new IllegalStateException("repository failed");
        doThrow(failure).when(consumersRepository).saveConsumer(consumer);

        assertThatThrownBy(() -> service.saveConsumer(consumer)).isSameAs(failure);
    }

    private static ConsumersRepository.ConsumerRecord consumerRecord() {
        return new ConsumersRepository.ConsumerRecord(UUID.randomUUID(), "localhost:29092", "events",
                "group-1", Instant.ofEpochMilli(1_000L), ConsumerStatus.RUNNING, null, 0L, 1L);
    }

    private static ConsumerMessageResponse message() {
        return new ConsumerMessageResponse(1L, "key", "value", "events", 0, 3L,
                Instant.ofEpochMilli(2_000L), List.of());
    }
}
