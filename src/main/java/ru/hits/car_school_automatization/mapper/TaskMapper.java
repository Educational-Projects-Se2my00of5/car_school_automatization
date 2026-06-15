package ru.hits.car_school_automatization.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hits.car_school_automatization.dto.DeadlinePenaltyDto;
import ru.hits.car_school_automatization.dto.P2PParamDto;
import ru.hits.car_school_automatization.dto.TaskDto;
import ru.hits.car_school_automatization.dto.UpdateTaskDto;
import ru.hits.car_school_automatization.entity.DeadlinePenalty;
import ru.hits.car_school_automatization.entity.P2PParam;
import ru.hits.car_school_automatization.entity.Task;

import java.util.List;

@Mapper(componentModel = "spring", uses = TeamMapper.class)
public abstract class TaskMapper {

    @Mapping(target = "channelId", source = "channel.id")
    @Mapping(target = "teams", source = "teams", qualifiedByName = "toSortedDtoListFromSet")
    @Mapping(target = "p2pParam", source = "p2pParam")
    public abstract TaskDto toDto(Task task);

    public abstract List<TaskDto> toDtoList(List<Task> tasks);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "teams", ignore = true)
    @Mapping(target = "startAt", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "deadlinePenalty", ignore = true)
    @Mapping(target = "p2pParam", ignore = true) // We handle this manually in the service
    @Mapping(target = "isAnonymousVoting", source = "isAnonymousVoting")
    public abstract void updateTaskFromDto(UpdateTaskDto dto, @MappingTarget Task task);

    public abstract P2PParamDto p2pParamToDto(P2PParam p2pParam);

    public DeadlinePenaltyDto map(DeadlinePenalty penalty) {
        if (penalty == null) {
            return null;
        }
        return DeadlinePenaltyDto.builder()
                .unit(penalty.getUnit())
                .step(penalty.getStep())
                .value(penalty.getValue())
                .build();
    }
}
