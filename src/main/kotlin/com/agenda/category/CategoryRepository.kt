package com.agenda.category

import org.springframework.data.mongodb.repository.MongoRepository

interface CategoryRepository : MongoRepository<Category, String> {
    fun findByIsDefaultTrue(): List<Category>
    fun existsByNameAndIsDefaultTrue(name: String): Boolean
    fun findByNameAndIsDefaultTrue(name: String): Category?
    fun findByUserId(userId: String): List<Category>
}