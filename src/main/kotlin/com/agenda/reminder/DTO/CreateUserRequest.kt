package com.agenda.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank @field:Size(min = 2, max = 100) val name: String,
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String,
    @field:NotBlank @field:Size(min = 10, max = 72) val password: String,
)
