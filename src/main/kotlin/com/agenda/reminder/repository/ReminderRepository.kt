package com.agenda.reminder.repository

import com.agenda.reminder.model.Reminder
import com.agenda.reminder.model.ReminderStatus
import org.springframework.data.mongodb.repository.MongoRepository

interface ReminderRepository : MongoRepository<Reminder, String> {
    fun findByOwnerIdAndStatus(ownerId: String, status: ReminderStatus): List<Reminder>
    fun findByIdAndOwnerId(id: String, ownerId: String): Reminder?
}
