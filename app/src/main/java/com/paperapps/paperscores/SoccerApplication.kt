package com.paperapps.paperscores

import android.app.Application
import com.paperapps.paperscores.db.SoccerDatabase
import com.paperapps.paperscores.repository.SoccerRepository

class SoccerApplication : Application() {
    val database by lazy { SoccerDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        SoccerRepository.initialize(database.soccerDao())
    }
}
