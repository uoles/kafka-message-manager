# Project Structure

## Package Organization

```
com.company.project/
├── config/ # Spring configuration classes
├── controller/ # REST controllers
├── service/ # Business logic
├── repository/ # Data access layer
├── model/ # DTOs and domain models
├── kafka/ # Kafka producers/consumers
│ ├── config/
│ ├── producer/
│ ├── consumer/
│ └── event/
├── exception/ # Custom exceptions
├── util/ # Utility classes
└── aspect/ # AOP aspects
```

## Module Structure (for multi-module projects)

```
project-root/
├── pom.xml (parent)
├── common/
│ └── pom.xml
├── api/
│ └── pom.xml
├── core/
│ └── pom.xml
├── infrastructure/
│ └── pom.xml
└── starter/
└── pom.xml
```

