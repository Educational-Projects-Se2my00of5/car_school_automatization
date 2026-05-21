package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.hits.car_school_automatization.dto.ControlDto;
import ru.hits.car_school_automatization.dto.IdLabelDto;
import ru.hits.car_school_automatization.entity.Control;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ControlMapper {

    @Mapping(target = "postTaskIds", source = "postTaskIds", qualifiedByName = "postTaskIdsToDto")
    @Mapping(target = "taskIds", source = "taskIds", qualifiedByName = "taskIdsToDto")
    ControlDto toDto(Control control, @Context PostRepository postRepository, @Context TaskRepository taskRepository);

    @Named("postTaskIdsToDto")
    default Set<IdLabelDto> postTaskIdsToDto(Set<UUID> postTaskIds, @Context PostRepository postRepository) {
        if (postTaskIds == null || postTaskIds.isEmpty()) {
            return new HashSet<>();
        }

        Map<UUID, String> labelsById = postRepository.findAllById(postTaskIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, Post::getLabel));

        return postTaskIds.stream()
                .map(id -> IdLabelDto.builder().id(id).label(labelsById.get(id)).build())
                .collect(Collectors.toSet());
    }

    @Named("taskIdsToDto")
    default Set<IdLabelDto> taskIdsToDto(Set<UUID> taskIds, @Context TaskRepository taskRepository) {
        if (taskIds == null || taskIds.isEmpty()) {
            return new HashSet<>();
        }

        Map<UUID, String> labelsById = taskRepository.findAllById(taskIds)
                .stream()
                .collect(Collectors.toMap(Task::getId, Task::getLabel));

        return taskIds.stream()
                .map(id -> IdLabelDto.builder().id(id).label(labelsById.get(id)).build())
                .collect(Collectors.toSet());
    }
}
