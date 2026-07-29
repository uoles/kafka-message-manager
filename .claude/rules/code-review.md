# Code Review Checklist

## General
- [ ] Code follows project structure and conventions
- [ ] No `System.out.println()` or debug code
- [ ] No commented out code
- [ ] No TODOs without issue reference
- [ ] Proper package organization
- [ ] Consistent naming conventions

## Functionality
- [ ] Tests cover new functionality
- [ ] Edge cases handled
- [ ] Error scenarios tested
- [ ] Validation implemented
- [ ] Business logic correct

## Performance
- [ ] No performance bottlenecks
- [ ] Database queries optimized
- [ ] Caching implemented where appropriate
- [ ] No memory leaks
- [ ] Asynchronous processing used for long operations

## Error Handling
- [ ] Proper exception handling
- [ ] Custom exceptions defined
- [ ] Error responses consistent
- [ ] Logging at appropriate levels
- [ ] Sensitive data not logged

## Security
- [ ] Input validation
- [ ] Authorization checks
- [ ] No sensitive data in logs or responses
- [ ] Secure dependencies
- [ ] Rate limiting considered

## Kafka Specific
- [ ] Error handlers configured
- [ ] Idempotent consumption
- [ ] Dead letter queue configured
- [ ] Proper key selection for partitioning
- [ ] Consumer lag monitored

## Documentation
- [ ] JavaDoc for public APIs
- [ ] API documentation updated
- [ ] README updated if needed
- [ ] ADR for important decisions

## Configuration
- [ ] Configuration externalized
- [ ] Environment-specific properties
- [ ] Default values provided
- [ ] Secrets not in code

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Test coverage maintained/improved
- [ ] Edge cases tested
- [ ] Kafka tests included

## Dependencies
- [ ] No duplicate dependencies
- [ ] Versions consistent
- [ ] No vulnerable dependencies
- [ ] Dependencies minimized

## Migrations
- [ ] Migration scripts tested
- [ ] Rollback scripts provided
- [ ] Backward compatibility maintained
- [ ] Data migration considered

## Monitoring
- [ ] Metrics added for new features
- [ ] Health indicators updated
- [ ] Logging sufficient for debugging
- [ ] Alerts configured

## Deployment
- [ ] Application can be started/stopped gracefully
- [ ] Health checks functional
- [ ] Resource limits considered
- [ ] Can be rolled back