# Core Java 21 Standards

## Language Features
- Use Java 21 LTS features where appropriate
- Prefer `var` for local variables when type is evident
- Use records for immutable data carriers
- Use switch expressions with pattern matching
- Utilize text blocks for multi-line strings
- Use `String.formatted()` over concatenation
- Leverage `Optional` for nullable values
- Prefer `List.of()`, `Set.of()`, `Map.of()` for immutable collections
- Use `instanceof` pattern matching
- Implement sealed classes for restricted hierarchies
- Use `@Serial` annotation for serialization

## Code Style
- Follow Google Java Style Guide with modifications:
    - 4-space indentation
    - 120 character line length maximum
    - Braces always on same line
- Use meaningful variable names (no single letters except loop indices)
- Group imports: static, third-party, project-specific
- No wildcard imports
- Use final for effective immutability where possible
- For WEB use thymeleaf with bootstrap framework v5.0

## Naming Conventions
- Classes: PascalCase
- Interfaces: PascalCase (no "I" prefix)
- Methods: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase
- Test classes: *Test suffix
- Abstract classes: Abstract* prefix