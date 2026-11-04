package com.agenda

import com.agenda.auth.AccessSessionRepository
import com.agenda.auth.TokenCodec
import com.agenda.reminder.model.Reminder
import com.agenda.reminder.repository.ReminderRepository
import com.agenda.user.repository.UserRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthHttpIntegrationTests @Autowired constructor(
    private val mvc: MockMvc,
    private val users: UserRepository,
    private val sessions: AccessSessionRepository,
    private val reminders: ReminderRepository,
) {
    private val json = JsonMapper.builder().build()

    @Test
    fun `preflight permite as origens locais e os headers usados pelo frontend`() {
        for (origin in listOf("http://localhost:8081", "http://localhost:8082", "http://127.0.0.1:8082")) {
            for (path in listOf("/users", "/auth/login")) {
                mvc.perform(options(path).header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
            }
            mvc.perform(options("/auth/me").header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
        }
    }

    @Test
    fun `origem nao autorizada e recusada mesmo no perfil local`() {
        mvc.perform(options("/auth/login").header(HttpHeaders.ORIGIN, "https://untrusted.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    @Test
    fun `sem token ou token malformado responde 401 JSON legivel pelo navegador`() {
        mvc.perform(get("/auth/me").header(HttpHeaders.ORIGIN, ORIGIN))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
            .andExpect(jsonPath("$.message").isNotEmpty())
        mvc.perform(get("/reminders").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun `cadastro normaliza email protege senha e rejeita duplicidade e nome vazio apos trim`() {
        val email = "${UUID.randomUUID()}@example.test"
        val response = mvc.perform(post("/users").header(HttpHeaders.ORIGIN, ORIGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(mapOf("name" to "  Pessoa Teste  ", "email" to email.uppercase(), "password" to PASSWORD))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pessoa Teste"))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
            .andReturn().response
        val id = json.readTree(response.contentAsString).path("id").asText()
        val stored = users.findById(id).orElseThrow()
        assertFalse(stored.password == PASSWORD)
        assertTrue(stored.password.startsWith("$2"))
        mvc.perform(post("/users").header(HttpHeaders.ORIGIN, ORIGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(mapOf("name" to "Outra Pessoa", "email" to email, "password" to PASSWORD))))
            .andExpect(status().isConflict())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
            .andExpect(jsonPath("$.message").isNotEmpty())
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
            .content(body(mapOf("name" to " a ", "email" to "${UUID.randomUUID()}@example.test", "password" to PASSWORD))))
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `login retorna contrato do frontend restaura sessao e logout revoga o token`() {
        val email = register()
        val login = login(email)
        val token = login.path("accessToken").asText()
        val id = login.path("user").path("id").asText()
        assertEquals("Bearer", login.path("tokenType").asText())
        assertTrue(TokenCodec.isValidFormat(token))
        assertTrue(Instant.parse(login.path("expiresAt").asText()).isAfter(Instant.now()))
        assertFalse(login.path("user").has("password"))
        assertNotNull(sessions.findByTokenHash(TokenCodec.sha256(token)))
        mvc.perform(get("/auth/me").header(HttpHeaders.ORIGIN, ORIGIN).header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "bearer $token"))
            .andExpect(status().isNoContent())
        assertNull(sessions.findByTokenHash(TokenCodec.sha256(token)))
        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnauthorized())
    }

    @Test
    fun `senha incorreta e usuario inexistente respondem 401 com CORS e sem emitir sessao`() {
        val email = register()
        for (candidate in listOf(email, "${UUID.randomUUID()}@example.test")) {
            mvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(mapOf("email" to candidate, "password" to "senha-incorreta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
        }
    }

    @Test
    fun `lembretes sao isolados por conta inclusive registros legados e ownerId enviado pelo cliente`() {
        val alice = login(register())
        val bob = login(register())
        val aliceToken = alice.path("accessToken").asText()
        val bobToken = bob.path("accessToken").asText()
        val aliceId = alice.path("user").path("id").asText()
        val bobId = bob.path("user").path("id").asText()
        val legacy = reminders.save(Reminder(title = "Registro anterior sem dono"))
        val created = mvc.perform(post("/reminders").header(HttpHeaders.AUTHORIZATION, "Bearer $aliceToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(mapOf("title" to "Lembrete de Alice", "ownerId" to bobId))))
            .andExpect(status().isCreated()).andReturn().response
        val reminderId = json.readTree(created.contentAsString).path("id").asText()
        assertEquals(aliceId, reminders.findById(reminderId).orElseThrow().ownerId)
        mvc.perform(get("/reminders").header(HttpHeaders.AUTHORIZATION, "Bearer $aliceToken"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(reminderId))
        mvc.perform(get("/reminders").header(HttpHeaders.AUTHORIZATION, "Bearer $bobToken"))
            .andExpect(status().isOk()).andExpect(content().json("[]"))
        for (id in listOf(reminderId, requireNotNull(legacy.id))) {
            mvc.perform(patch("/reminders/$id").header(HttpHeaders.AUTHORIZATION, "Bearer $bobToken")
                .contentType(MediaType.APPLICATION_JSON).content(body(mapOf("title" to "Tentativa"))))
                .andExpect(status().isNotFound())
            mvc.perform(patch("/reminders/$id/complete").header(HttpHeaders.AUTHORIZATION, "Bearer $bobToken"))
                .andExpect(status().isNotFound())
        }
        mvc.perform(patch("/reminders/$reminderId").header(HttpHeaders.ORIGIN, ORIGIN)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $aliceToken")
            .contentType(MediaType.APPLICATION_JSON).content(body(mapOf("title" to "Título atualizado"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Título atualizado"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
    }

    private fun register(): String {
        val email = "${UUID.randomUUID()}@example.test"
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
            .content(body(mapOf("name" to "Pessoa Teste", "email" to email, "password" to PASSWORD))))
            .andExpect(status().isCreated())
        return email
    }

    private fun login(email: String): JsonNode {
        val response = mvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, ORIGIN)
            .contentType(MediaType.APPLICATION_JSON).content(body(mapOf("email" to email, "password" to PASSWORD))))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andReturn().response
        return json.readTree(response.contentAsString)
    }

    private fun body(value: Map<String, String>): String = json.writeValueAsString(value)

    companion object {
        private const val ORIGIN = "http://localhost:8082"
        private const val PASSWORD = "senha-exclusiva-dos-testes"
        private val databaseName = "agenda_http_test_${UUID.randomUUID().toString().replace("-", "") }"

        @JvmStatic
        @DynamicPropertySource
        fun mongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.mongodb.uri") { "mongodb://localhost:27017/$databaseName" }
        }

        @JvmStatic
        @AfterAll
        fun cleanDatabase(@Autowired template: MongoTemplate) {
            check(template.db.name == databaseName)
            template.db.drop()
        }
    }
}
