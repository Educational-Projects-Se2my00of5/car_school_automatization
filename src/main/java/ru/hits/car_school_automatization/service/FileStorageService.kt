package ru.hits.car_school_automatization.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

@Service
class FileStorageService(
    @param:Value("\${file.path}") private val path: String,
) {
    private val uploadPath: Path = Paths.get(path).toAbsolutePath().normalize()

    init {
        Files.createDirectories(uploadPath)
    }

    fun store(file: MultipartFile): String {
        if (file.isEmpty) throw IllegalArgumentException("File is empty")
        val originalFilename = file.originalFilename ?: "unknown"
        val extension = originalFilename.substringAfterLast('.', "").let {
            if (it.isBlank()) "" else ".$it"
        }
        val filename = "${UUID.randomUUID()}$extension"
        val targetLocation = uploadPath.resolve(filename)
        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetLocation)
        }
        return "http://localhost:8080/$filename"
    }

    fun loadAsResource(filename: String): Path? {
        val filePath = uploadPath.resolve(filename).normalize()
        return if (Files.exists(filePath) && Files.isReadable(filePath)) filePath else null
    }

    fun delete(filename: String) {
        Files.deleteIfExists(uploadPath.resolve(filename))
    }
}