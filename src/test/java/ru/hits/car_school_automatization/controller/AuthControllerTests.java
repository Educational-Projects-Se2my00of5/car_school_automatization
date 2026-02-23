package ru.hits.car_school_automatization.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hits.car_school_automatization.dto.AuthDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.RoleName;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.service.JwtTokenProvider;
import ru.hits.car_school_automatization.service.AuthService;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.hits.car_school_automatization.testdata.AuthTestData.authHeader;
import static ru.hits.car_school_automatization.testdata.AuthTestData.loginRequest;
import static ru.hits.car_school_automatization.testdata.UserTestData.roleEntity;
import static ru.hits.car_school_automatization.testdata.UserTestData.userEntity;

/**
 * Тесты для AuthController
 */
@DisplayName("Auth Controller Tests")
class AuthControllerTests {

    private AuthController authController;
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
        authController = new AuthController(authService);
    }

    @Test
    @DisplayName("Успешный вход с валидными данными")
    void login_withValidCredentials_shouldReturnToken() {
        // Arrange
        String email = "test@test.ru";
        String password = "password123";
        String passwordHash = "hash";
        Long userId = 1L;
        String generatedToken = "valid-jwt-token";

        AuthDto.LoginRequest request = loginRequest(email, password);
        User user = userEntity(userId, "Иван", "Иванов", 25, "+79001112233", email, passwordHash, Set.of(roleEntity(1L,RoleName.STUDENT)), true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, passwordHash)).thenReturn(true);
        when(jwtTokenProvider.generateToken(userId)).thenReturn(generatedToken);

        // Act
        AuthDto.TokenResponse result = authController.login(request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(generatedToken, result.getToken())
        );

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, passwordHash);
        verify(jwtTokenProvider).generateToken(userId);
    }

    @Test
    @DisplayName("Вход с неверным паролем должен выбросить исключение")
    void login_withInvalidPassword_shouldThrowBadRequestException() {
        // Arrange
        String email = "test@test.ru";
        String correctPasswordHash = "hash";
        String wrongPassword = "wrongPassword";
        Long userId = 1L;

        AuthDto.LoginRequest request = loginRequest(email, wrongPassword);
        User user = userEntity(userId, "Иван", "Иванов", 25, "+79001112233", email, correctPasswordHash, Set.of(roleEntity(1L,RoleName.STUDENT)), true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPassword, correctPasswordHash)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authController.login(request)
        );

        assertEquals("Неверный email или пароль", exception.getMessage());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(wrongPassword, correctPasswordHash);
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Вход с несуществующим email должен выбросить исключение")
    void login_withNonExistentEmail_shouldThrowBadRequestException() {
        // Arrange
        String email = "nonexistent@test.ru";
        String password = "password123";

        AuthDto.LoginRequest request = loginRequest(email, password);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authController.login(request)
        );

        assertEquals("Неверный email или пароль", exception.getMessage());
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Успешное обновление токена с валидным токеном")
    void refreshToken_withValidToken_shouldReturnNewToken() {
        // Arrange
        String oldToken = "old-valid-token";
        String header = authHeader(oldToken);
        Long userId = 1L;
        String newToken = "new-refreshed-token";

        User user = userEntity(userId, "Иван", "Иванов", 25, "+79001112233", "test@test.ru", "hash", Set.of(roleEntity(1L,RoleName.STUDENT)), true);

        when(jwtTokenProvider.extractTokenFromHeader(header)).thenReturn(oldToken);
        when(jwtTokenProvider.validateToken(oldToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(oldToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(userId)).thenReturn(newToken);

        // Act
        AuthDto.TokenResponse result = authController.refreshToken(header);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(newToken, result.getToken()),
                () -> assertNotEquals(oldToken, result.getToken())
        );

        verify(jwtTokenProvider).extractTokenFromHeader(header);
        verify(jwtTokenProvider).validateToken(oldToken);
        verify(jwtTokenProvider).getUserIdFromToken(oldToken);
        verify(userRepository).findById(userId);
        verify(jwtTokenProvider).generateToken(userId);
    }

    @Test
    @DisplayName("Обновление токена с невалидным токеном должно выбросить исключение")
    void refreshToken_withInvalidToken_shouldThrowBadRequestException() {
        // Arrange
        String invalidToken = "invalid-or-malformed-token";
        String header = authHeader(invalidToken);

        when(jwtTokenProvider.extractTokenFromHeader(header)).thenReturn(invalidToken);
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authController.refreshToken(header)
        );

        assertEquals("Невалидный или истёкший токен", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(header);
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Обновление токена истёкшего токена должно выбросить исключение")
    void refreshToken_withExpiredToken_shouldThrowBadRequestException() {
        // Arrange
        String expiredToken = "expired-token";
        String header = authHeader(expiredToken);

        when(jwtTokenProvider.extractTokenFromHeader(header)).thenReturn(expiredToken);
        when(jwtTokenProvider.validateToken(expiredToken)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authController.refreshToken(header)
        );

        assertEquals("Невалидный или истёкший токен", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(header);
        verify(jwtTokenProvider).validateToken(expiredToken);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Обновление токена без префикса Bearer должно выбросить исключение")
    void refreshToken_withoutBearerPrefix_shouldThrowBadRequestException() {
        // Arrange
        String headerWithoutBearer = "just-a-token";

        when(jwtTokenProvider.extractTokenFromHeader(headerWithoutBearer))
                .thenThrow(new BadRequestException("Невалидный заголовок Authorization"));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authController.refreshToken(headerWithoutBearer)
        );

        assertEquals("Невалидный заголовок Authorization", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(headerWithoutBearer);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(any());
    }
}
