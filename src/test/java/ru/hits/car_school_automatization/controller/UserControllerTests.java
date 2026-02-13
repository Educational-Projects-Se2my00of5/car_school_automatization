package ru.hits.car_school_automatization.controller;

import org.checkerframework.checker.units.qual.s;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
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
import ru.hits.car_school_automatization.service.UserService;
import ru.hits.car_school_automatization.testdata.UserTestData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.hits.car_school_automatization.testdata.UserTestData.createUserRequest;
import static ru.hits.car_school_automatization.testdata.UserTestData.userEntity;
import static ru.hits.car_school_automatization.testdata.UserTestData.userFullInfoDto;

@DisplayName("User Controller Tests")
class UserControllerTests {

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @CsvSource({
            "Anna, Ivanova, 20, +79001112233, anna@test.ru, password, STUDENT",
            "Boris, Petrov, 35, +79002223344, boris@test.ru, password, TEACHER",
            "Olga, Sidorova, 25, +79003334455, olga@test.ru, password,  ADMIN"
    })
    void createUser_withDifferentData(
            String firstName, String lastName, Integer age,
            String phone, String email, String password, String roleString) {
        // Arrange
        Role role = Role.valueOf(roleString);
        String passwordHash = "hash";
        UserDto.CreateUser request = createUserRequest(firstName, lastName, age, phone, email, password, role);
        User userToSave = userEntity(null, firstName, lastName, age, phone, email, passwordHash, role, true);
        User savedUser = userEntity(1L, firstName, lastName, age, phone, email, passwordHash, role, true);
        UserDto.FullInfo expected = userFullInfoDto(1L, firstName, lastName, age, phone, email, role, true);

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
                () -> assertEquals(role, result.getRole()),
                () -> assertEquals(age, result.getAge()),
                () -> assertEquals(phone, result.getPhone())
        );

        verify(passwordEncoder).encode(password);
        verify(userMapper).toEntity(request);
        verify(userRepository).save(userToSave);
        verify(userMapper).toDto(savedUser);
    }

    @ParameterizedTest
    @MethodSource("ru.hits.car_school_automatization.testdata.UserTestData#provideNullFieldsTestData")
    void createUser_withNullField_shouldThrowBadRequestException(UserDto.CreateUser request) {
        assertThrows(
                BadRequestException.class,
                () -> userController.createUser(request)
        );
    }

    @Test
    void getAllUsers() {
        // Arrange
        User user1 = userEntity(1L, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);
        User user2 = userEntity(2L, "Jane", "Smith", 25, "+79002223344", "jane@test.ru", "hash", Role.TEACHER, true);
        User user3 = userEntity(3L, "Bob", "Johnson", 30, "+79003334455", "bob@test.ru", "hash", Role.ADMIN, true);
        List<User> userEntities = Arrays.asList(user1, user2, user3);

        UserDto.FullInfo dto1 = userFullInfoDto(1L, "John", "Doe", 20, "+79001112233", "john@test.ru", Role.STUDENT, true);
        UserDto.FullInfo dto2 = userFullInfoDto(2L, "Jane", "Smith", 25, "+79002223344", "jane@test.ru", Role.TEACHER, true);
        UserDto.FullInfo dto3 = userFullInfoDto(3L, "Bob", "Johnson", 30, "+79003334455", "bob@test.ru", Role.ADMIN, true);
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
        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", Role.STUDENT, true);

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
                "+79991112233", "updated@test.ru", Role.TEACHER
        );

        User existingUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);
        User updatedUser = userEntity(userId, "UpdatedName", "UpdatedLastName", 25, "+79991112233", "updated@test.ru", "hash", Role.TEACHER, true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "UpdatedName", "UpdatedLastName", 25, "+79991112233", "updated@test.ru", Role.TEACHER, true);

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
                () -> assertEquals(Role.TEACHER, result.getRole())
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
                "+79991112233", "updated@test.ru", Role.TEACHER
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
    @MethodSource("ru.hits.car_school_automatization.testdata.UserTestData#provideNullFieldsTestDataForUpdate")
    void updateUser_withNullField_shouldThrowBadRequestException(UserDto.UpdateUser updateRequest) {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        assertThrows(
                BadRequestException.class,
                () -> userController.updateUser(userId, updateRequest)
        );
    }
        

    @Test
    void deleteUser_shouldDeleteSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);

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
        User activeUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);
        User deactivatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, false);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", Role.STUDENT, false);

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
        User inactiveUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, false);
        User activatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", "hash", Role.STUDENT, true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", Role.STUDENT, true);

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
    void changePassword() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";
        String oldPasswordHash = "oldHash";
        String newPasswordHash = "newHash";

        UserDto.ChangePassword request = UserTestData.changePasswordRequest(oldPassword, newPassword);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", oldPasswordHash, Role.STUDENT, true);
        User updatedUser = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", newPasswordHash, Role.STUDENT, true);
        UserDto.FullInfo expected = userFullInfoDto(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", Role.STUDENT, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, oldPasswordHash)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expected);

        // Act
        UserDto.FullInfo result = userController.changePassword(userId, request);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId())
        );

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, oldPasswordHash);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
        verify(userMapper).toDto(updatedUser);
    }

    @Test
    void changePassword_whenUserNotFound_shouldThrowNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        UserDto.ChangePassword request = UserTestData.changePasswordRequest("oldPassword", "newPassword");

        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.changePassword(nonExistentId, request)
        );

        assertEquals("Пользователь с id " + nonExistentId + " не найден", exception.getMessage());
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void changePassword_whenOldPasswordIncorrect_shouldThrowBadRequestException() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword456";
        String actualPasswordHash = "actualHash";

        UserDto.ChangePassword request = UserTestData.changePasswordRequest(oldPassword, newPassword);

        User user = userEntity(userId, "John", "Doe", 20, "+79001112233", "john@test.ru", actualPasswordHash, Role.STUDENT, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, actualPasswordHash)).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userController.changePassword(userId, request)
        );

        assertEquals("Неверный старый пароль", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, actualPasswordHash);
    }

    @ParameterizedTest
    @CsvSource({
            ", newPassword",
            "oldPassword, "
    })
    void changePassword_withNullFields_shouldThrowBadRequestException(String oldPassword, String newPassword) {
        // Arrange
        Long userId = 1L;
        UserDto.ChangePassword request = UserTestData.changePasswordRequest(oldPassword, newPassword);

        // Act & Assert
        assertThrows(
                BadRequestException.class,
                () -> userController.changePassword(userId, request)
        );
    }
}
