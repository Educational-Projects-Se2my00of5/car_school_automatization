package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.P2PPairStatus;
import ru.hits.car_school_automatization.enums.P2PType;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.P2PMapper;
import ru.hits.car_school_automatization.repository.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class P2PService {

    private final P2PParamRepository p2pParamRepository;
    private final P2PPairPersonalRepository p2pPairPersonalRepository;
    private final P2PPairTeamRepository p2pPairTeamRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final SolutionRepository solutionRepository;
    private final TaskSolutionRepository taskSolutionRepository;
    private final P2PMapper p2PMapper;

    @Transactional(readOnly = true)
    public ReviewTasksDto getReviewTasks() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<PersonalReviewTaskDto> personalTasks = p2pPairPersonalRepository.findByReviewerId(userId)
                .stream()
                .map(p2PMapper::toPersonalReviewTaskDto)
                .collect(Collectors.toList());

        List<UUID> teamIds = teamRepository.findByUsers_Id(userId)
                .stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        List<TeamReviewTaskDto> teamTasks = teamIds.isEmpty() ? List.of() :
                p2pPairTeamRepository.findByReviewerTeamIdIn(teamIds)
                        .stream()
                        .map(p2PMapper::toTeamReviewTaskDto)
                        .collect(Collectors.toList());

        return new ReviewTasksDto(personalTasks, teamTasks);
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

    public void generateP2PForPost(UUID postId) {
        P2PParam param = p2pParamRepository.findById(postId).orElse(null);
        if (param == null || param.getType() != P2PType.RANDOM) {
            return;
        }

        List<Solution> solutions = new ArrayList<>(solutionRepository.findByTaskId(postId));
        if (solutions.size() < 2) {
            log.warn("Недостаточно решений для P2P по посту: {}", postId);
            return;
        }

        Collections.shuffle(solutions);
        List<P2PPairPersonal> pairs = new ArrayList<>();

        for (int i = 0; i < solutions.size(); i++) {
            Solution reviewer = solutions.get(i);
            Solution target = solutions.get((i + 1) % solutions.size());

            P2PPairPersonal pair = P2PPairPersonal.builder()
                    .postId(postId)
                    .reviewerId(reviewer.getStudentId())
                    .ownerId(target.getStudentId())
                    .targetSolutionId(target.getId())
                    .status(P2PPairStatus.PENDING)
                    .build();
            pairs.add(pair);
        }

        p2pPairPersonalRepository.saveAll(pairs);
        log.info("Сгенерировано {} P2P-пар для поста {}", pairs.size(), postId);
    }

    public void generateP2PForTask(UUID taskId) {
        P2PParam param = p2pParamRepository.findById(taskId).orElse(null);
        if (param == null || param.getType() != P2PType.RANDOM) {
            return;
        }

        if (!p2pPairTeamRepository.findByTaskId(taskId).isEmpty()) {
            return; // Уже сгенерировано
        }

        List<Team> teams = teamRepository.findByTask_Id(taskId);
        List<TaskSolution> selectedSolutions = new ArrayList<>();

        for (Team team : teams) {
            taskSolutionRepository.findByTaskIdAndTeamIdAndIsSelectedTrue(taskId, team.getId())
                    .ifPresent(selectedSolutions::add);
        }

        if (selectedSolutions.size() < 2) {
            log.warn("Недостаточно командных решений для P2P по таске: {}", taskId);
            return;
        }

        Collections.shuffle(selectedSolutions);
        List<P2PPairTeam> pairs = new ArrayList<>();

        for (int i = 0; i < selectedSolutions.size(); i++) {
            TaskSolution reviewerSolution = selectedSolutions.get(i);
            TaskSolution targetSolution = selectedSolutions.get((i + 1) % selectedSolutions.size());

            P2PPairTeam pair = P2PPairTeam.builder()
                    .taskId(taskId)
                    .reviewerTeamId(reviewerSolution.getTeamId())
                    .ownerTeamId(targetSolution.getTeamId())
                    .targetTaskSolutionId(targetSolution.getId())
                    .status(P2PPairStatus.PENDING)
                    .build();
            pairs.add(pair);
        }

        p2pPairTeamRepository.saveAll(pairs);
        log.info("Сгенерировано {} P2P-пар команд для таски {}", pairs.size(), taskId);
    }
}
