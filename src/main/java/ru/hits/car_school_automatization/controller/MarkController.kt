package ru.hits.car_school_automatization.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import ru.hits.car_school_automatization.dto.CreateDistributionDto
import ru.hits.car_school_automatization.dto.DistributionDto
import ru.hits.car_school_automatization.dto.MarkResponse
import ru.hits.car_school_automatization.service.UserMarkService
import java.util.*


@RestController
@RequestMapping("/api/mark")
@Tag(name = "Оценки", description = "Управление оценками за задания")
class MarkController(
    private val userMarkService: UserMarkService
) {

    @PostMapping("/user/{userId}/task/{taskId}")
    @Operation(summary = "Установка оценки конкретному пользователю")
    fun setUserMark(
        @PathVariable userId: Long,
        @RequestParam mark: Float,
        @Parameter(hidden = true) @RequestParam isOverride: Boolean = false,
        @PathVariable taskId: UUID,
    ) {
        userMarkService.setUserMark(
            isOverride = isOverride,
            userId = userId,
            mark = mark,
            taskId = taskId
        )
    }

    @PostMapping("/task/{taskId}")
    @Operation(summary = "Распределение оценок по пользователям (применять на всю команду срау)")
    fun reduceMark(
        @RequestBody distribution: Set<CreateDistributionDto>,
        @PathVariable taskId: UUID
    ): List<DistributionDto> {
        return userMarkService.reduceMark(
            distribution = distribution,
            taskId = taskId
        )
    }

    @GetMapping("/user/{userId}/task/{taskId}")
    @Operation(summary = "Получение оценки определенного пользователя")
    fun getUserMark(@PathVariable userId: Long, @PathVariable taskId: UUID): MarkResponse? {
        return userMarkService.getUserMark(userId, taskId)
    }

    @PostMapping("/accept/distribution/task/{taskId}/team/{teamId}")
    @Operation(summary = "Подтверждение распределения оценок")
    fun acceptDistributionMark(@PathVariable taskId: UUID, @PathVariable teamId: UUID) {
        return userMarkService.acceptDistributionMark(taskId, teamId)
    }

    @PostMapping("/reject/distribution/task/{taskId}/team/{teamId}")
    @Operation(summary = "Отклонение распределения оценок")
    fun rejectDistributionMark(@PathVariable taskId: UUID, @PathVariable teamId: UUID) {
        return userMarkService.rejectDistributionMark(taskId, teamId)
    }

    @GetMapping("/distribution/task/{taskId}/team/{teamId}")
    @Operation(summary = "Получение распределения оценок")
    fun getDistributionMark(@PathVariable taskId: UUID, @PathVariable teamId: UUID): List<DistributionDto> {
        return userMarkService.getDistributionMark(taskId, teamId)
    }
}