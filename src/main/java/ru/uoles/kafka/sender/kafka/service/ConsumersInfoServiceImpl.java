package ru.uoles.kafka.sender.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.uoles.kafka.sender.kafka.repository.ConsumersRepository;
import ru.uoles.kafka.sender.kafka.repository.MessagesRepository;
import ru.uoles.kafka.sender.model.ConsumerMessageResponse;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumersInfoServiceImpl implements ConsumersInfoService {

    private final ConsumersRepository consumersRepository;
    private final MessagesRepository messagesRepository;

    /** Сохраняет новый консьюмер или обновляет его состояние. */
    @Override
    public void saveConsumer(ConsumersRepository.ConsumerRecord consumer) {
        consumersRepository.saveConsumer(consumer);
    }

    /** Загружает все сохранённые определения консьюмеров. */
    @Override
    public List<ConsumersRepository.ConsumerRecord> findAllConsumers() {
        return consumersRepository.findAllConsumers();
    }

    /** Удаляет консьюмера и связанные сообщения каскадно. */
    @Override
    public void deleteConsumer(UUID id) {
        consumersRepository.deleteConsumer(id);
    }

    /** Сохраняет сообщение и новое состояние последовательности консьюмера. */
    @Override
    public void saveMessage(UUID consumerId, ConsumerMessageResponse message, long droppedCount, long nextSequence) {
        messagesRepository.saveMessage(consumerId, message);
        consumersRepository.saveConsumer(consumerId, droppedCount, nextSequence);
    }

    /** Удаляет из базы сообщение, вытесненное ограниченным буфером. */
    @Override
    public void deleteMessage(UUID consumerId, long sequence, long droppedCount, long nextSequence) {
        messagesRepository.deleteMessage(consumerId, sequence);
        consumersRepository.saveConsumer(consumerId, droppedCount, nextSequence);
    }

    /** Загружает сохранённые сообщения консьюмера в порядке sequence. */
    @Override
    public List<ConsumerMessageResponse> findMessages(UUID consumerId) {
        return messagesRepository.findMessages(consumerId);
    }
}
