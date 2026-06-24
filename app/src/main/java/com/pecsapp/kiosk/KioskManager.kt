package com.pecsapp.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Context

class KioskManager(private val activity: Activity) {

    private val activityManager =
        activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun startKioskMode() {
        activity.startLockTask()
    }

    fun stopKioskMode() {
        activity.stopLockTask()
    }

    fun isInKioskMode(): Boolean {
        return activityManager.lockTaskModeState !=
                ActivityManager.LOCK_TASK_MODE_NONE
    }
}