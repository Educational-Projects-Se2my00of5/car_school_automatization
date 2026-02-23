package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.Role;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.RoleName;
import ru.hits.car_school_automatization.repository.RoleRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования User entity в DTO и обратно
 */
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected RoleRepository roleRepository;

    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "roles", source = "roleName", qualifiedByName = "roleNamesToRoles")
    public abstract User toEntity(UserDto.CreateUser dto);

    @Mapping(target = "roles", source = "dto.roleName", qualifiedByName = "roleNamesToRoles")
    public abstract void updateEntity(@MappingTarget User user, UserDto.UpdateUser dto);


    @Mapping(target = "roleName", source = "roles", qualifiedByName = "rolesToRoleNames")
    public abstract UserDto.FullInfo toDto(User user);



    @Named("rolesToRoleNames")
    protected Set<RoleName> rolesToRoleNames(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    @Named("roleNamesToRoles")
    protected Set<Role> roleNamesToRoles(Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>();
        }
        return roleNames.stream()
                .map(roleRepository::findByName)
                .collect(Collectors.toSet());
    }
}

