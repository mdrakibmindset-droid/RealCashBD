package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { UserRepository(database.appDao) }

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Pre-populate data asynchronously on start
        applicationScope.launch {
            repository.prepopulateDb()
        }
    }
}
