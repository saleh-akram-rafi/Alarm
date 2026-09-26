package com.discipline.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.discipline.alarm.domain.usecase.alarm.RescheduleAlarmsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }

    @Inject
    lateinit var rescheduleAlarmsUseCase: RescheduleAlarmsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action")

        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Rescheduling all enabled alarms after time or timezone shift...")
                    rescheduleAlarmsUseCase()
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after time change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
