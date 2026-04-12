package ru.hits.car_school_automatization.service

import org.springframework.stereotype.Service
import ru.hits.car_school_automatization.dto.CreateDistributionDto
import ru.hits.car_school_automatization.dto.DistributionDto
import ru.hits.car_school_automatization.dto.MarkResponse
import ru.hits.car_school_automatization.entity.UserMark
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.repository.TaskRepository
import ru.hits.car_school_automatization.repository.TeamRepository
import ru.hits.car_school_automatization.repository.UserMarkRepository
import ru.hits.car_school_automatization.sumOf
import java.util.*
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Service
class UserMarkService(
    private val userMarkRepository: UserMarkRepository,
    private val userService: UserService,
    private val taskRepository: TaskRepository,
    private val teamRepository: TeamRepository
) {

    fun setUserMark(
        userId: Long,
        isOverride: Boolean,
        mark: Float,
        taskId: UUID,
    ) {
        val task = userMarkRepository.findMarkByUserIdAndTaskId(userId, taskId)?.let {
            if (!isOverride) {
                throw BadRequestException("User Mark already exist")
            }
            if (it.isAccessedDistribution) throw BadRequestException("User Mark already accessed")
            it
        }

        userMarkRepository.save(
            task?.copy(
                mark = mark,
            ) ?: UserMark(
                userId = userId, taskId = taskId, mark = mark
            )
        )
    }

    fun reduceMark(
        distribution: Set<CreateDistributionDto>, taskId: UUID
    ): List<DistributionDto> {
        if (distribution.isEmpty()) {
            throw BadRequestException("User Mark does not contain any distribution")
        }
        val teams = distribution.mapNotNull { teamRepository.findByTask_IdAndUsers_Id(taskId, it.userId).getOrNull() }
        if (teams.any { teams.first() != it }) {
            throw BadRequestException("Not all users are from the same team")
        }
        if (teams.first().users == null) {
            throw BadRequestException("Team ${teams.first().id} does not contain any users")
        }
        val sumOfMarks = teams.first().mark * teams.first().users!!.size
        val distributionMarksSum = distribution.sumOf { it.mark }
        if (sumOfMarks < distributionMarksSum) {
            throw BadRequestException("Incorrect mark count")
        }
        val distributionMarks = distribution.map {
            val lastMark = userMarkRepository.findMarkByUserIdAndTaskId(it.userId, taskId)
            if (lastMark != null) userMarkRepository.save(lastMark.copy(mark = it.mark))
            else userMarkRepository.save(UserMark(userId = it.userId, taskId = taskId, mark = it.mark))
        }
        val otherTeammates = teams.first().users.filter { !distribution.map { it.userId }.contains(it.id) }
        val different = sumOfMarks - distributionMarksSum
        return (otherTeammates.map {
            val lastMark = userMarkRepository.findMarkByUserIdAndTaskId(it.id, taskId)
            if (lastMark != null) userMarkRepository.save(lastMark.copy(mark = different / otherTeammates.size))
            else userMarkRepository.save(
                UserMark(
                    userId = it.id, taskId = taskId, mark = different / otherTeammates.size
                )
            )
        } + distributionMarks).map {
            DistributionDto(userService.getUserById(it.userId), it.mark)
        }
    }

    fun getUserMark(userId: Long, taskId: UUID): MarkResponse? {
        return userMarkRepository.findMarkByUserIdAndTaskId(userId, taskId)?.let {
            val user = userService.getUserById(userId)
            MarkResponse(
                user = user, mark = it.mark
            )
        }
    }

    fun acceptDistributionMark(taskId: UUID, teamId: UUID) {
        val team =
            teamRepository.findById(teamId).getOrElse { throw BadRequestException("Team $teamId does not exist") }
        taskRepository.findById(taskId).getOrElse { throw BadRequestException("Task $taskId does not exist") }

        team.users.forEach {
            val mark = userMarkRepository.findMarkByUserIdAndTaskId(it.id, taskId)
                ?: throw BadRequestException("User $it doesn't have mark")
            userMarkRepository.save(mark.copy(isAccessedDistribution = true))
        }
    }

    fun rejectDistributionMark(taskId: UUID, teamId: UUID) {
        val team =
            teamRepository.findById(teamId).getOrElse { throw BadRequestException("Team $teamId does not exist") }
        taskRepository.findById(taskId).getOrElse { throw BadRequestException("Task $taskId does not exist") }

        team.users.forEach {
            val mark = userMarkRepository.findMarkByUserIdAndTaskId(it.id, taskId)
                ?: throw BadRequestException("User $it doesn't have mark")
            userMarkRepository.save(mark.copy(isAccessedDistribution = false))
        }
    }

    fun getDistributionMark(taskId: UUID, teamId: UUID): List<DistributionDto> {
        val team =
            teamRepository.findById(teamId).getOrElse { throw BadRequestException("Team $teamId does not exist") }
        taskRepository.findById(taskId).getOrElse { throw BadRequestException("Task $taskId does not exist") }
        return team.users.map {
            val mark = userMarkRepository.findMarkByUserIdAndTaskId(it.id, taskId)
            DistributionDto(userService.getUserById(it.id), mark?.mark ?: -1f)
        }
    }
}