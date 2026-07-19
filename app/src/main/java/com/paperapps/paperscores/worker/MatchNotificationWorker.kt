package com.paperapps.paperscores.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paperapps.paperscores.service.ScoreOverlayService

class MatchNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val matchId = inputData.getString("matchId") ?: return Result.failure()

            val channelId = "match_reminders"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Match Reminders",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                val manager = appContext.getSystemService(android.app.NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val serviceIntent = Intent(appContext, ScoreOverlayService::class.java).apply {
                putExtra("matchId", matchId)
            }
            
            val pendingIntent = android.app.PendingIntent.getForegroundService(
                appContext,
                matchId.hashCode(),
                serviceIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(com.paperapps.paperscores.R.mipmap.ic_launcher)
                .setContentTitle("Match Starting Soon")
                .setContentText("Tap to open live score overlay")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = androidx.core.app.NotificationManagerCompat.from(appContext)
            if (androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(matchId.hashCode(), notification)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
