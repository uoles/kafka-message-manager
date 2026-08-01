package ru.uoles.kafka.sender.kafka.service;

import ru.uoles.kafka.sender.kafka.repository.ConsumersRepository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.util.List;
import java.util.UUID;

/** Сервисный интерфейс сохранения конфигураций потребителей и их сообщений. */
public interface ConsumersInfoService {

    /** Сохраняет описание потребителя и его текущее состояние. */
    void saveConsumer(ConsumersRepository.ConsumerRecord consumer);

    /** Возвращает все сохранённые конфигурации потребителей. */
    List<ConsumersRepository.ConsumerRecord> findAllConsumers();

    /** Удаляет потребителя вместе со связанными сообщениями. */
    void deleteConsumer(UUID id);

    /** Сохраняет полученное сообщение и обновляет счётчики потребителя. */
    void saveMessage(UUID consumerId, ConsumerMessageResponse message, long droppedCount, long nextSequence);

    /** Удаляет сообщение, вытесненное из ограниченного буфера, и обновляет его состояние. */
    void deleteMessage(UUID consumerId, long sequence, long droppedCount, long nextSequence);

    /** Загружает сохранённые сообщения потребителя в порядке локальной последовательности. */
    List<ConsumerMessageResponse> findMessages(UUID consumerId);
}
