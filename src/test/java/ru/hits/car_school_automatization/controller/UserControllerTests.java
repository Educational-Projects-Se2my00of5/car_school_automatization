package ru.hits.car_school_automatization.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.UserMapper;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.service.JwtTokenProvider;
import ru.hits.car_school_automatization.service.UserService;
import ru.hits.car_school_automatization.testdata.UserTestData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static ru.hits.car_school_automatization.testdata.AuthTestData.authHeader;
import static ru.hits.car_school_automatization.testdata.UserTestData.createUserRequest;
import static ru.hits.car_school_automatization.testdata.UserTestData.userEntity;
import static ru.hits.car_school_automatization.testdata.UserTestData.userFullInfoDto;

@DisplayName("User Controller Tests")
class UserControllerTests {

    private UserController userController;
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, userMapper, passwordEncoder, jwtTokenProvider);
        userController = new UserController(userService);
    }

    @ParameterizedTest
    @CsvSource({
            "Anna, Ivanova, 20, +79001112233, anna@test.ru, password, STUDENT",
            "Boris, Petrov, 35, +79002223344, boris@test.ru, password, TEACHER",
            "Olga, Sidorova, 25, +79003334455, olga@test.ru, password,  MANAGER"
    })
    void createUser_withDifferentData(
            String firstName, String lastName, Integer age,
            String phone, String email, String password, String roleString) {
        // Arrange
        Role role = Role.valueOf(roleString);
        List<Role> roles = List.of(role);
        String passwordHash = "hash";
        UserDto.CreateUser request = createUserRequest(firstName, lastName, age, phone, email, password, roles);
        User userToSave = userEntity(null, firstName, lastName, age, phone, email, passwordHash, roles, true);
        User savedUser = userEntity(1L, firstName, lastName, age, phone, email, passwordHash, roles, true);
        UserDto.FullInfo expected = userFullInfoDto(1L, firstName, lastName, age, phone, email, roles, true);

        when(passwordEncoder.encode(password)).thenReturn(passwordHash);
        when(userMapper.toEntity(request)).thenReturn(userToSave);
        when(userRepository.save(userToSave)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.createUser(request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(expected, result),
                () -> assertEquals(firstName, result.getFirstName()),
                () -> assertEquals(lastName, result.getLastName()),
                () -> assertEquals(email, result.getEmail()),
                () -> assertEquals(roles, result.getRole()),
                () -> assertEquals(age, result.getAge()),
                () -> assertEquals(phone, result.getPhone())
        );

        verify(passwordEncoder).encode(password);
        verify(userMapper).toEntity(request);
        verify(userRepository).save(userToSave);
        verify(userMapper).toDto(savedUser);
    }

    @Test
    void getAllUsers() {
        // Arrange
        User user1 = userEntity(1L, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        User user2 = userEntity(2L, "Jane", "Smith", 25, "+79002223344", "jane@test.ru", "hash", List.of(Role.TEACHER), true);
        User user3 = userEntity(3L, "Bob", "Johnson", 30, "+79003334455", "bob@test.ru", "hash", List.of(Role.MANAGER), true);
        List<User> userEntities = Arrays.asList(user1, user2, user3);

        UserDto.FullInfo dto1 = userFullInfoDto(1L, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);
        UserDto.FullInfo dto2 = userFullInfoDto(2L, "Jane", "Smith", 25, "+79002223344", "jane@test.ru", List.of(Role.TEACHER), true);
        UserDto.FullInfo dto3 = userFullInfoDto(3L, "Bob", "Johnson", 30, "+79003334455", "bob@test.ru", List.of(Role.MANAGER), true);
        List<UserDto.FullInfo> expectedDtos = Arrays.asList(dto1, dto2, dto3);

        when(userRepository.findAll()).thenReturn(userEntities);
        when(userMapper.toDto(user1)).thenReturn(dto1);
        when(userMapper.toDto(user2)).thenReturn(dto2);
        when(userMapper.toDto(user3)).thenReturn(dto3);

        // Act
        List<UserDto.FullInfo> result = userController.getAllUsers();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(3, result.size()),
                () -> assertEquals(expectedDtos, result)
        );

        verify(userRepository).findAll();
        verify(userMapper).toDto(user1);
        verify(userMapper).toDto(user2);
        verify(userMapper).toDto(user3);
    }

    @Test
    void getUserById() {
        // Arrange
        Long userId = 10L;
        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.getUserById(userId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals("John", result.getFirstName()),
                () -> assertEquals("Doe", result.getLastName()),
                () -> assertEquals("john@test.ru", result.getEmail())
        );

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    void getUserById_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.getUserById(nonExistentId)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        // Arrange
        Long userId = 1L;
        UserDto.UpdateUser updateRequest = UserTestData.updateUserRequest(
                "UpdatedName", "UpdatedLastName", 25,
                "+79991112233", "updated@test.ru", List.of(Role.TEACHER)
        );

        User existingUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        User updatedUser = userEntity(userId, "UpdatedName", "UpdatedLastName", 25, "+79991112233", "updated@test.ru", "hash", List.of(Role.TEACHER), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "UpdatedName", "UpdatedLastName", 25, "+79991112233", "updated@test.ru", List.of(Role.TEACHER), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.updateUser(userId, updateRequest);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals("UpdatedName", result.getFirstName()),
                () -> assertEquals("UpdatedLastName", result.getLastName()),
                () -> assertEquals("updated@test.ru", result.getEmail()),
                () -> assertEquals(List.of(Role.TEACHER), result.getRole())
        );

        verify(userRepository).findById(userId);
        verify(userRepository).save(existingUser);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    void updateUser_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        UserDto.UpdateUser updateRequest = UserTestData.updateUserRequest(
                "UpdatedName", "UpdatedLastName", 25,
                "+79991112233", "updated@test.ru", List.of(Role.TEACHER)
        );

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.updateUser(nonExistentId, updateRequest)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void deleteUser_shouldDeleteSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userController.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.deleteUser(nonExistentId)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void deactivateUser_shouldDeactivateSuccessfully() {
        // Arrange
        Long userId = 1L;
        User activeUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        User deactivatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), false);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(deactivatedUser);
        when(userMapper.toDto(deactivatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.deactivateUser(userId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals(false, result.getIsActive()),
                () -> assertEquals("John", result.getFirstName())
        );

        verify(userRepository).findById(userId);
        verify(userRepository).save(activeUser);
        verify(userMapper).toDto(deactivatedUser);
    }

    @Test
    void deactivateUser_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.deactivateUser(nonExistentId)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void activateUser_shouldActivateSuccessfully() {
        // Arrange
        Long userId = 1L;
        User inactiveUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), false);
        User activatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(inactiveUser)).thenReturn(activatedUser);
        when(userMapper.toDto(activatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.activateUser(userId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals(true, result.getIsActive()),
                () -> assertEquals("John", result.getFirstName())
        );

        verify(userRepository).findById(userId);
        verify(userRepository).save(inactiveUser);
        verify(userMapper).toDto(activatedUser);
    }

    @Test
    void activateUser_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.activateUser(nonExistentId)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Успешная смена пароля с валидным токеном")
    void changePassword_withValidToken_shouldChangePasswordSuccessfully() {
        // Arrange
        String token = "valid-token";
        String authHeaderValue = authHeader(token);
        Long userId = 1L;
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";
        String oldPasswordHash = "oldHash";
        String newPasswordHash = "newHash";

        UserDto.ChangePassword request = UserTestData.changePasswordRequest(oldPassword, newPassword);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", oldPasswordHash, List.of(Role.STUDENT), true);
        User updatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", newPasswordHash, List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(jwtTokenProvider.extractTokenFromHeader(authHeaderValue)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, oldPasswordHash)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.changePassword(authHeaderValue, request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId())
        );

        verify(jwtTokenProvider).extractTokenFromHeader(authHeaderValue);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, oldPasswordHash);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    @DisplayName("Смена пароля с невалидным токеном должна выбросить исключение")
    void changePassword_withInvalidToken_shouldThrowBadRequestException() {
        // Arrange
        String token = "invalid-token";
        String authHeaderValue = authHeader(token);
        UserDto.ChangePassword request = UserTestData.changePasswordRequest("oldPassword", "newPassword");

        when(jwtTokenProvider.extractTokenFromHeader(authHeaderValue)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.changePassword(authHeaderValue, request)
        );

        assertEquals("Невалидный или истёкший токен", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(authHeaderValue);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Смена пароля с неверным старым паролем должна выбросить исключение")
    void changePassword_withIncorrectOldPassword_shouldThrowBadRequestException() {
        // Arrange
        String token = "valid-token";
        String authHeaderValue = authHeader(token);
        Long userId = 1L;
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword456";
        String actualPasswordHash = "actualHash";

        UserDto.ChangePassword request = UserTestData.changePasswordRequest(oldPassword, newPassword);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", actualPasswordHash, List.of(Role.STUDENT), true);

        when(jwtTokenProvider.extractTokenFromHeader(authHeaderValue)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, actualPasswordHash)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.changePassword(authHeaderValue, request)
        );

        assertEquals("Неверный старый пароль", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(authHeaderValue);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, actualPasswordHash);
    }

    @Test
    @DisplayName("Успешное получение профиля по токену")
    void getProfile_withValidToken_shouldReturnUserProfile() {
        // Arrange
        String token = "valid-token";
        String authHeaderValue = authHeader(token);
        Long userId = 1L;

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(jwtTokenProvider.extractTokenFromHeader(authHeaderValue)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.getProfile(authHeaderValue);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals("John", result.getFirstName()),
                () -> assertEquals("Doe", result.getLastName()),
                () -> assertEquals("john@test.ru", result.getEmail())
        );

        verify(jwtTokenProvider).extractTokenFromHeader(authHeaderValue);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Получение профиля с невалидным токеном должно выбросить исключение")
    void getProfile_withInvalidToken_shouldThrowBadRequestException() {
        // Arrange
        String token = "invalid-token";
        String authHeaderValue = authHeader(token);

        when(jwtTokenProvider.extractTokenFromHeader(authHeaderValue)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.getProfile(authHeaderValue)
        );

        assertEquals("Невалидный или истёкший токен", exception.getMessage());
        verify(jwtTokenProvider).extractTokenFromHeader(authHeaderValue);
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Успешное добавление роли пользователю")
    void addRole_withValidData_shouldAddRoleSuccessfully() {
        // Arrange
        Long userId = 1L;
        Role roleToAdd = Role.TEACHER;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(roleToAdd);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        User updatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT, Role.TEACHER), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT, Role.TEACHER), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.addRole(userId, request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertTrue(result.getRole().contains(Role.STUDENT)),
                () -> assertTrue(result.getRole().contains(Role.TEACHER)),
                () -> assertEquals(2, result.getRole().size())
        );

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    @DisplayName("Добавление роли, которая уже есть у пользователя - роль не дублируется")
    void addRole_whenRoleAlreadyExists_shouldNotDuplicateRole() {
        // Arrange
        Long userId = 1L;
        Role existingRole = Role.STUDENT;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(existingRole);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.addRole(userId, request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertTrue(result.getRole().contains(Role.STUDENT)),
                () -> assertEquals(1, result.getRole().size())
        );

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("Добавление роли несуществующему пользователю должно выбросить исключение")
    void addRole_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(Role.TEACHER);

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.addRole(nonExistentId, request)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Успешное удаление роли у пользователя")
    void removeRole_withValidData_shouldRemoveRoleSuccessfully() {
        // Arrange
        Long userId = 1L;
        Role roleToRemove = Role.TEACHER;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(roleToRemove);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT, Role.TEACHER), true);
        User updatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.removeRole(userId, request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertTrue(result.getRole().contains(Role.STUDENT)),
                () -> assertFalse(result.getRole().contains(Role.TEACHER)),
                () -> assertEquals(1, result.getRole().size())
        );

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    @DisplayName("Удаление роли, которой нет у пользователя - должно выбросить исключение")
    void removeRole_whenRoleDoesNotExist_shouldThrowBadRequestException() {
        // Arrange
        Long userId = 1L;
        Role roleToRemove = Role.TEACHER;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(roleToRemove);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.removeRole(userId, request)
        );

        assertEquals("У пользователя нет роли " + roleToRemove, exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Удаление последней роли у пользователя должно выбросить исключение")
    void removeRole_whenRemovingLastRole_shouldThrowBadRequestException() {
        // Arrange
        Long userId = 1L;
        Role lastRole = Role.STUDENT;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(lastRole);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", List.of(Role.STUDENT), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.removeRole(userId, request)
        );

        assertEquals("Невозможно удалить последнюю роль пользователя", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Удаление роли у несуществующего пользователя должно выбросить исключение")
    void removeRole_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        UserDto.RoleOperation request = UserTestData.roleOperationRequest(Role.TEACHER);

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.removeRole(nonExistentId, request)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }
}
