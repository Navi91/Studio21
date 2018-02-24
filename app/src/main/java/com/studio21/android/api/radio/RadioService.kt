package com.studio21.android.api.radio

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.studio21.android.api.playback.PlaybackManager
import com.studio21.android.api.playback.RadioPlayback
import com.studio21.android.view.activity.RadioActivity

/**
 * Created by Dmitriy on 24.02.2018.
 */
class RadioService : MediaBrowserServiceCompat(), PlaybackManager.PlaybackServiceCallback {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var session: MediaSessionCompat
    private lateinit var radioNotificationManager: RadioNotificationManager

    companion object {
        const val MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__"
        const val MEDIA_ID_ROOT = "__ROOT__"
    }

    override fun onCreate() {
        super.onCreate()

        val playback = RadioPlayback(applicationContext, "http://icecast-studio21.cdnvideo.ru/S21_1")
        playbackManager = PlaybackManager(playback, this)

        session = MediaSessionCompat(applicationContext, "RadioService")
        sessionToken = session.sessionToken
        session.setCallback(playbackManager.mediaSessionCallback)
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        val intent = Intent(this, RadioActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        session.setSessionActivity(pendingIntent)

        playbackManager.updatePlaybackState(null)

        radioNotificationManager = RadioNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPlaybackStart() {
    }

    override fun onNotificationRequired() {
    }

    override fun onPlaybackStop() {
    }

    override fun onPlaybackStateUpdated(state: PlaybackStateCompat) {
    }

    override fun onPlaybackMetadataUpdated(metadata: MediaMetadataCompat) {
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? = BrowserRoot(MEDIA_ID_ROOT, null)
}