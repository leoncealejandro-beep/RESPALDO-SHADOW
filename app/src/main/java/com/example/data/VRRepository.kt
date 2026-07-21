package com.example.data

import kotlinx.coroutines.flow.Flow

class VRRepository(
    private val vrWindowDao: VRWindowDao
) {

    val windowConfigs: Flow<List<VRWindowConfig>> =
        vrWindowDao.getAllWindows()

    suspend fun saveWindowConfig(config: VRWindowConfig) {
        vrWindowDao.insert(config)
    }

    suspend fun saveWindowConfigs(configs: List<VRWindowConfig>) {
        vrWindowDao.insertAll(configs)
    }

    suspend fun deleteWindowConfig(id: String) {
        vrWindowDao.deleteById(id)
    }
}