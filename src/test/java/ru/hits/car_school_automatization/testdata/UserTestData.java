package ru.hits.car_school_automatization.testdata;

import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;

import java.util.List;

/**
 * Фабрика тестовых данных для User
 */
public class UserTestData {

    public static UserDto.CreateUser createUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, String password, List<Role> roles) {
        return UserDto.CreateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .password(password)
                .role(roles)
                .build();
    }

    public static UserDto.UpdateUser updateUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, List<Role> roles) {
        return UserDto.UpdateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .role(roles)
                .build();
    }

    public static User userEntity(Long id, String firstName, String lastName, Integer age,
                                  String phone, String email, String passwordHash, List<Role> roles, Boolean isActive) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .passwordHash(passwordHash)
                .role(roles)
                .isActive(isActive)
                .build();
    }

    public static UserDto.FullInfo userFullInfoDto(Long id, String firstName, String lastName, Integer age,
                                                   String phone, String email, List<Role> roles, Boolean isActive) {
        return UserDto.FullInfo.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .role(roles)
                .isActive(isActive)
                .build();
    }

    public static UserDto.ChangePassword changePasswordRequest(String oldPassword, String newPassword) {
        return UserDto.ChangePassword.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();
    }

    public static UserDto.RoleOperation roleOperationRequest(Role role) {
        return UserDto.RoleOperation.builder()
                .role(role)
                .build();
    }
}
