package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.RoleName;

import java.util.Set;

/**
 * DTO для работы с пользователями
 */
public class UserDto {

    /**
     * DTO для создания нового пользователя
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUser {
        @NotNull(message = "Имя не может быть null")
        private String firstName;

        @NotNull(message = "Фамилия не может быть null")
        private String lastName;

        @NotNull(message = "Возраст не может быть null")
        private Integer age;

        @NotNull(message = "Телефон не может быть null")
        private String phone;

        @NotNull(message = "Email не может быть null")
        private String email;

        @NotNull(message = "Пароль не может быть null")
        private String password;

        @NotNull(message = "Роль не может быть null")
        private Set<RoleName> roleName;
    }

    /**
     * DTO для обновления данных пользователя
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUser {
        @NotNull(message = "Имя не может быть null")
        private String firstName;

        @NotNull(message = "Фамилия не может быть null")
        private String lastName;

        @NotNull(message = "Возраст не может быть null")
        private Integer age;

        @NotNull(message = "Телефон не может быть null")
        private String phone;

        @NotNull(message = "Email не может быть null")
        private String email;

        @NotNull(message = "Роль не может быть null")
        private Set<RoleName> roleName;
    }

    /**
     * DTO с полной информацией о пользователе
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private Integer age;
        private String phone;
        private String email;
        private Set<RoleName> roleName;
        private Boolean isActive;
    }

    /**
     * DTO для смены пароля
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePassword {
        @NotNull(message = "Старый пароль не может быть null")
        private String oldPassword;

        @NotNull(message = "Новый пароль не может быть null")
        private String newPassword;
    }

    /**
     * DTO для операций добавления/удаления роли
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleOperation {
        @NotNull(message = "Роль не может быть null")
        private RoleName roleName;
    }

    /**
     * DTO для параметров поиска пользователей
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchParams {
        private String name;
        private String email;
        private RoleName roleName;
    }}