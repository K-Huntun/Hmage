package top.heiha.huntun.hmage

import android.app.Application

object HmageContenxt {
    internal lateinit var application: Application

    fun init(application: Application) {
        this.application = application
    }
}