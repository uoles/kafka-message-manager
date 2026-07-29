# Performance & Optimization

## Database
- Index appropriate columns
- Use batch operations where possible
- Avoid N+1 queries with JOIN FETCH
- Use caching (`@Cacheable`) for read-heavy operations
- Paginate large result sets

## Memory Management
- Use primitive collections where possible
- Avoid holding large data in memory
- Close resources properly (try-with-resources)
- Use `@Lazy` for heavy beans
- Clean up thread-local after use

## Kafka Performance
- Batch producer records
- Use compression (snappy/lz4)
- Configure appropriate partition count
- Tune linger.ms and batch.size
- Monitor consumer lag
- Use async processing with `@Async`

## Connection Management
- Configure connection pool size
- Set appropriate timeouts
- Use connection reuse
- Monitor connection pool metrics

## JVM Tuning
- Set appropriate heap size
- Use G1GC for low latency
- Monitor GC metrics
- Configure thread pools appropriately