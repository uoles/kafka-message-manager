# Development Rules Index

## Core Rules
1. [Core Java Standards](./core-java.md)
2. [Project Structure](./project-structure.md)
3. [Spring Boot Best Practices](./spring-boot.md)

[//]: # (4. [Kafka Implementation]&#40;./kafka.md&#41;)

## Quality

[//]: # (5. [Testing Standards]&#40;./testing.md&#41;)
6. [Code Review Checklist](./code-review.md)
7. [Anti-Patterns to Avoid](./anti-patterns.md)

## Operations

[//]: # (8. [Error Handling & Logging]&#40;./error-handling.md&#41;)
9. [Performance & Optimization](./performance.md)
10. [Monitoring](./monitoring.md)

## Architecture
11. [API Design](./api-design.md)
12. [Security](./security.md)

[//]: # (13. [Dependencies Management]&#40;./dependencies.md&#41;)

## Process
14. [Development Workflow](./development-workflow.md)

## How to Use
1. Read `core-java.md, task-memory.md, claude-file-update.md` first
2. Follow `project-structure.md` for organization
3. Apply `spring-boot.md` for implementation
4. Use `code-review.md` for reviews
5. Check `anti-patterns.md` regularly

## Quick Reference
- All DTOs should be records
- Use constructor injection
- Implement error handling for all Kafka listeners
- Write tests for all new features
- Document all public APIs
- Externalize configuration