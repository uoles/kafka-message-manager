package ru.uoles.kafka.sender.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {

    @NotBlank(message = "Topic name is required")
    private String topic;

    @NotBlank(message = "Kafka address is required")
    private String kafkaAddress;

    @NotBlank(message = "Message text is required")
    private String messageText;
}
