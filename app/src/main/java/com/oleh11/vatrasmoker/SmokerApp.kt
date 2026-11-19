package com.oleh11.vatrasmoker

import android.app.Application
import com.oleh11.vatrasmoker.data.repository.SmokerRepository
import com.oleh11.vatrasmoker.data.repository.SmokerRepositoryImpl

class SmokerApp : Application() {

    lateinit var repository: SmokerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = SmokerRepositoryImpl(this)
    }
}
