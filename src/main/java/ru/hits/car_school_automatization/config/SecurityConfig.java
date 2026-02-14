package ru.hits.car_school_automatization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        // эндпоинты /users
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/users/*/deactivate", "/users/*/activate").hasRole("MANAGER")
                        // нужно поменять через authHeader пользователь может поменять только свой пароль
                        .requestMatchers(HttpMethod.PATCH, "/users/*/change-password").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAnyRole("TEACHER", "MANAGER")
                        // TODO нужно добавить /me эндпоинты, но после AuthController, так как требуется работа с токеном

                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
