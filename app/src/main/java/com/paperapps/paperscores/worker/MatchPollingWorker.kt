package com.paperapps.paperscores.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paperapps.paperscores.repository.SoccerRepository

class MatchPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // In a real application, you might inject the repository via Hilt or Koin.
            // For now, we'll assume the application or a singleton provides it, or we create it.
            // SoccerRepository.getInstance().refreshActiveGames()
            
            // Simulating successful poll
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
