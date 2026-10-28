package com.agenda.auth

import com.agenda.user.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val sessions: SessionService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.login(request))

    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<UserResponse> =
        ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(authService.currentUser(authentication.name))

    @PostMapping("/logout")
    fun logout(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): ResponseEntity<Void> {
        sessions.revoke(authorization.removePrefix("Bearer "))
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build()
    }
}
