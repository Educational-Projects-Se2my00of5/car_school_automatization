package ru.hits.car_school_automatization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.service.UserService;

import java.util.List;

/**
 * REST API контроллер для работы с пользователями
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Создание нового пользователя
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.FullInfo createUser(@Valid @RequestBody UserDto.CreateUser dto) {
        return userService.createUser(dto);
    }

    /**
     * Получение всех пользователей
     */
    @GetMapping
    public List<UserDto.FullInfo> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Получение пользователя по ID
     */
    @GetMapping("/{id}")
    public UserDto.FullInfo getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * Обновление данных пользователя
     */
    @PutMapping("/{id}")
    public UserDto.FullInfo updateUser(@PathVariable Long id, @Valid @RequestBody UserDto.UpdateUser dto) {
        return userService.updateUser(id, dto);
    }

    /**
     * Удаление пользователя
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    /**
     * Деактивация пользователя
     */
    @PatchMapping("/{id}/deactivate")
    public UserDto.FullInfo deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    /**
     * Активация пользователя
     */
    @PatchMapping("/{id}/activate")
    public UserDto.FullInfo activateUser(@PathVariable Long id) {
        return userService.activateUser(id);
    }

    /**
     * Смена пароля пользователя
     */
    @PatchMapping("/{id}/change-password")
    public UserDto.FullInfo changePassword(@PathVariable Long id, @Valid @RequestBody UserDto.ChangePassword dto) {
        return userService.changePassword(id, dto);
    }
}
