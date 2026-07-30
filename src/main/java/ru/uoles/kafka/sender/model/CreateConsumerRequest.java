package ru.uoles.kafka.sender.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Запрос на создание Kafka-потребителя. */
@Data
public class CreateConsumerRequest {
    /** Адрес Kafka-брокера. */
    @NotBlank(message = "Bootstrap address is required")
    @Pattern(regexp = "^[A-Za-z0-9.-]+:[0-9]{1,5}$", message = "Bootstrap address must use host:port format")
    private String bootstrapAddress;
    /** Название топика. */
    @NotBlank(message = "Topic is required")
    @Size(max = 249, message = "Topic must not exceed 249 characters")
    private String topic;
}
