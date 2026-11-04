package com.agenda.config

import com.agenda.vox.VoxApiError
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.dao.TransientDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DataAccessResourceFailureException::class, TransientDataAccessException::class, RecoverableDataAccessException::class)
    fun handleDataAccess(@Suppress("UNUSED_PARAMETER") ex: DataAccessException): ResponseEntity<VoxApiError> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(VoxApiError("O serviço de dados está indisponível. Tente novamente mais tarde."))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<VoxApiError> =
        ResponseEntity.status(ex.statusCode).body(VoxApiError(ex.reason ?: "A requisição não pôde ser concluída."))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<VoxApiError> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "Os dados enviados são inválidos."
        return ResponseEntity.badRequest().body(VoxApiError(message))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<VoxApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(VoxApiError(ex.message ?: "Os dados enviados são inválidos."))

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (ex.message ?: "Não encontrado")))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: "Requisição inválida")))
}
