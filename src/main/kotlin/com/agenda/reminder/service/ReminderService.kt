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
    private val completionRepository: ReminderCompletionRepository,
) {
    fun create(reminder: Reminder, ownerId: String): Reminder =
        reminderRepository.save(ensureCategories(reminder.copy(ownerId = ownerId), ownerId))

    private fun ensureCategories(reminder: Reminder, ownerId: String): Reminder {
        if (reminder.categoryIds.isEmpty()) {
            val general = categoryRepository.findByNameAndIsDefaultTrue("Geral")
                ?: throw IllegalStateException("Categoria padrão 'Geral' não encontrada")
            return reminder.copy(categoryIds = setOf(requireNotNull(general.id)))
        }
        val found = categoryRepository.findAllById(reminder.categoryIds).toList()
        if (found.size != reminder.categoryIds.size ||
            found.any { !(it.isDefault && it.userId == null) && it.userId != ownerId }
        ) {
            throw IllegalArgumentException("Uma ou mais categorias não existem ou não pertencem ao usuário.")
        }
        return reminder
    }

    fun list(status: ReminderStatus, ownerId: String): List<Reminder> =
        reminderRepository.findByOwnerIdAndStatus(ownerId, status)

    fun complete(id: String, ownerId: String) {
        val reminder = reminderRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NoSuchElementException("Lembrete não encontrado")
        if (reminder.recurrence == Recurrence.ONCE) {
            reminderRepository.save(reminder.copy(status = ReminderStatus.COMPLETED))
        } else {
            completionRepository.save(ReminderCompletion(reminderId = id, ownerId = ownerId))
        }
    }

    fun update(id: String, request: UpdateReminderRequest, ownerId: String): Reminder {
        val current = reminderRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NoSuchElementException("Lembrete não encontrado")
        return reminderRepository.save(ensureCategories(request.applyTo(current), ownerId))
    }
}
