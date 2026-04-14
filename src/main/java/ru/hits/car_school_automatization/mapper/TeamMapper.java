package ru.hits.car_school_automatization.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.dto.UserShortDto;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "users", expression = "java(mapUsers(team))")
    TeamDto toDto(Team team);

    List<TeamDto> toDtoList(List<Team> teams);

    default List<TeamDto> toSortedDtoList(List<Team> teams) {
        if (teams == null || teams.isEmpty()) {
            return List.of();
        }

        return teams.stream()
                .sorted(Comparator
                        .comparing(Team::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Team::getId, Comparator.nullsFirst(UUID::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Named("toSortedDtoListFromSet")
    default List<TeamDto> toSortedDtoListFromSet(Set<Team> teams) {
        if (teams == null || teams.isEmpty()) {
            return List.of();
        }
        return toSortedDtoList(List.copyOf(teams));
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateTeamFromDto(UpdateTeamDto dto, @MappingTarget Team team);

    default Set<UserShortDto> mapUsers(Team team) {
        if (team.getUsers() == null) {
            return Set.of();
        }

        Comparator<User> byNameThenSurname = Comparator
                .comparing(User::getFirstName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(User::getLastName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));

        return team.getUsers().stream()
                .sorted(byNameThenSurname)
                .map(UserMapperKt::toShort)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
