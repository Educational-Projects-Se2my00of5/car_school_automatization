package ru.hits.car_school_automatization.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import ru.hits.car_school_automatization.filter.JwtAuthFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
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

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers("/channel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/channel/**").hasRole("MANAGER")
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
