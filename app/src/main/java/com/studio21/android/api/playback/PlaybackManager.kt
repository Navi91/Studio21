package com.studio21.android.api.playback

import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.studio21.android.util.Logger

/**
 * Created by Dmitriy on 24.02.2018.
 */
class PlaybackManager(val playback: Playback, private val serviceCallback: PlaybackServiceCallback) : Playback.Callback {

    val TAG = "playback_manager"

    init {
        playback.setCallback(this)
    }

    val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            handlePlayRequest()
        }

        override fun onSeekTo(pos: Long) {
            playback.seekTo(pos)
        }

        override fun onPause() {
            handlePauseRequest()
        }

        override fun onStop() {
            handleStopRequest(null)
        }
    }

    fun handlePlayRequest() {
        serviceCallback.onPlaybackStart()
        playback.play()
    }

    fun handlePauseRequest() {
        if (playback.isPlaying) {
            playback.pause()
            serviceCallback.onPlaybackStop()
        }
    }

    fun handleStopRequest(withError: String?) {
        playback.stop(true)
        serviceCallback.onPlaybackStop()
        updatePlaybackState(withError)
    }

    fun updatePlaybackMetadata(key: String?, value: String?) {
        //Bundle[{StreamTitle=Баста - Папа what's up}]
        val split = value?.split(" - ") ?: return

        try {
            val author = split[0]
            val description = split[1]
            val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, author)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, description)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, value)
                    .build()
            serviceCallback.onPlaybackMetadataUpdated(metadata)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    fun updatePlaybackState(error: String?) {
        Logger.log(TAG, "updatePlaybackState $error")

        var position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        if (playback.isConnected) {
            position = playback.currentStreamPosition
        }

        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions())

        var state = playback.state

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error)
            state = PlaybackStateCompat.STATE_ERROR
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build())

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired()
        }
    }

    private fun getAvailableActions(): Long {
        var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE

        if (playback.isPlaying()) {
            actions = actions or PlaybackStateCompat.ACTION_PAUSE
        } else {
            actions = actions or PlaybackStateCompat.ACTION_PLAY
        }

        return actions
    }

    override fun onCompletion() {
        handleStopRequest(null)
    }

    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String?) {
        updatePlaybackState(error)
    }

    override fun onMetadata(key: String?, value: String?) {
        updatePlaybackMetadata(key, value)
    }

    interface PlaybackServiceCallback {
        fun onPlaybackStart()

        fun onNotificationRequired()

        fun onPlaybackStop()

        fun onPlaybackStateUpdated(state: PlaybackStateCompat)

        fun onPlaybackMetadataUpdated(metadata: MediaMetadataCompat)
    }
}