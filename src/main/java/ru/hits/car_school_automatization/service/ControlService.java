package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.ControlDto;
import ru.hits.car_school_automatization.dto.UpdateControlDto;
import ru.hits.car_school_automatization.entity.Control;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.ControlMapper;
import ru.hits.car_school_automatization.repository.ControlRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ControlService {

    private final ControlRepository controlRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ControlMapper controlMapper;
    private final JwtTokenProvider tokenProvider;

    public Control createControl(Post post, Set<UUID> postTaskIds, Set<UUID> taskIds) {
        if (post.getType() != PostType.CONTROL) {
            throw new BadRequestException("Пост не является контрольной");
        }

        if (controlRepository.existsById(post.getId())) {
            throw new BadRequestException("Контрольная уже существует");
        }

        Set<UUID> validatedPostTasks = validatePostTaskIds(postTaskIds, post.getChannelId());
        Set<UUID> validatedTasks = validateTaskIds(taskIds, post.getChannelId());

        Control control = Control.builder()
                .postId(post.getId())
                .channelId(post.getChannelId())
                .postTaskIds(validatedPostTasks)
                .taskIds(validatedTasks)
                .build();

        return controlRepository.save(control);
    }

    public ControlDto getControl(UUID postId, String authHeader) {
        getUserFromHeader(authHeader);
        Control control = controlRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Контрольная не найдена"));
        return controlMapper.toDto(control);
    }

    public ControlDto updateControl(UUID postId, UpdateControlDto dto, String authHeader) {
        RoleUtils.requireTeacher(getUserFromHeader(authHeader), "Только преподаватель может управлять контрольными");
        Control control = controlRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Контрольная не найдена"));

        Set<UUID> postTaskIds = dto.getPostTaskIds() == null ? control.getPostTaskIds() : validatePostTaskIds(dto.getPostTaskIds(), control.getChannelId());
        Set<UUID> taskIds = dto.getTaskIds() == null ? control.getTaskIds() : validateTaskIds(dto.getTaskIds(), control.getChannelId());

        control.setPostTaskIds(postTaskIds);
        control.setTaskIds(taskIds);

        return controlMapper.toDto(controlRepository.save(control));
    }

    public void deleteControlByPostId(UUID postId) {
        if (controlRepository.existsById(postId)) {
            controlRepository.deleteById(postId);
        }
    }

    private Set<UUID> validatePostTaskIds(Set<UUID> postTaskIds, UUID channelId) {
        if (postTaskIds == null || postTaskIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<UUID> result = new HashSet<>();
        for (UUID postId : postTaskIds) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BadRequestException("Пост задания не найден"));
            if (post.getType() != PostType.TASK) {
                throw new BadRequestException("Пост не является заданием");
            }
            if (!post.getChannelId().equals(channelId)) {
                throw new BadRequestException("Пост не принадлежит предмету контрольной");
            }
            result.add(postId);
        }
        return result;
    }

    private Set<UUID> validateTaskIds(Set<UUID> taskIds, UUID channelId) {
        if (taskIds == null || taskIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<UUID> result = new HashSet<>();
        for (UUID taskId : taskIds) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new BadRequestException("Командное задание не найдено"));
            if (task.getChannel() == null || !task.getChannel().getId().equals(channelId)) {
                throw new BadRequestException("Командное задание не принадлежит предмету контрольной");
            }
            result.add(taskId);
        }
        return result;
    }

    private User getUserFromHeader(String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
