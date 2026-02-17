package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.AuthDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.security.JwtTokenProvider;

/**
 * Сервис для аутентификации
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Вход пользователя в систему
     */
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный email или пароль");
        }

        String token = jwtTokenProvider.generateToken(user.getId());
        return AuthDto.TokenResponse.builder()
                .token(token)
                .build();
    }

    /**
     * Обновление токена
     */
    public AuthDto.TokenResponse refreshToken(String authHeader) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Невалидный или истёкший токен");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        String newToken = jwtTokenProvider.generateToken(userId);
        return AuthDto.TokenResponse.builder()
                .token(newToken)
                .build();
    }
}
