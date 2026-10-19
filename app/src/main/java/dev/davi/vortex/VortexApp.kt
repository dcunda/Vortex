package dev.davi.vortex

import android.app.Application
import dev.davi.vortex.data.DownloadRepository
import dev.davi.vortex.engine.Engine
import dev.davi.vortex.service.Notifications

class VortexApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        Engine.init(this)
        DownloadRepository.init(this)
    }
}
