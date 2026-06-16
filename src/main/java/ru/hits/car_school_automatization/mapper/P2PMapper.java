package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.P2PPairPersonal;
import ru.hits.car_school_automatization.entity.P2PPairTeam;
import ru.hits.car_school_automatization.entity.P2PParam;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

@Mapper(componentModel = "spring")
public abstract class P2PMapper {

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PostRepository postRepository;
    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected TeamRepository teamRepository;
    @Autowired
    protected ru.hits.car_school_automatization.repository.SolutionRepository solutionRepository;
    @Autowired
    protected ru.hits.car_school_automatization.repository.TaskSolutionRepository taskSolutionRepository;
    @Autowired
    protected TaskMapper taskMapper;
    @Autowired
    protected TeamMapper teamMapper;

    public abstract P2PParamDto toDto(P2PParam entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    public abstract P2PParam toEntity(P2PParamDto dto);

    @Mapping(target = "post", expression = "java(postToDto(entity.getPostId()))")
    @Mapping(target = "reviewer", expression = "java(userToShortDto(entity.getReviewerId()))")
    @Mapping(target = "owner", expression = "java(userToShortDto(entity.getOwnerId()))")
    public abstract P2PPairPersonalDto toDto(P2PPairPersonal entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "ownerId", source = "owner.id")
    public abstract P2PPairPersonal toEntity(P2PPairPersonalDto dto);

    @Mapping(target = "task", expression = "java(taskToDto(entity.getTaskId()))")
    @Mapping(target = "reviewerTeam", expression = "java(teamToDto(entity.getReviewerTeamId()))")
    @Mapping(target = "ownerTeam", expression = "java(teamToDto(entity.getOwnerTeamId()))")
    public abstract P2PPairTeamDto toDto(P2PPairTeam entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "reviewerTeamId", source = "reviewerTeam.id")
    @Mapping(target = "ownerTeamId", source = "ownerTeam.id")
    public abstract P2PPairTeam toEntity(P2PPairTeamDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "post", expression = "java(postToDto(entity.getPostId()))")
    @Mapping(target = "owner", expression = "java(userToShortDto(entity.getOwnerId()))")
    @Mapping(target = "targetSolution", expression = "java(solutionToDto(entity.getTargetSolutionId()))")
    @Mapping(target = "status", source = "status")
    public abstract PersonalReviewTaskDto toPersonalReviewTaskDto(P2PPairPersonal entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "task", expression = "java(taskToDto(entity.getTaskId()))")
    @Mapping(target = "ownerTeam", expression = "java(teamToDto(entity.getOwnerTeamId()))")
    @Mapping(target = "targetTaskSolution", expression = "java(taskSolutionToDto(entity.getTargetTaskSolutionId()))")
    @Mapping(target = "status", source = "status")
    public abstract TeamReviewTaskDto toTeamReviewTaskDto(P2PPairTeam entity);

    protected PostDto postToDto(java.util.UUID id) {
        if (id == null) return null;
        return postRepository.findById(id).map(p -> {
            PostDto dto = new PostDto();
            dto.setId(p.getId());
            dto.setLabel(p.getLabel());
            dto.setText(p.getText());
            dto.setType(p.getType());
            dto.setDeadline(p.getDeadline());
            dto.setIsMetricsVisibleToStudents(p.getIsMetricsVisibleToStudents());
            dto.setIsMetricValuesVisibleToStudents(p.getIsMetricValuesVisibleToStudents());
            dto.setIsP2pEnabled(p.getIsP2pEnabled());
            if (p.getAuthorId() != null) {
                userRepository.findById(p.getAuthorId()).ifPresent(author ->
                        dto.setAuthorName(author.getFirstName() + " " + author.getLastName()));
            }
            return dto;
        }).orElse(null);
    }

    protected TaskDto taskToDto(java.util.UUID id) {
        if (id == null) return null;
        return taskRepository.findById(id).map(taskMapper::toDto).orElse(null);
    }

    protected TeamDto teamToDto(java.util.UUID id) {
        if (id == null) return null;
        return teamRepository.findById(id).map(teamMapper::toDto).orElse(null);
    }

    protected UserShortDto userToShortDto(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).map(u -> new UserShortDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail())).orElse(null);
    }

    protected SolutionDto solutionToDto(java.util.UUID id) {
        if (id == null) return null;
        return solutionRepository.findById(id).map(s -> {
            SolutionDto dto = new SolutionDto();
            dto.setId(s.getId());
            dto.setStudentId(s.getStudentId());
            dto.setTaskId(s.getTaskId());
            dto.setText(s.getText());
            dto.setFileUrl(s.getFileUrl());
            dto.setFileName(s.getFileName());
            dto.setSubmittedAt(s.getSubmittedAt());
            dto.setUpdatedAt(s.getUpdatedAt());
            if (s.getStudentId() != null) {
                userRepository.findById(s.getStudentId()).ifPresent(student ->
                        dto.setStudentName(student.getFirstName() + " " + student.getLastName()));
            }
            if (s.getTaskId() != null) {
                postRepository.findById(s.getTaskId()).ifPresent(post ->
                        dto.setTaskLabel(post.getLabel()));
            }
            return dto;
        }).orElse(null);
    }

    protected TaskSolutionDto taskSolutionToDto(java.util.UUID id) {
        if (id == null) return null;
        return taskSolutionRepository.findById(id).map(s -> {
            TaskSolutionDto dto = new TaskSolutionDto();
            dto.setId(s.getId());
            dto.setTaskId(s.getTaskId());
            dto.setStudentId(s.getStudentId());
            dto.setCreatedAt(s.getCreatedAt());
            dto.setUpdatedAt(s.getUpdatedAt());
            if (s.getDocuments() != null) {
                dto.setDocuments(s.getDocuments().stream()
                        .map(document -> TaskDocumentDto.builder()
                                .fileName(document.getFileName())
                                .fileUrl(document.getFileUrl())
                                .build())
                        .toList());
            } else {
                dto.setDocuments(new java.util.ArrayList<>());
            }
            return dto;
        }).orElse(null);
    }
}


