package com.pelotcl.app.generic.data.repository.api

interface OfflineDataManager {
    suspend fun downloadAllOfflineData()
    fun cancelDownload()
}
