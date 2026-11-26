package com.oleh11.vatrasmoker

import android.app.Application
import com.oleh11.vatrasmoker.service.GrillNotificationContent
import com.oleh11.vatrasmoker.service.GrillStatusService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.oleh11.vatrasmoker.data.repository.SmokerRepository
import com.oleh11.vatrasmoker.data.repository.SmokerRepositoryImpl

class SmokerApp : Application() {

    lateinit var repository: SmokerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SmokerRepositoryImpl(this)

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            combine(repository.connectionState, repository.smokerState, repository.liveNotification) { _, _, _ ->
                wantsStatusNotification()
            }.distinctUntilChanged().collect { wanted ->
                if (wanted) GrillStatusService.start(this@SmokerApp)
            }
        }
    }

    private fun wantsStatusNotification(): Boolean =
        repository.liveNotification.value &&
            GrillNotificationContent.from(repository.connectionState.value, repository.smokerState.value) != null

    /** Retries a start that Android refused while the app was in the background. */
    fun syncStatusNotification() {
        if (wantsStatusNotification()) GrillStatusService.start(this)
    }
}
