package com.openlifting.domain.model

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
    val authToken: String? = null,
    val serverId: Long? = null
)
