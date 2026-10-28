package com.agenda.user.repository

import com.agenda.user.model.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun findByEmailNormalized(emailNormalized: String): User?
    fun findByEmailIgnoreCase(email: String): User?
    fun existsByEmailNormalized(emailNormalized: String): Boolean
    fun existsByEmailIgnoreCase(email: String): Boolean
}
