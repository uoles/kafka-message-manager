# Security

## Authentication/Authorization
- Use Spring Security with JWT
- Implement method-level security: `@PreAuthorize`
- Validate all inputs
- Use HTTPS in production
- Implement rate limiting
- Use CSRF protection for stateful APIs

## JWT Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

Data Protection

- Encrypt sensitive data
- Use @JsonProperty(access = Access.WRITE_ONLY) for passwords
- Avoid logging sensitive information
- Use secure password encoding (BCrypt)

CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("${cors.allowed-origins}")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*");
            }
        };
    }
}
```

Security Headers
- Enable X-Content-Type-Options: nosniff
- Enable X-Frame-Options: DENY
- Enable Strict-Transport-Security
- Enable Content-Security-Policy