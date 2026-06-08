package ru.uoles.kafka.sender.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.uoles.kafka.sender.model.MessageRequest;
import ru.uoles.kafka.sender.model.MessageResponse;
import ru.uoles.kafka.sender.service.KafkaMessageService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class MessageController {

    private final KafkaMessageService kafkaMessageService;

    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received request to send message to topic: {}, kafka: {}",
                request.getTopic(), request.getKafkaAddress());

        try {
            kafkaMessageService.sendMessage(
                    request.getTopic(),
                    request.getKafkaAddress(),
                    request.getMessageText()
            );

            MessageResponse response = new MessageResponse(
                    "success",
                    "Message sent successfully to topic: " + request.getTopic(),
                    request.getTopic(),
                    request.getKafkaAddress(),
                    System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to send message", e);
            MessageResponse errorResponse = new MessageResponse(
                    "error",
                    "Failed to send message: " + e.getMessage(),
                    request.getTopic(),
                    request.getKafkaAddress(),
                    System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> healthCheck() {
        MessageResponse response = new MessageResponse(
                "success",
                "Kafka Producer Service is running",
                null,
                null,
                System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }
}
