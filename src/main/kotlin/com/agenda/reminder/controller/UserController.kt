package com.agenda.reminder.controller

import com.agenda.user.dto.CreateUserRequest
import com.agenda.user.dto.UserResponse
import com.agenda.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateUserRequest): UserResponse = userService.create(request)
}
