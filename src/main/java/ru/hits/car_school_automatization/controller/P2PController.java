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
    public List<P2PPairPersonalDto> getP2PPairPersonal(@PathVariable UUID postId, @RequestHeader("Authorization") String authHeader) {
        return p2pService.getP2PPairPersonal(postId, authHeader);
    }

    @Operation(summary = "Получение пар для командного задания (таски)")
    @GetMapping("/team/{taskId}")
    public List<P2PPairTeamDto> getP2PPairTeam(@PathVariable UUID taskId, @RequestHeader("Authorization") String authHeader) {
        return p2pService.getP2PPairTeam(taskId, authHeader);
    }

    @Operation(summary = "Ручное назначение пары для одиночного задания")
    @PostMapping("/personal/assign")
    public P2PPairPersonalDto assignP2PPersonal(@Valid @RequestBody AssignP2PPersonalDto dto, @RequestHeader("Authorization") String authHeader) {
        return p2pService.assignP2PPersonal(dto, authHeader);
    }

    @Operation(summary = "Ручное назначение пары для командного задания")
    @PostMapping("/team/assign")
    public P2PPairTeamDto assignP2PTeam(@Valid @RequestBody AssignP2PTeamDto dto, @RequestHeader("Authorization") String authHeader) {
        return p2pService.assignP2PTeam(dto, authHeader);
    }

    @Operation(summary = "Переназначение проверяющего для одиночного задания")
    @PatchMapping("/personal/{pairId}/reassign")
    public P2PPairPersonalDto reassignP2PPersonal(@PathVariable UUID pairId, @Valid @RequestBody ReassignP2PPersonalDto dto, @RequestHeader("Authorization") String authHeader) {
        return p2pService.reassignP2PPersonal(pairId, dto, authHeader);
    }

    @Operation(summary = "Переназначение проверяющей команды для командного задания")
    @PatchMapping("/team/{pairId}/reassign")
    public P2PPairTeamDto reassignP2PTeam(@PathVariable UUID pairId, @Valid @RequestBody ReassignP2PTeamDto dto, @RequestHeader("Authorization") String authHeader) {
        return p2pService.reassignP2PTeam(pairId, dto, authHeader);
    }

    @Operation(summary = "Отмена P2P назначения для одиночного задания")
    @DeleteMapping("/personal/{pairId}")
    public void removeP2PPersonal(@PathVariable UUID pairId, @RequestHeader("Authorization") String authHeader) {
        p2pService.removeP2PPersonal(pairId, authHeader);
    }

    @Operation(summary = "Отмена P2P назначения для командного задания")
    @DeleteMapping("/team/{pairId}")
    public void removeP2PTeam(@PathVariable UUID pairId, @RequestHeader("Authorization") String authHeader) {
        p2pService.removeP2PTeam(pairId, authHeader);
    }
}
