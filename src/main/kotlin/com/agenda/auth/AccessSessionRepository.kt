package com.agenda.auth

import org.springframework.data.mongodb.repository.MongoRepository

interface AccessSessionRepository : MongoRepository<AccessSession, String> {
    fun findByTokenHash(tokenHash: String): AccessSession?
}
