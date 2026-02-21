package ru.hits.car_school_automatization.controller

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.hits.car_school_automatization.service.FileStorageService
import java.nio.file.Path

@RequestMapping("/file")
@RestController
open class FileController(
    private val fileStorageService: FileStorageService
) {

    @GetMapping("/{name}")
    fun getFile(@PathVariable name: String): ResponseEntity<Resource> {
        val filePath: Path = fileStorageService.loadAsResource(name)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")

        val resource = FileSystemResource(filePath)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filePath.fileName}\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(filePath.toFile().length())
            .body(resource)
    }

    @DeleteMapping("/{name}")
    fun deleteFile(@PathVariable name: String) {
        return fileStorageService.delete(name)
    }
}