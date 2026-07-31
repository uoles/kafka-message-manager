package ru.uoles.kafka.sender.kafka.service;

import ru.uoles.kafka.sender.kafka.repository.ConsumersRepository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.util.List;
import java.util.UUID;

public interface ConsumersInfoService {

    void saveConsumer(ConsumersRepository.ConsumerRecord consumer);

    List<ConsumersRepository.ConsumerRecord> findAllConsumers();

    void deleteConsumer(UUID id);

    void saveMessage(UUID consumerId, ConsumerMessageResponse message, long droppedCount, long nextSequence);

    void deleteMessage(UUID consumerId, long sequence, long droppedCount, long nextSequence);

    List<ConsumerMessageResponse> findMessages(UUID consumerId);
}
