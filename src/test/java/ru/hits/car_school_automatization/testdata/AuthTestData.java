package ru.hits.car_school_automatization.testdata;


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

    public static AuthDto.RefreshRequest refreshRequest(String token) {
        return AuthDto.RefreshRequest.builder()
                .token(token)
                .build();
    }

    public static AuthDto.TokenResponse tokenResponse(String token) {
        return AuthDto.TokenResponse.builder()
                .token(token)
                .build();
    }
}
