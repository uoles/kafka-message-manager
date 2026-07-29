# Anti-Patterns to Avoid

## Spring Anti-Patterns
- ❌ `@Autowired` field injection
- ❌ Business logic in controllers
- ❌ `@Transactional` in controllers
- ❌ Ignoring exceptions
- ❌ Hardcoded configuration values
- ❌ Constructor injection on every class

## Java Anti-Patterns
- ❌ `Thread.sleep()` in production
- ❌ String concatenation in loops
- ❌ `System.out.println()` in production
- ❌ Deep inheritance hierarchies
- ❌ Overusing reflection
- ❌ Ignoring generic warnings

## Kafka Anti-Patterns
- ❌ Using synchronous sends without error handling
- ❌ Not handling consumer deserialization errors
- ❌ Using auto commit without control
- ❌ Not setting appropriate timeouts
- ❌ Creating too many topics

## Performance Anti-Patterns
- ❌ N+1 queries
- ❌ Holding database connections too long
- ❌ Large transactions
- ❌ Inefficient data structures
- ❌ Not using connection pooling
- ❌ Blocking I/O in reactive contexts

## Code Quality Anti-Patterns
- ❌ Copy-paste code duplication
- ❌ Magic numbers and strings
- ❌ Too many method parameters (>4)
- ❌ God classes
- ❌ Method too long (>20 lines)
- ❌ Comments that explain bad code

## Testing Anti-Patterns
- ❌ Not testing exception scenarios
- ❌ Using `Thread.sleep()` in tests
- ❌ Ignoring flaky tests
- ❌ Not using test containers
- ❌ Testing implementation details
- ❌ Not cleaning up test data

## Security Anti-Patterns
- ❌ Storing passwords in source code
- ❌ Using weak encryption
- ❌ Not validating inputs
- ❌ Exposing stack traces in responses
- ❌ Not implementing rate limiting
- ❌ Insecure CORS configuration

## Architecture Anti-Patterns
- ❌ Circular dependencies
- ❌ God object in service layer
- ❌ Not separating concerns
- ❌ Over-engineering for future
- ❌ Ignoring database migrations
- ❌ Not handling backward compatibility