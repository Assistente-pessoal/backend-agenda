package com.agenda.reminder.repository

import com.agenda.reminder.model.Reminder
import com.agenda.reminder.model.ReminderStatus
import org.springframework.data.mongodb.repository.MongoRepository

interface ReminderRepository : MongoRepository<Reminder, String> {
    fun findByStatus(status: ReminderStatus): List<Reminder>
}