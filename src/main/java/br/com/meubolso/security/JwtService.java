package br.com.meubolso.security;

import br.com.meubolso.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-ttl-min}") long accessTtlMinutes,
            @Value("${app.security.jwt.refresh-ttl-days}") long refreshTtlDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, TYPE_ACCESS, accessTtlMinutes, ChronoUnit.MINUTES);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, TYPE_REFRESH, refreshTtlDays, ChronoUnit.DAYS);
    }

    public OffsetDateTime extractExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TYPE_ACCESS);

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        return new AuthenticatedUser(userId, email);
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TYPE_REFRESH);

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        return new AuthenticatedUser(userId, email);
    }

    private String generateToken(User user, String tokenType, long ttl, ChronoUnit unit) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl, unit)))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
    }
}
