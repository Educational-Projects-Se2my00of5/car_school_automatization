package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hits.car_school_automatization.dto.AuthDto;
import ru.hits.car_school_automatization.service.AuthService;

/**
 * REST API контроллер для аутентификации
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Вход в систему")
    @PostMapping("/login")
    public AuthDto.TokenResponse login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Обновление токена")
    @PostMapping("/refresh")
    public AuthDto.TokenResponse refreshToken(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return authService.refreshToken(authHeader);
    }
}
