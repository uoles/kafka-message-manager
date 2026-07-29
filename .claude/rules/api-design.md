# API Design

## REST Principles
- Use proper HTTP methods (GET, POST, PUT, DELETE)
- Return appropriate status codes
- Use resource-based URLs (plural nouns)
- Version APIs: `/api/v1/resource`
- Use query parameters for filtering
- Use `@JsonView` for different responses

## DTOs
```java
public record CreateOrderRequest(
    @NotBlank String customerId,
    @Positive BigDecimal amount,
    @Valid List<OrderItemDto> items
) {}

public record OrderResponse(
    UUID id,
    String customerId,
    BigDecimal amount,
    OrderStatus status,
    Instant createdAt
) {}
```

API Documentation

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "Order management endpoints")
public class OrderController {
    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new order")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request
    ) {
        // Implementation
    }
}
```

Pagination

```java
@GetMapping
public ResponseEntity<Page<OrderResponse>> getOrders(
    @PageableDefault(size = 20) Pageable pageable,
    @RequestParam(required = false) OrderStatus status
) {
    return ResponseEntity.ok(orderService.getOrders(pageable, status));
}
```

Validation

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request,
    @RequestHeader("X-Request-ID") String requestId
) {
    // Implementation with validation context
}
```

Error Responses

- Consistent error format 
- Include request ID in errors 
- Provide actionable error messages 
- Hide internal details in production 
- Include timestamp for debugging