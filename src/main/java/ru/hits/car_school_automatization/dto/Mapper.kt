package ru.hits.car_school_automatization.dto

import ru.hits.car_school_automatization.entity.Comment
import ru.hits.car_school_automatization.mapper.UserMapper
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Comment.toCommentDto(userMapper: UserMapper) = CommentDto(
    id = id,
    text = text,
    authorDto = userMapper.toDto(author),
    updateAt = editAt,
    createAt = createAt
)