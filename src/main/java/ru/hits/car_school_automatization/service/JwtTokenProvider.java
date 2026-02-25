package ru.hits.car_school_automatization.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.UserRepository;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Провайдер для работы с JWT токенами
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;
    private final UserRepository userRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds,
            UserRepository userRepository) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.validityInMilliseconds = validityInMilliseconds;
        this.userRepository = userRepository;
    }

    /**
     * Генерация JWT токена для пользователя
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Валидация JWT токена
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Извлечение ID пользователя из токена
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * Извлечение токена из заголовка Authorization
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Невалидный заголовок Authorization");
        }
        return authHeader.substring(7);
    }
}
