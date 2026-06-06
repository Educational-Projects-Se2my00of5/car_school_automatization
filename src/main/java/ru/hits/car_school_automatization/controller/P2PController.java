package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.service.P2PService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/p2p")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class P2PController {

    private final P2PService p2pService;

    @Operation(summary = "Получение задач к проверке пользователем")
    @GetMapping("/jobs")
    public ReviewTasksDto getReviewTasks() {
        return p2pService.getReviewTasks();
    }

    @Operation(summary = "Получение пар для одиночного задания (поста)")
    @GetMapping("/personal/{postId}")
    public List<P2PPairPersonalDto> getP2PPairPersonal(@PathVariable UUID postId) {
        return p2pService.getP2PPairPersonal(postId);
    }

    @Operation(summary = "Получение пар для командного задания (таски)")
    @GetMapping("/team/{taskId}")
    public List<P2PPairTeamDto> getP2PPairTeam(@PathVariable UUID taskId) {
        return p2pService.getP2PPairTeam(taskId);
    }

    @Operation(summary = "Ручное назначение пары для одиночного задания")
    @PostMapping("/personal/assign")
    public P2PPairPersonalDto assignP2PPersonal(@Valid @RequestBody AssignP2PPersonalDto dto) {
        return p2pService.assignP2PPersonal(dto);
    }

    @Operation(summary = "Ручное назначение пары для командного задания")
    @PostMapping("/team/assign")
    public P2PPairTeamDto assignP2PTeam(@Valid @RequestBody AssignP2PTeamDto dto) {
        return p2pService.assignP2PTeam(dto);
    }

    @Operation(summary = "Отмена P2P назначения для одиночного задания")
    @DeleteMapping("/personal/{pairId}")
    public void removeP2PPersonal(@PathVariable UUID pairId) {
        p2pService.removeP2PPersonal(pairId);
    }

    @Operation(summary = "Отмена P2P назначения для командного задания")
    @DeleteMapping("/team/{pairId}")
    public void removeP2PTeam(@PathVariable UUID pairId) {
        p2pService.removeP2PTeam(pairId);
    }
}
