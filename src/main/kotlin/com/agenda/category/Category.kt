package com.agenda.category

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
@Document("categories")
data class Category(
    @Id val id: String? = null,
    val name: String,
    val isDefault: Boolean = false,
    // nulo = categoria padrão do sistema; preenchido = categoria criada por esse usuário
    val userId: String? = null
)