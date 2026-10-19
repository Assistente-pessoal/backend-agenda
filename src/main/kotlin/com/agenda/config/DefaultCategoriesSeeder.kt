package com.agenda.config

import com.agenda.category.Category
import com.agenda.category.CategoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DefaultCategoriesSeeder(
    private val repository: CategoryRepository
) : CommandLineRunner {

    private val defaults = listOf("Saúde", "Documentos", "Compromissos", "Objetos perdidos", "Geral")

    override fun run(vararg args: String) {
        defaults
            .filterNot { repository.existsByNameAndIsDefaultTrue(it) }
            .forEach { repository.save(Category(name = it, isDefault = true)) }
    }
}