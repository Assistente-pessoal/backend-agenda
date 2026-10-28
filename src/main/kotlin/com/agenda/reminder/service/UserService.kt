package com.agenda.user.service

import com.agenda.user.dto.CreateUserRequest
import com.agenda.user.dto.UserResponse
import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.util.Locale

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun create(request: CreateUserRequest): UserResponse {
        val email = request.email.trim().lowercase(Locale.ROOT)
        if (request.password.toByteArray(StandardCharsets.UTF_8).size > 72) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha muito longa (máximo de 72 bytes em UTF-8).")
        }
        if (userRepository.existsByEmailNormalized(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.")
        }
        val user = User(
            name = request.name.trim(),
            email = email,
            emailNormalized = email,
            password = requireNotNull(passwordEncoder.encode(request.password)) {
            "Falha ao gerar o hash da senha."
        },
        )
        val saved = try {
            userRepository.save(user)
        } catch (_: DuplicateKeyException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.")
        }
        return UserResponse(id = requireNotNull(saved.id), name = saved.name, email = saved.email)
    }
}
