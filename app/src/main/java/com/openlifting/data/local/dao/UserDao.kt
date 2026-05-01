package com.openlifting.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.openlifting.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE authToken IS NOT NULL LIMIT 1")
    fun observeLoggedInUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE authToken IS NOT NULL LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Query("UPDATE users SET authToken = NULL WHERE id = :id")
    suspend fun clearToken(id: Long)

    @Delete
    suspend fun delete(user: UserEntity)
}
