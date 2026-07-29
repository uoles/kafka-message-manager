# Spring Boot Best Practices

## Configuration
- Use `@ConfigurationProperties` for external configurations
- Prefer constructor injection over field injection
- Use `@Profile` for environment-specific beans
- Define beans in configuration classes, not in main class
- Use `@Conditional` for conditional bean creation
- Externalize all configurable properties

## Controllers
- Keep controllers thin - delegate to services
- Use `@RestController` with `@RequestMapping`
- Validate inputs with `@Valid`
- Return `ResponseEntity` for flexible responses
- Use `@ControllerAdvice` for global exception handling
- Document APIs with OpenAPI/Swagger annotations

## Services
- Implement interfaces with `@Service`
- Use `@Transactional` at service layer
- Keep methods focused and single-purpose
- Handle business exceptions with custom exceptions
- Use validation annotations on DTOs

## Repository Layer
- Use Spring Data JPA with `@Repository`
- Prefer JpaRepository interface methods
- Use `@Query` for complex queries
- Implement custom repositories for complex logic
- Use projection interfaces for partial data

## Actuator & Monitoring
- Enable health, metrics, info endpoints
- Custom health indicators for dependencies
- Export metrics to monitoring system
- Use micrometer for custom metrics