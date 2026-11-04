package com.agenda.auth

import com.agenda.config.writeSecurityError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.dao.TransientDataAccessException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/** Instanciado diretamente no SecurityFilterChain; não é um filtro Servlet registrado duas vezes. */
class BearerTokenFilter(private val sessions: SessionService) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return request.method == "OPTIONS" ||
            (request.method == "POST" && (path == "/users" || path == "/auth/login")) ||
            (request.method == "GET" && path == "/api/vox/health")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorizations = request.getHeaders(HttpHeaders.AUTHORIZATION).toList()
        if (authorizations.isEmpty()) {
            filterChain.doFilter(request, response) // SecurityFilterChain devolve 401
            return
        }
        val token = authorizations.singleOrNull()?.let(TokenCodec::fromAuthorizationHeader)
        val userId = try {
            token?.let(sessions::resolveUserId)
        } catch (ex: DataAccessException) {
            if (ex !is DataAccessResourceFailureException &&
                ex !is TransientDataAccessException && ex !is RecoverableDataAccessException
            ) throw ex
            SecurityContextHolder.clearContext()
            writeSecurityError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Não foi possível validar a sessão. Tente novamente mais tarde.")
            return
        }
        if (userId == null) {
            SecurityContextHolder.clearContext()
            writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado.")
            return
        }

        val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
