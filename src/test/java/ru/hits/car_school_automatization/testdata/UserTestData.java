package ru.hits.car_school_automatization.testdata;

import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.Role;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.RoleName;

import java.util.HashSet;
import java.util.Set;

/**
 * Фабрика тестовых данных для User
 */
public class UserTestData {

    public static UserDto.CreateUser createUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, String password, Set<RoleName> roleNames) {
        return UserDto.CreateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .password(password)
                .roleName(roleNames)
                .build();
    }

    public static UserDto.UpdateUser updateUserRequest(String firstName, String lastName, Integer age,
                                                       String phone, String email, Set<RoleName> roleNames) {
        return UserDto.UpdateUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .roleName(roleNames)
                .build();
    }

    public static User userEntity(Long id, String firstName, String lastName, Integer age,
                                  String phone, String email, String passwordHash, Set<Role> roles, Boolean isActive) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .passwordHash(passwordHash)
                .roles(roles)
                .isActive(isActive)
                .build();
    }

    public static UserDto.FullInfo userFullInfoDto(Long id, String firstName, String lastName, Integer age,
                                                   String phone, String email, Set<RoleName> roleNames, Boolean isActive) {
        return UserDto.FullInfo.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .age(age)
                .phone(phone)
                .email(email)
                .roleName(roleNames)
                .isActive(isActive)
                .build();
    }

    public static UserDto.ChangePassword changePasswordRequest(String oldPassword, String newPassword) {
        return UserDto.ChangePassword.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .build();
    }

    public static UserDto.RoleOperation roleOperationRequest(RoleName roleName) {
        return UserDto.RoleOperation.builder()
                .roleName(roleName)
                .build();
    }

    public static Role roleEntity(Long id, RoleName roleName) {
        return Role.builder()
                .id(id)
                .name(roleName)
                .build();
    }

    // Вспомогательный метод для создания Set<Role> из Set<RoleName>
    public static Set<Role> roleSetFromSet(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        int i = 1;
        for (RoleName roleName : roleNames) {
            roles.add(roleEntity((long) i++, roleName));
        }
        return roles;
    }

}
