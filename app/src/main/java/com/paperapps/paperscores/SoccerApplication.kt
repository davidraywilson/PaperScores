package com.paperapps.paperscores

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.paperapps.paperscores.db.SoccerDatabase
import com.paperapps.paperscores.repository.SoccerRepository

class SoccerApplication : Application() {
    val database by lazy { SoccerDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        SoccerRepository.initialize(this, database.soccerDao())
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "score_overlay_channel",
                "Live Score Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active score overlays"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
