package ru.uoles.kafka.sender.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Данные ответа API после обработки запроса отправки сообщения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /** Статус операции, например success или error. */
    private String status;

    /** Описание результата операции. */
    private String message;

    /** Название обработанного Kafka-топика. */
    private String topic;

    /** Адрес использованного Kafka-брокера. */
    private String kafkaAddress;

    /** Время формирования ответа в миллисекундах Unix epoch. */
    private Long timestamp;
}
