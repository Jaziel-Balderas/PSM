package com.example.psm

import Model.repository.ConnectivityObserver
import Model.worker.SyncWorker
import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class PSMApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar observador de conectividad
        ConnectivityObserver.init(this)
        
        // Configurar WorkManager para sincronización periódica
        setupPeriodicSync()
        
        // Configurar sincronización cuando hay conectividad
        setupConnectivitySync()
    }
    
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    private fun setupConnectivitySync() {
        // Sync inmediato cuando hay conectividad
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val oneTimeSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniqueWork(
            "${SyncWorker.WORK_NAME}_IMMEDIATE",
            ExistingWorkPolicy.REPLACE,
            oneTimeSyncRequest
        )
    }
}
