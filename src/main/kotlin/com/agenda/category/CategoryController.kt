package com.agenda.category

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class CategoryResponse(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

@RestController
@RequestMapping("/categories")
class CategoryController(private val repository: CategoryRepository) {

    // Padrões do sistema + as do próprio usuário. A regra de "padrão" é a mesma do
    // ReminderService.ensureCategories (isDefault e sem dono), para a lista nunca
    // oferecer uma categoria que a criação de lembretes recusaria.
    @GetMapping
    fun list(authentication: Authentication): List<CategoryResponse> {
        val defaults = repository.findByIsDefaultTrue().filter { it.userId == null }
        val own = repository.findByUserId(authentication.name)
        return (defaults + own)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Category> { it.isDefault }.thenBy { it.name })
            .map { CategoryResponse(requireNotNull(it.id), it.name, it.isDefault) }
    }
}