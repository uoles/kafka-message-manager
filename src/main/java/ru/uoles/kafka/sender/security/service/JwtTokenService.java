package ru.uoles.kafka.sender.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.uoles.kafka.sender.config.SecurityProperties;
import ru.uoles.kafka.sender.model.AuthResponse;
import ru.uoles.kafka.sender.security.model.UserAccount;

import java.time.Instant;

/** Выпускает подписанные JWT-токены доступа. */
@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    /** Создаёт токен с идентификатором пользователя и ролями. */
    public String createToken(UserAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(java.util.List.of(properties.jwt().audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(account.id().toString())
                .claim("username", account.username())
                .claim("roles", account.roles().stream().map(role -> "ROLE_" + role).toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return properties.jwt().accessTokenTtl().toSeconds();
    }
}
