package ru.uoles.kafka.sender.kafka.service;

import java.util.concurrent.ExecutionException;

public interface KafkaMessageService {

    void sendMessage(String topic, String kafkaAddress, String messageText, String headers) throws ExecutionException;
}
