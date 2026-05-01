package com.openlifting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val email: String,
    val name: String,
    val role: String,           // "athlete" | "instructor"
    val authToken: String? = null,
    val serverId: Long? = null
)
