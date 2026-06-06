package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.P2PPairStatus;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.P2PMapper;
import ru.hits.car_school_automatization.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class P2PService {

    private final P2PParamRepository p2pParamRepository;
    private final P2PPairPersonalRepository p2pPairPersonalRepository;
    private final P2PPairTeamRepository p2pPairTeamRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final P2PMapper p2PMapper;

    @Transactional(readOnly = true)
    public ReviewTasksDto getReviewTasks() {
        // Заглушка
        return new ReviewTasksDto(List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public List<P2PPairPersonalDto> getP2PPairPersonal(UUID postId) {
        return p2pPairPersonalRepository.findByPostId(postId).stream()
                .map(p2PMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<P2PPairTeamDto> getP2PPairTeam(UUID taskId) {
        return p2pPairTeamRepository.findByTaskId(taskId).stream()
                .map(p2PMapper::toDto)
                .toList();
    }

    public P2PPairPersonalDto assignP2PPersonal(AssignP2PPersonalDto dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NotFoundException("Пост не найден"));

        P2PPairPersonal pair = P2PPairPersonal.builder()
                .postId(dto.getPostId())
                .reviewerId(dto.getReviewerId())
                .ownerId(dto.getOwnerId())
                .targetSolutionId(dto.getTargetSolutionId())
                .status(P2PPairStatus.PENDING)
                .build();
        return p2PMapper.toDto(p2pPairPersonalRepository.save(pair));
    }

    public P2PPairTeamDto assignP2PTeam(AssignP2PTeamDto dto) {
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        P2PPairTeam pair = P2PPairTeam.builder()
                .taskId(dto.getTaskId())
                .reviewerTeamId(dto.getReviewerTeamId())
                .ownerTeamId(dto.getOwnerTeamId())
                .targetTaskSolutionId(dto.getTargetTaskSolutionId())
                .status(P2PPairStatus.PENDING)
                .build();
        return p2PMapper.toDto(p2pPairTeamRepository.save(pair));
    }

    public void removeP2PPersonal(UUID pairId) {
        p2pPairPersonalRepository.deleteById(pairId);
    }

    public void removeP2PTeam(UUID pairId) {
        p2pPairTeamRepository.deleteById(pairId);
    }
}
