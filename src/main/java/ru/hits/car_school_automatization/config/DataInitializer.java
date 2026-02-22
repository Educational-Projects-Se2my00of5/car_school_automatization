package ru.hits.car_school_automatization.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.List;

/**
 * Инициализатор данных при старте приложения
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultManager();
    }

    private void createDefaultManager() {
        String managerEmail = "manager@carschool.ru";

        if (!userRepository.existsByEmail(managerEmail)) {
            User manager = User.builder()
                    .firstName("Администратор")
                    .lastName("Системы")
                    .age(30)
                    .phone("+79999999999")
                    .email(managerEmail)
                    .passwordHash(passwordEncoder.encode("manager123"))
                    .role(List.of(Role.MANAGER))
                    .isActive(true)
                    .build();

            userRepository.save(manager);
            log.debug("Создан пользователь по умолчанию: {} с ролью MANAGER", managerEmail);
        } else {
            log.debug("Пользователь {} уже существует", managerEmail);
        }
    }
}
