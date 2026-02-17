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
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/users/*/deactivate", "/users/*/activate", "/users/*/change-role"
                        ).hasRole("MANAGER")
                        .requestMatchers("/users/change-password", "/users/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAnyRole("TEACHER", "MANAGER")

                        .requestMatchers("auth/**").permitAll()

                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
