package com.example.purrytify.utils

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.example.purrytify.MainActivity
import com.example.purrytify.R

class MusicPlayerService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "music_channel"
    private lateinit var musicPlayerManager: MusicPlayerManager

    override fun onCreate() {
        super.onCreate()
        musicPlayerManager = MusicPlayerManager.getInstance()

        mediaSession = MediaSessionCompat(this, "MusicService")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "ACTION_PLAY" -> playMusic()
            "ACTION_PAUSE" -> pauseMusic()
            "ACTION_STOP" -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playMusic() {
        // inisialisasi dan mulai pemutaran
        musicPlayerManager.play()
        showNotification(isPlaying = true)
    }

    private fun pauseMusic() {
        // pause media
        musicPlayerManager.pause()
        showNotification(isPlaying = false)
    }

    private fun showNotification(isPlaying: Boolean) {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pause, "Pause",
                getPendingIntent("ACTION_PAUSE")
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play, "Play",
                getPendingIntent("ACTION_PLAY")
            )
        }

        val currentSong = musicPlayerManager.currentSongInfo.value

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(currentSong?.title ?: "Unknown Song")
            .setContentText(currentSong?.artist ?: "Unknown Artist")
            .setContentIntent(getContentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .setDeleteIntent(getPendingIntent("ACTION_STOP"))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1) // Play/Pause di compact view
            )
            .build()

        startForeground(1, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
