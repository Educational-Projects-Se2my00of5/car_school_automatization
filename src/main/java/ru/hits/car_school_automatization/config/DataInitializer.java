package ru.hits.car_school_automatization.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.entity.Role;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.RoleName;
import ru.hits.car_school_automatization.repository.RoleRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Инициализатор данных при старте приложения
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeRoles();
        createDefaultManager();
    }

    /**
     * Инициализация базовых ролей в БД
     */
    private void initializeRoles() {
        for (RoleName roleName : RoleName.values()) {
            Role existingRole = roleRepository.findByName(roleName);
            if (existingRole == null) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
                log.debug("Создана роль: {}", roleName);
            }
        }
    }

    private void createDefaultManager() {
        String managerEmail = "manager@carschool.ru";

        if (!userRepository.existsByEmail(managerEmail)) {
            // Получаем роли из БД
            Role managerRole = roleRepository.findByName(RoleName.MANAGER);
            Role studentRole = roleRepository.findByName(RoleName.STUDENT);

            Set<Role> roles = new HashSet<>();
            roles.add(managerRole);
            roles.add(studentRole);

            User manager = User.builder()
                    .firstName("Администратор")
                    .lastName("Системы")
                    .age(30)
                    .phone("+79999999999")
                    .email(managerEmail)
                    .passwordHash(passwordEncoder.encode("manager123"))
                    .roles(roles)
                    .isActive(true)
                    .build();

            userRepository.save(manager);
            log.debug("Создан пользователь по умолчанию: {} с ролями MANAGER, STUDENT", managerEmail);
        } else {
            log.debug("Пользователь {} уже существует", managerEmail);
        }
    }
}
