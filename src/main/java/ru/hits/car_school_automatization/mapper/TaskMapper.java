package ru.hits.car_school_automatization.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.hits.car_school_automatization.dto.TaskDto;
import ru.hits.car_school_automatization.dto.UpdateTaskDto;
import ru.hits.car_school_automatization.entity.Task;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "channelId", source = "channel.id")
    TaskDto toDto(Task task);

    List<TaskDto> toDtoList(List<Task> tasks);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "teams", ignore = true)
    @Mapping(target = "startAt", ignore = true)
    @Mapping(target = "documents", ignore = true)
    void updateTaskFromDto(UpdateTaskDto dto, @MappingTarget Task task);
}
