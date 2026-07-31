package ru.uoles.kafka.sender.enums;

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
