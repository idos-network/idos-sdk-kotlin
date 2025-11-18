package org.idos.app

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.idos.app.data.ConnectedUser
import org.idos.app.data.repository.UserRepository
import org.idos.app.di.appModule
import org.idos.enclave.EnclaveOrchestrator
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger(if (isDebuggable) Level.ERROR else Level.NONE)
            androidContext(this@App)
            modules(appModule)
        }

        // Initialize repositories on background thread using ProcessLifecycleOwner
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val userRepository = get<UserRepository>()
            userRepository.initialize()
        }
    }
}
