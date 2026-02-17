package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для аутентификации
 */
public class AuthDto {

    /**
     * DTO для запроса входа в систему
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotNull(message = "Email не может быть null")
        private String email;

        @NotNull(message = "Пароль не может быть null")
        private String password;
    }

    /**
     * DTO для ответа с токеном
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String token;
    }
}
