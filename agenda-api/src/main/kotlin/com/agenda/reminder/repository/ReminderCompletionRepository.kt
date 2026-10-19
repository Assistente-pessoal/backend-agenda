package com.agenda.reminder.repository

import com.agenda.reminder.model.ReminderCompletion
import org.springframework.data.mongodb.repository.MongoRepository

interface ReminderCompletionRepository : MongoRepository<ReminderCompletion, String>