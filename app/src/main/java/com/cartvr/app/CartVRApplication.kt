package com.cartvr.app

import android.app.Application
import com.cartvr.app.data.local.CartVrDatabase
import com.cartvr.app.data.preferences.AppPreferences
import com.cartvr.app.data.sync.CartVrRepository
import com.cartvr.app.data.sync.NetworkMonitor
import com.cartvr.app.data.updater.GitHubUpdater

class CartVRApplication : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var database: CartVrDatabase
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var repository: CartVrRepository
        private set

    lateinit var updater: GitHubUpdater
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        database = CartVrDatabase.getInstance(this)
        networkMonitor = NetworkMonitor(this)
        repository = CartVrRepository(
            dao = database.cartVrDao(),
            prefs = preferences,
            networkMonitor = networkMonitor
        )
        updater = GitHubUpdater(this)
    }
}
