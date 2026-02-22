package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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

    @Operation(summary = "Создание нового пользователя")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto.FullInfo createUser(@Valid @RequestBody UserDto.CreateUser dto) {
        return userService.createUser(dto);
    }

    @Operation(summary = "Получение всех пользователей")
    @GetMapping
    public List<UserDto.FullInfo> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Получение пользователя по ID")
    @GetMapping("/{id}")
    public UserDto.FullInfo getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "Обновление данных пользователя")
    @PutMapping("/{id}")
    public UserDto.FullInfo updateUser(@PathVariable Long id, @Valid @RequestBody UserDto.UpdateUser dto) {
        return userService.updateUser(id, dto);
    }

    @Operation(summary = "Удаление пользователя")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @Operation(summary = "Деактивация пользователя")
    @PatchMapping("/{id}/deactivate")
    public UserDto.FullInfo deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    @Operation(summary = "Активация пользователя")
    @PatchMapping("/{id}/activate")
    public UserDto.FullInfo activateUser(@PathVariable Long id) {
        return userService.activateUser(id);
    }

    @Operation(summary = "Смена пароля текущего пользователя")
    @PatchMapping("/change-password")
    public UserDto.FullInfo changePassword(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserDto.ChangePassword dto) {
        return userService.changePassword(authHeader, dto);
    }

    @Operation(summary = "Получение профиля текущего пользователя")
    @GetMapping("/profile")
    public UserDto.FullInfo getProfile(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return userService.getProfile(authHeader);
    }

    @Operation(summary = "Добавление роли пользователю")
    @PostMapping("/{id}/add-role")
    public UserDto.FullInfo addRole(@PathVariable Long id, @Valid @RequestBody UserDto.RoleOperation dto) {
        return userService.addRole(id, dto);
    }

    @Operation(summary = "Удаление роли у пользователя")
    @PostMapping("/{id}/remove-role")
    public UserDto.FullInfo removeRole(@PathVariable Long id, @Valid @RequestBody UserDto.RoleOperation dto) {
        return userService.removeRole(id, dto);
    }
}
