package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.UserMapper;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пользователями
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Создание нового пользователя
     */
    public UserDto.FullInfo createUser(UserDto.CreateUser dto) {
        User user = userMapper.toEntity(dto);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Получение всех пользователей
     */
    @Transactional(readOnly = true)
    public List<UserDto.FullInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение пользователя по ID
     */
    @Transactional(readOnly = true)
    public UserDto.FullInfo getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toDto(user);
    }

    /**
     * Обновление данных пользователя
     */
    public UserDto.FullInfo updateUser(Long id, UserDto.UpdateUser dto) {
        User user = findUserById(id);

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAge(dto.getAge());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());

        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Удаление пользователя
     */
    public void deleteUser(Long id) {
        User user = findUserById(id);
        userRepository.delete(user);
    }

    /**
     * Деактивация пользователя
     */
    public UserDto.FullInfo deactivateUser(Long id) {
        User user = findUserById(id);

        if (!user.getIsActive()) {
            throw new BadRequestException("Пользователь уже деактивирован");
        }

        user.setIsActive(false);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Активация пользователя
     */
    public UserDto.FullInfo activateUser(Long id) {
        User user = findUserById(id);

        if (user.getIsActive()) {
            throw new BadRequestException("Пользователь уже активирован");
        }

        user.setIsActive(true);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Смена пароля пользователя по токену
     */
    public UserDto.FullInfo changePassword(String authHeader, UserDto.ChangePassword dto) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Невалидный или истёкший токен");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = findUserById(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный старый пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Получение профиля текущего пользователя по токену
     */
    @Transactional(readOnly = true)
    public UserDto.FullInfo getProfile(String authHeader) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Невалидный или истёкший токен");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = findUserById(userId);
        return userMapper.toDto(user);
    }

    /**
     * Добавление роли пользователю
     */
    public UserDto.FullInfo addRole(Long id, UserDto.RoleOperation dto) {
        User user = findUserById(id);
        
        // Проверяем, есть ли уже такая роль
        if (user.getRole().contains(dto.getRole())) {
            // Роль уже есть, просто возвращаем текущее состояние без сохранения
            return userMapper.toDto(user);
        }
        
        // Добавляем новую роль
        List<Role> updatedRoles = new ArrayList<>(user.getRole());
        updatedRoles.add(dto.getRole());
        user.setRole(updatedRoles);
        
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Удаление роли у пользователя
     */
    public UserDto.FullInfo removeRole(Long id, UserDto.RoleOperation dto) {
        User user = findUserById(id);
        
        // Проверяем, есть ли роль у пользователя
        if (!user.getRole().contains(dto.getRole())) {
            throw new BadRequestException("У пользователя нет роли " + dto.getRole());
        }
        
        // Проверяем, не последняя ли это роль
        if (user.getRole().size() == 1) {
            throw new BadRequestException("Невозможно удалить последнюю роль пользователя");
        }
        
        // Удаляем роль
        List<Role> updatedRoles = new ArrayList<>(user.getRole());
        updatedRoles.remove(dto.getRole());
        user.setRole(updatedRoles);
        
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }
}
