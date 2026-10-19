package com.agenda.reminder.service

import com.agenda.category.CategoryRepository
import com.agenda.reminder.DTO.UpdateReminderRequest
import com.agenda.reminder.model.Recurrence
import com.agenda.reminder.model.Reminder
import com.agenda.reminder.model.ReminderCompletion
import com.agenda.reminder.model.ReminderStatus
import com.agenda.reminder.repository.ReminderCompletionRepository
import com.agenda.reminder.repository.ReminderRepository
import org.springframework.stereotype.Service

@Service
class ReminderService(
    private val reminderRepository: ReminderRepository,
    private val categoryRepository: CategoryRepository,
    private val completionRepository: ReminderCompletionRepository
)  {

    fun create(reminder: Reminder): Reminder {
        val withCategories = ensureCategories(reminder)
        return reminderRepository.save(withCategories)
    }

    // RN1: todo lembrete precisa ter pelo menos uma categoria
    private fun ensureCategories(reminder: Reminder): Reminder {
        // Caso 1: lista vazia -> cai na categoria padrão "Geral"
        if (reminder.categoryIds.isEmpty()) {
            val geral = categoryRepository.findByNameAndIsDefaultTrue("Geral")
                ?: throw IllegalStateException("Categoria padrão 'Geral' não encontrada")
            return reminder.copy(categoryIds = setOf(geral.id!!))
        }

        // Caso 2: algum id não existe -> erro
        val found = categoryRepository.findAllById(reminder.categoryIds)
        if (found.size != reminder.categoryIds.size) {
            throw IllegalArgumentException("Uma ou mais categorias não existem")
        }

        // Caso 3: tudo certo, devolve como veio
        return reminder
    }
    fun list(status: ReminderStatus): List<Reminder> =
        reminderRepository.findByStatus(status)

    fun complete(id: String) {
        val reminder = reminderRepository.findById(id)
            .orElseThrow { NoSuchElementException("Lembrete não encontrado") }

        if (reminder.recurrence == Recurrence.ONCE) {
            reminderRepository.save(reminder.copy(status = ReminderStatus.COMPLETED))
        } else {
            completionRepository.save(ReminderCompletion(reminderId = id))
        }
    }
    fun update(id: String, request: UpdateReminderRequest): Reminder {
        val current = reminderRepository.findById(id)
            .orElseThrow { NoSuchElementException("Lembrete não encontrado") }

        return reminderRepository.save(ensureCategories(request.applyTo(current)))
    }
}