package com.agenda.user.service

import com.agenda.user.dto.CreateUserRequest
import com.agenda.user.dto.UserResponse
import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun create(request: CreateUserRequest): UserResponse {

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("E-mail já cadastrado")
        }

        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password)!!
        )

        val savedUser = userRepository.save(user)

        return UserResponse(
            id = savedUser.id!!,
            name = savedUser.name,
            email = savedUser.email
        )
    }
}