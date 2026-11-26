package com.oleh11.vatrasmoker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.oleh11.vatrasmoker.MainActivity
import com.oleh11.vatrasmoker.R
import com.oleh11.vatrasmoker.SmokerApp
import com.oleh11.vatrasmoker.data.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the Bluetooth link alive and shows grill temperature,
 * set temperature and smoke level while the grill is on. Stops itself otherwise.
 */
class GrillStatusService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observer: Job? = null
    private val notifications by lazy { getSystemService(NotificationManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repo = (application as SmokerApp).repository
        val current = GrillNotificationContent.from(repo.connectionState.value, repo.smokerState.value)

        // Android requires startForeground() after every startForegroundService(), even if we stop right away.
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                build(current ?: GrillNotificationContent(getString(R.string.app_name), "")),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter the foreground: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        notifications.cancel(LOST_NOTIFICATION_ID)
        observer?.cancel()
        observer = scope.launch {
            combine(repo.connectionState, repo.smokerState, repo.liveNotification) { connection, state, enabled ->
                connection to if (enabled) GrillNotificationContent.from(connection, state) else null
            }.distinctUntilChanged().collect { (connection, content) ->
                if (content != null) {
                    notifications.notify(NOTIFICATION_ID, build(content))
                } else {
                    // Say so if the link failed for good, instead of just vanishing mid-cook
                    if (connection is ConnectionState.Error && repo.liveNotification.value) {
                        notifications.notify(
                            LOST_NOTIFICATION_ID,
                            build(GrillNotificationContent("Lost connection to the grill", "Open the app to reconnect."), ongoing = false)
                        )
                    }
                    ServiceCompat.stopForeground(this@GrillStatusService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        // Not sticky: if Android kills the process the Bluetooth link is gone too.
        return START_NOT_STICKY
    }

    private fun build(content: GrillNotificationContent, ongoing: Boolean = true): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_grill)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setContentIntent(openApp)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "GrillStatusService"
        private const val CHANNEL_ID = "grill_status"
        private const val NOTIFICATION_ID = 1001
        private const val LOST_NOTIFICATION_ID = 1002

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, GrillStatusService::class.java))
            } catch (e: Exception) {
                // Android refuses background starts; MainActivity.onResume tries again.
                Log.w(TAG, "Could not start: ${e.message}")
            }
        }
    }
}
