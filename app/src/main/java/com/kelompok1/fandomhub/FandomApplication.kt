package com.kelompok1.fandomhub

import android.app.Application
import com.kelompok1.fandomhub.data.FandomRepository
import com.kelompok1.fandomhub.data.local.FandomDatabase

class FandomApplication : Application() {
    // Manual DI
    val database by lazy { FandomDatabase.getDatabase(this) }
    val repository by lazy { FandomRepository(database) }
}
