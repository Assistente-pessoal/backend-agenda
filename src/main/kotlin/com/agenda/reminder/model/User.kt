package com.agenda.user.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("users")
data class User(
    @Id val id: String? = null,
    val name: String,
    val email: String,
    val password: String,
    // Índice esparso evita bloquear a inicialização com usuários antigos sem este campo.
    @Indexed(unique = true, sparse = true) val emailNormalized: String? = null,
)
