package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VRWindowDao {
    @Query("SELECT * FROM vr_windows")
    fun getAllWindows(): Flow<List<VRWindowConfig>>

    @Query("SELECT * FROM vr_windows WHERE id = :id")
    suspend fun getWindowById(id: String): VRWindowConfig?

    @Query("SELECT * FROM vr_windows WHERE isOpen = 1")
    fun getOpenWindows(): Flow<List<VRWindowConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: VRWindowConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(windows: List<VRWindowConfig>)

    @Update
    suspend fun update(window: VRWindowConfig)

    @Delete
    suspend fun delete(window: VRWindowConfig)

    @Query("DELETE FROM vr_windows WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vr_windows")
    suspend fun deleteAll()
}