package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    /**
     * Поиск пользователей по фильтрам
     * @param name поиск по имени или фамилии (поддерживает частичное совпадение)
     * @param email поиск по email (поддерживает частичное совпадение)
     * @param role фильтрация по роли
     * @return список найденных пользователей
     */
    @Query(value = "SELECT * FROM users WHERE " +
            "(:name IS NULL OR LOWER(first_name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(last_name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:email IS NULL OR LOWER(email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "AND (:role IS NULL OR :role = ANY(role))", nativeQuery = true)
    List<User> findByFilters(@Param("name") String name,
                             @Param("email") String email,
                             @Param("role") String role);
}
