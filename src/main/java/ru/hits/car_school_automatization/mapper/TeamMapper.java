package ru.hits.car_school_automatization.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.dto.UserShortDto;
import ru.hits.car_school_automatization.entity.Team;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "users", expression = "java(mapUsers(team))")
    TeamDto toDto(Team team);

    List<TeamDto> toDtoList(List<Team> teams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateTeamFromDto(UpdateTeamDto dto, @MappingTarget Team team);

    default Set<UserShortDto> mapUsers(Team team) {
        if (team.getUsers() == null) {
            return Set.of();
        }
        return team.getUsers().stream()
                .map(UserMapperKt::toShort)
                .collect(Collectors.toSet());
    }
}
