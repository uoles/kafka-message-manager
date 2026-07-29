# Monitoring & Observability

## Metrics Configuration
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
  health:
    liveness:
      enabled: true
    readiness:
      enabled: true
```

Custom Metrics
```java
@Component
public class OrderMetrics {
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;
    
    public OrderMetrics(MeterRegistry registry) {
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(registry);
            
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(registry);
    }
    
    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }
}
```

Distributed Tracing

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

Kafka Monitoring

```java
@Component
public class KafkaMetricsListener implements ConsumerRebalanceListener {
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Track partition assignments
    }
    
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // Track partitions revoked
    }
}
```

Alert Rules

- High error rate (>5%)
- High latency (p95 > 500ms)
- Consumer lag (>1000)
- Low disk space (<10%)
- High GC time (>10%)