package ru.uoles.kafka.sender.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String status;
    private String message;
    private String topic;
    private String kafkaAddress;
    private Long timestamp;
}
