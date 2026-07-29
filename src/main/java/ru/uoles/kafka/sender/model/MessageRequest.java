package ru.uoles.kafka.sender.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные запроса на отправку сообщения в Kafka.
 */
@Data
public class MessageRequest {

    /** Название Kafka-топика назначения. */
    @NotBlank(message = "Topic name is required")
    private String topic;

    /** Адрес Kafka-брокера в формате host:port. */
    @NotBlank(message = "Kafka address is required")
    private String kafkaAddress;

    /** Текст отправляемого сообщения. */
    @NotBlank(message = "Message text is required")
    private String messageText;

    /** Необязательные заголовки в формате name=value,name2=value2. */
    private String headers;
}
