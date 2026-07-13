package eu.dotshell.telo.generic.data.repository.api

interface OfflineDataManager {
    suspend fun downloadAllOfflineData()
    fun cancelDownload()
}
