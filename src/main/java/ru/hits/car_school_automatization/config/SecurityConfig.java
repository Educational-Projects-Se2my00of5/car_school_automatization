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
import org.springframework.web.cors.CorsConfiguration;
import ru.hits.car_school_automatization.filter.JwtAuthFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
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
                        .requestMatchers(HttpMethod.PATCH, "/users/*/deactivate", "/users/*/activate").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/users/*/add-role", "/users/*/remove-role").hasRole("MANAGER")
                        .requestMatchers("/users/change-password", "/users/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAnyRole("TEACHER", "MANAGER")

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers("/channel").authenticated()
                        .requestMatchers(HttpMethod.POST, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/channel/**").hasRole("MANAGER")

                        .requestMatchers("/posts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/channel/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/channel/**").hasAnyRole("MANAGER", "STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/channel/**").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/teams/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/teams/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/teams/*/members/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/*/members/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/teams/*/join").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/*/leave").hasRole("STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/tasks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tasks").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/tasks/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/tasks/*/documents").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/tasks/*/documents").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/tasks/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/tasks/*/copy").hasRole("TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/task-solutions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/task-solutions").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/task-solutions/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/task-solutions/*").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/teams/*/invite/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/teams/invites/*/accept").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/invites/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/teams/invites/my").hasRole("STUDENT")

                        .requestMatchers(HttpMethod.POST, "/api/teams/choose-captain").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/teams/*/init-captain-voting").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/teams/*/captain").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/teams/*/captain-votes").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/task-solutions/vote").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/task-solutions/vote/my/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/task-solutions/vote/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/task-solutions/*/voting-results").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/task-solutions/*/selected-solution").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/task-solutions/*/selected-solutions").hasRole("TEACHER")

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
