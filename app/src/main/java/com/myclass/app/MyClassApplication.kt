package com.myclass.app

import android.app.Application
import com.myclass.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyClassApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed the default teacher profile row so settings can be edited immediately.
        applicationScope.launch {
            database.teacherProfileDao().upsert(
                com.myclass.app.data.local.TeacherProfile(id = 1)
            )
        }
    }
}
