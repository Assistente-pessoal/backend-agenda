package com.agenda.config

import com.agenda.auth.BearerTokenFilter
import com.agenda.auth.SessionService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val sessionService: SessionService,
    @Value("\${auth.cors-allowed-origins:}") private val allowedOrigins: String,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Stateless Authorization: Bearer, sem cookies: não usa CSRF de sessão.
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { rules ->
                rules.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                rules.requestMatchers(HttpMethod.POST, "/users", "/auth/login").permitAll()
                rules.requestMatchers(HttpMethod.GET, "/api/vox/health").permitAll()
                rules.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { _, response, _ ->
                    writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "Autenticação necessária.")
                }
                exceptions.accessDeniedHandler { _, response, _ ->
                    writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado.")
                }
            }
            .addFilterAfter(BearerTokenFilter(sessionService), LogoutFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = allowedOrigins.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        require(origins.none { it == "*" || it.contains('*') }) {
            "Configure AUTH_CORS_ALLOWED_ORIGINS com origens HTTPS explícitas, sem curingas."
        }
        val configuration = CorsConfiguration().apply {
            allowedOrigins = origins
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}

internal fun writeSecurityError(response: HttpServletResponse, status: Int, message: String) {
    response.status = status
    response.characterEncoding = "UTF-8"
    response.contentType = "application/json"
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store")
    // Só são usadas mensagens literais internas, não dados de entrada do cliente.
    response.writer.write("{\"message\":\"$message\"}")
}
