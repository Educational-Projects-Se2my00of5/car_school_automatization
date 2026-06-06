package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;

@Mapper(componentModel = "spring")
public interface P2PMapper {

    P2PParamDto toDto(P2PParam entity);

    @Mapping(target = "id", ignore = true)
    P2PParam toEntity(P2PParamDto dto);

    P2PPairPersonalDto toDto(P2PPairPersonal entity);

    @Mapping(target = "id", ignore = true)
    P2PPairPersonal toEntity(P2PPairPersonalDto dto);

    P2PPairTeamDto toDto(P2PPairTeam entity);

    @Mapping(target = "id", ignore = true)
    P2PPairTeam toEntity(P2PPairTeamDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "targetSolutionId", source = "targetSolutionId")
    @Mapping(target = "status", source = "status")
    PersonalReviewTaskDto toPersonalReviewTaskDto(P2PPairPersonal entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "ownerTeamId", source = "ownerTeamId")
    @Mapping(target = "targetTaskSolutionId", source = "targetTaskSolutionId")
    @Mapping(target = "status", source = "status")
    TeamReviewTaskDto toTeamReviewTaskDto(P2PPairTeam entity);
}
