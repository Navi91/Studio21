package com.studio21.android.api.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Util
import com.studio21.android.api.IcyDataSourceFactory
import com.studio21.android.api.PlayerCallback

/**
 * Created by Dmitriy on 24.02.2018.
 */
class RadioPlayback(val context: Context, val url: String) : Playback {

    private val mAudioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                if (isPlaying) {
//                    val i = Intent(context, MusicService::class.java)
//                    i.action = MusicService.ACTION_CMD
//                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE)
//                    mContext.startService(i)
                    pause()
                }
            }
        }
    }

    private val playerCallback: RadioPlayerCallback = object : RadioPlayerCallback() {
        override fun playerMetadata(key: String?, value: String?) {
            callback?.onMetadata(key, value)
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            val what: String?
            when (error?.type) {
                ExoPlaybackException.TYPE_SOURCE -> what = error.getSourceException().message
                ExoPlaybackException.TYPE_RENDERER -> what = error.getRendererException().message
                ExoPlaybackException.TYPE_UNEXPECTED -> what = error.getUnexpectedException().message
                else -> what = "Unknown: " + error?.message
            }

            callback?.onError("ExoPlayer error " + what)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_IDLE, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY -> callback?.onPlaybackStatusChanged(state)
                ExoPlayer.STATE_ENDED -> callback?.onCompletion()
            }
        }
    }

    private val uri = Uri.parse(url)
    private val extractorsFactory = DefaultExtractorsFactory()
    private val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
    private val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    private val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
    private val loadControl = DefaultLoadControl()
    private val dataSourceFactory: DataSource.Factory = IcyDataSourceFactory(context, Util.getUserAgent(context, "Studio21"), true, playerCallback)
    private val mediaSource = ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null)
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var exoPlayer: SimpleExoPlayer? = null
    private var playOnFocusGain: Boolean = false
    private var exoPlayerNullIsStopped = false
    private var audioNoisyReceiverRegistered = false
    private var currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
    private var callback: Playback.Callback? = null

    companion object {
        val VOLUME_DUCK = 0.2f
        // The volume we set the media player when we have audio focus.
        val VOLUME_NORMAL = 1.0f

        // we don't have audio focus, and can't duck (play at a low volume)
        private val AUDIO_NO_FOCUS_NO_DUCK = 0
        // we don't have focus, but can duck (play at a low volume)
        private val AUDIO_NO_FOCUS_CAN_DUCK = 1
        // we have full audio focus
        private val AUDIO_FOCUSED = 2
    }

    init {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
        exoPlayer?.addListener(playerCallback)
    }

    override fun start() {
    }

    override fun stop(notifyListeners: Boolean) {
    }

    override fun setState(state: Int) {
    }

    override fun getState(): Int {
        if (exoPlayer == null) {
            return if (exoPlayerNullIsStopped) PlaybackStateCompat.STATE_STOPPED else PlaybackStateCompat.STATE_NONE
        }

        when (exoPlayer?.playbackState) {
            ExoPlayer.STATE_IDLE -> return PlaybackStateCompat.STATE_PAUSED
            ExoPlayer.STATE_BUFFERING -> return PlaybackStateCompat.STATE_BUFFERING
            ExoPlayer.STATE_READY -> return if (exoPlayer?.playWhenReady == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            ExoPlayer.STATE_ENDED -> return PlaybackStateCompat.STATE_PAUSED
            else -> return PlaybackStateCompat.STATE_NONE
        }
    }

    override fun isConnected(): Boolean = true

    override fun isPlaying(): Boolean = playOnFocusGain || (exoPlayer?.playWhenReady ?: false)

    override fun getCurrentStreamPosition(): Long = exoPlayer?.currentPosition ?: 0

    override fun updateLastKnownStreamPosition() {
    }

    override fun play() {


        exoPlayer?.prepare(mediaSource)
        exoPlayer?.playWhenReady = true

        registerAudioNoisyReceiver()
    }

    override fun pause() {
        exoPlayer?.playWhenReady = false

        unregisterAudioNoisyReceiver()
    }

    override fun seekTo(position: Long) {
        if (exoPlayer != null) {
            registerAudioNoisyReceiver()
            exoPlayer?.seekTo(position)
        }
    }

    override fun setCurrentMediaId(mediaId: String?) {
    }

    override fun getCurrentMediaId(): String = ""

    override fun setCallback(callback: Playback.Callback?) {
        this.callback = callback
    }

    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> currentAudioFocusState = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                currentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost audio focus, but will gain it back (shortly), so note whether
                // playback should resume
                currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                playOnFocusGain = exoPlayer?.playWhenReady == true
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                // Lost audio focus, probably "permanently"
                currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }

        if (exoPlayer != null) {
            // Update the player state based on the change
            configurePlayerState()
        }
    }

    private fun tryToGetAudioFocus() {
        val result = audioManager.requestAudioFocus(
                mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AUDIO_FOCUSED
        } else {
            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    private fun configurePlayerState() {
        if (currentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause()
        } else {
            registerAudioNoisyReceiver()

            if (currentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                exoPlayer?.volume = VOLUME_DUCK
            } else {
                exoPlayer?.volume = VOLUME_NORMAL
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                exoPlayer?.playWhenReady = true
                playOnFocusGain = false
            }
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(mAudioNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }
}