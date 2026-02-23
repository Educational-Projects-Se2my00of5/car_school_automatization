package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.hits.car_school_automatization.enums.RoleName;

/**
 * Сущность роли пользователя
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleName name;
}
