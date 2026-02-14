package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.UserMapper;
import ru.hits.car_school_automatization.repository.UserRepository;

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
     * Смена пароля пользователя
     */
    public UserDto.FullInfo changePassword(Long id, UserDto.ChangePassword dto) {
        User user = findUserById(id);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный старый пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }
}
