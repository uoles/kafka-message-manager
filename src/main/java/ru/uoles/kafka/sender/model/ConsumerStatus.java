package ru.uoles.kafka.sender.model;

/** Состояние жизненного цикла Kafka-потребителя. */
public enum ConsumerStatus {
    /** Потребитель запускается. */
    STARTING,
    /** Потребитель получает сообщения. */
    RUNNING,
    /** При работе потребителя произошла ошибка. */
    ERROR,
    /** Потребитель остановлен. */
    STOPPED
}
