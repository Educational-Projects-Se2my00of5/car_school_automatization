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
        createDefaultStudents();
    }

    private void createDefaultManager() {
        String managerEmail = "manager@carschool.ru";
        String teacherEmail = "teacher@carschool.ru";

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

            User teacher = User.builder()
                    .firstName("Препод")
                    .lastName("Системы")
                    .age(30)
                    .phone("+79999999998")
                    .email(teacherEmail)
                    .passwordHash(passwordEncoder.encode("teacher123"))
                    .role(List.of(Role.TEACHER))
                    .isActive(true)
                    .build();

            userRepository.save(manager);
            userRepository.save(teacher);
            log.debug("Создан пользователь по умолчанию: {} с ролью MANAGER", managerEmail);
        } else {
            log.debug("Пользователь {} уже существует", managerEmail);
        }
    }

    private void createDefaultStudents() {
        String student1Email = "student1@carschool.ru";
        String student2Email = "student2@carschool.ru";

        if (!userRepository.existsByEmail(student1Email)) {
            User student1 = User.builder()
                    .firstName("АC")
                    .lastName("С")
                    .age(30)
                    .phone("+79999999997")
                    .email(student1Email)
                    .passwordHash(passwordEncoder.encode("student123"))
                    .role(List.of(Role.STUDENT))
                    .isActive(true)
                    .build();

            User student2 = User.builder()
                    .firstName("Cb")
                    .lastName("a")
                    .age(30)
                    .phone("+79999999996")
                    .email(student2Email)
                    .passwordHash(passwordEncoder.encode("student123"))
                    .role(List.of(Role.STUDENT))
                    .isActive(true)
                    .build();

            userRepository.save(student1);
            userRepository.save(student2);

        }
    }
}
