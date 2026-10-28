package com.agenda.auth

import com.agenda.user.dto.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class LoginResponse(
    val tokenType: String = "Bearer",
    val accessToken: String,
    val expiresAt: Instant,
    val user: UserResponse,
)
