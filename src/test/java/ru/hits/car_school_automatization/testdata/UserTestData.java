package ru.hits.car_school_automatization.testdata;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;

/**
 * Фабрика тестовых данных для User
 */
public class UserTestData {

    public static UserDto.CreateUser createUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, String password, Role role) {
        return UserDto.CreateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }

    public static UserDto.UpdateUser updateUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, Role role) {
        return UserDto.UpdateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .role(role)
                .build();
    }

    public static User userEntity(Long id, String firstName, String lastName, Integer age,
                                  String phone, String email, String passwordHash, Role role, Boolean isActive) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .passwordHash(passwordHash)
                .role(role)
                .isActive(isActive)
                .build();
    }

    public static UserDto.FullInfo userFullInfoDto(Long id, String firstName, String lastName, Integer age,
                                                   String phone, String email, Role role, Boolean isActive) {
        return UserDto.FullInfo.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .role(role)
                .isActive(isActive)
                .build();
    }

    public static UserDto.ChangePassword changePasswordRequest(String oldPassword, String newPassword) {
        return UserDto.ChangePassword.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();
    }

    public static Stream<Arguments> provideNullFieldsTestData() {
        return Stream.of(
                Arguments.of(
                        createUserRequest(null, "Doe", 20, "+79001112233", "test@test.ru", "password", Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", null, 20, "+79001112233", "test@test.ru", "password", Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", "Doe", null, "+79001112233", "test@test.ru", "password", Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", "Doe", 20, null, "test@test.ru", "password", Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", "Doe", 20, "+79001112233", null, "password", Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", "Doe", 20, "+79001112233", "test@test.ru", null, Role.STUDENT)
                ),
                Arguments.of(
                        createUserRequest("John", "Doe", 20, "+79001112233", "test@test.ru", "password", null)
                )
        );        
    }
    public static Stream<Arguments> provideNullFieldsTestDataForUpdate() {
        return Stream.of(
                Arguments.of(
                        updateUserRequest(null, "Doe", 20, "+79001112233", "test@test.ru", Role.STUDENT)
                ),
                Arguments.of(
                        updateUserRequest("John", null, 20, "+79001112233", "test@test.ru", Role.STUDENT)
                ),
                Arguments.of(
                        updateUserRequest("John", "Doe", null, "+79001112233", "test@test.ru", Role.STUDENT)
                ),
                Arguments.of(
                        updateUserRequest("John", "Doe", 20, null, "test@test.ru", Role.STUDENT)
                ),
                Arguments.of(
                        updateUserRequest("John", "Doe", 20, "+79001112233", null, Role.STUDENT)
                ),
                Arguments.of(
                        updateUserRequest("John", "Doe", 20, "+79001112233", "test@test.ru", null)
                )
        );        
    }
}
