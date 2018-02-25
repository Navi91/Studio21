package com.studio21.android.view.fragment

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import com.studio21.android.R
import com.studio21.android.api.ArtLoadRequest
import com.studio21.android.api.ArtLoader
import com.studio21.android.api.playback.PlaybackManager
import com.studio21.android.util.Preferences
import com.studio21.android.view.Studio21Fragment

class RadioFragment : Studio21Fragment() {

    @BindView(R.id.volume)
    lateinit var volumeSeekBar: SeekBar
    @BindView(R.id.action)
    lateinit var actionImageView: ImageView
    @BindView(R.id.title)
    lateinit var titleTextView: TextView
    @BindView(R.id.subtitle)
    lateinit var subtitleTextView: TextView
    @BindView(R.id.art)
    lateinit var artImageView: ImageView

    private var artUrl: String? = null
    private lateinit var artLoader: ArtLoader
    private lateinit var audioManager: AudioManager
    private var loadImageRequest: ArtLoadRequest? = null

    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            this@RadioFragment.onPlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            this@RadioFragment.onMetadataChanged(metadata)
        }
    }

    companion object {
        fun newInstance(): RadioFragment {
            val fragment = RadioFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        artLoader = ArtLoader(activity!!)
        audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_radio, container, false)

        butter(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invalidateVolumeSeekBar()

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Preferences.setVolume(activity!!, convertProgressToFloat(progress))
                setVolumeMedia()
            }
        })
    }

    private fun invalidateVolumeSeekBar() {
        volumeSeekBar.progress = convertProgressFromFloat(Preferences.getVolume(activity!!))
    }

    override fun onStart() {
        super.onStart()

        val controller = MediaControllerCompat.getMediaController(activity!!)
        if (controller != null) {
            onConnected()
        }
    }

    override fun onStop() {
        super.onStop()

        val controller = MediaControllerCompat.getMediaController(activity!!)
        controller?.unregisterCallback(callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unsubscribeLoadRequest()
    }

    @OnClick(R.id.action)
    fun onActionClicked(view: View) {
        val controller = MediaControllerCompat.getMediaController(activity!!)
        val stateObj = controller.playbackState
        val state = stateObj?.state ?: PlaybackStateCompat.STATE_NONE

        if (state == PlaybackStateCompat.STATE_PAUSED ||
                state == PlaybackStateCompat.STATE_STOPPED ||
                state == PlaybackStateCompat.STATE_NONE) {
            playMedia()
        } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_BUFFERING ||
                state == PlaybackStateCompat.STATE_CONNECTING) {
            pauseMedia()
        }
    }

    fun onConnected() {
        val controller = MediaControllerCompat.getMediaController(activity!!)

        if (controller != null) {
            onMetadataChanged(controller.metadata)
            onPlaybackStateChanged(controller.playbackState)
            controller.registerCallback(callback)
        }
    }

    private fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        if (activity == null || state == null) {
            return
        }

        var enablePlay = false
        when (state.state) {
            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_NONE -> enablePlay = true
            PlaybackStateCompat.STATE_ERROR -> {
                Toast.makeText(activity, R.string.error, Toast.LENGTH_LONG).show()
                pauseMedia()
            }
        }

        if (enablePlay) {
            actionImageView.setImageDrawable(ContextCompat.getDrawable(activity!!, R.drawable.ic_play_accent_big))
        } else {
            actionImageView.setImageDrawable(ContextCompat.getDrawable(activity!!, R.drawable.ic_pause_accent_big))
        }
    }

    private fun convertProgressFromFloat(progress: Float): Int = (progress * 100).toInt()

    private fun convertProgressToFloat(progress: Int): Float = progress.toFloat() / 100

    private fun subscribeLoadRequest() {
        loadImageRequest?.subscribe()
    }

    private fun unsubscribeLoadRequest() {
        loadImageRequest?.unsubscribe()
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (activity == null || metadata == null) {
            return
        }

        val author = metadata.description.title
        val song = metadata.description.subtitle

        if (TextUtils.isEmpty(author) || TextUtils.isEmpty(song)) return
        unsubscribeLoadRequest()

        titleTextView.text = author
        subtitleTextView.text = song


        loadArtImage(author.toString(), song.toString())
    }

    private fun loadArtImage(author: String, song: String) {
        loadImageRequest = artLoader.request(author, song, object : ArtLoadRequest.LoadCallback {
            override fun onLoad(bitmap: Bitmap?) {
                if (bitmap != null) {
                    activity?.runOnUiThread({
                        artImageView.setImageBitmap(bitmap)
                    })
                }
            }
        })

        artImageView.setImageResource(R.mipmap.ic_notification_placeholder)
        subscribeLoadRequest()
    }

    private fun playMedia() {
        val controller = MediaControllerCompat.getMediaController(activity!!)
        controller?.transportControls?.play()
    }

    private fun pauseMedia() {
        val controller = MediaControllerCompat.getMediaController(activity!!)
        controller?.transportControls?.pause()
    }

    private fun setVolumeMedia() {
        val controller = MediaControllerCompat.getMediaController(activity!!)
        controller?.transportControls?.sendCustomAction(PlaybackManager.VOLUME_CUSTOM_ACTION, PlaybackManager.createVolumeArgs(Preferences.getVolume(activity!!)))
    }
}
