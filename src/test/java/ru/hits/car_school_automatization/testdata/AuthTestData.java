package ru.hits.car_school_automatization.testdata;


import ru.hits.car_school_automatization.dto.AuthDto;

/**
 * Фабрика тестовых данных для аутентификации
 */
public class AuthTestData {

    public static AuthDto.LoginRequest loginRequest(String email, String password) {
        return AuthDto.LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    public static String authHeader(String token) {
        return "Bearer " + token;
    }

    public static AuthDto.TokenResponse tokenResponse(String token) {
        return AuthDto.TokenResponse.builder()
                .token(token)
                .build();
    }
}
