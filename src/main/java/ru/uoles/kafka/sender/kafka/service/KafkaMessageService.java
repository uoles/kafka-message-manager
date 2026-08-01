package ru.uoles.kafka.sender.kafka.service;

import java.util.concurrent.ExecutionException;

/** Сервис отправки сообщений в Kafka. */
public interface KafkaMessageService {

    /**
     * Отправляет сообщение в указанный топик Kafka.
     *
     * @param topic название топика
     * @param kafkaAddress адрес брокера в формате host:port
     * @param messageText текст сообщения
     * @param headers необязательные заголовки в формате name=value,name2=value2
     * @throws ExecutionException если Kafka сообщает об ошибке отправки
     */
    void sendMessage(String topic, String kafkaAddress, String messageText, String headers) throws ExecutionException;
}
