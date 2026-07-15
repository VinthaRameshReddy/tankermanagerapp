package com.tankermanager.app

import android.app.Application
import com.tankermanager.app.data.repo.SessionStore
import com.tankermanager.app.data.repo.TankerRepository

class TankerApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var repository: TankerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        repository = TankerRepository(sessionStore)
    }
}

val Application.tankerApp: TankerApp
    get() = this as TankerApp
