package com.studio21.android.api.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.studio21.android.R
import com.studio21.android.view.activity.RadioActivity

/**
 * Created by Dmitriy on 25.02.2018.
 */
class RadioNotificationManager(val service: RadioService) : BroadcastReceiver() {

    private var sessionToken: MediaSessionCompat.Token? = null
    private var controller: MediaControllerCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    private var playbackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null

    private val notificationManager: NotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val pkg = service.packageName
    private val playIntent: PendingIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    private val pauseIntent: PendingIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
    private val stopIntent: PendingIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)

    private var started = false

    companion object {
        private val CHANNEL_ID = "radio_channel_id"

        private val NOTIFICATION_ID = 412
        private val REQUEST_CODE = 100

        const val ACTION_PAUSE = "action_pause"
        const val ACTION_PLAY = "action_play"
        const val ACTION_STOP = "action_stop"
    }

    init {
        notificationManager.cancelAll()
    }

    fun startNotification() {
        if (!started) {
            metadata = controller?.getMetadata()
            playbackState = controller?.getPlaybackState()

            // The notification must be updated after setting started to true
            val notification = createNotification()
            if (notification != null) {
                controller?.registerCallback(mediaControllerCallback)
                val filter = IntentFilter()
                filter.addAction(ACTION_PAUSE)
                filter.addAction(ACTION_PLAY)
                service.registerReceiver(this, filter)

                service.startForeground(NOTIFICATION_ID, notification)
                started = true
            }
        }
    }

    fun stopNotification() {
        if (started) {
            started = false
            controller?.unregisterCallback(mediaControllerCallback)
            try {
                notificationManager.cancel(NOTIFICATION_ID)
                service.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
                // ignore if the receiver is not registered.
            }

            service.stopForeground(true)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {

    }

    private fun createContentIntent(description: MediaDescriptionCompat?): PendingIntent {
        val openUI = Intent(service, RadioActivity::class.java)
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//        openUI.putExtra(MusicPlayerActivity.EXTRA_START_FULLSCREEN, true)

        if (description != null) {
//            openUI.putExtra(MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description)
        }

        return PendingIntent.getActivity(service, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            this@RadioNotificationManager.playbackState = state
            if (state.state == PlaybackStateCompat.STATE_STOPPED || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            this@RadioNotificationManager.metadata = metadata
            val notification = createNotification()
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val freshToken = service.getSessionToken()
        if (sessionToken == null && freshToken != null || sessionToken != null && sessionToken != freshToken) {
            controller?.unregisterCallback(mediaControllerCallback)

            sessionToken = freshToken
            if (sessionToken != null) {
                controller = MediaControllerCompat(service, sessionToken!!)
                transportControls = controller?.getTransportControls()

                if (started) {
                    controller?.registerCallback(mediaControllerCallback)
                }
            }
        }
    }

    private fun createNotification(): Notification? {
        if (metadata == null || playbackState == null) {
            return null
        }

        val description = metadata?.description

        if (description == null) return null

        var fetchArtUrl: String? = null
        var art: Bitmap? = null
        if (description.iconUri != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            val artUrl = description.iconUri.toString()
//            art = AlbumArtCache.getInstance().getBigImage(artUrl)
//            if (art == null) {
//                fetchArtUrl = artUrl
//                // use a placeholder art while the remote art is being downloaded
//                art = BitmapFactory.decodeResource(service.resources,
//                        R.drawable.ic_default_art)
//            }
        }

        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notificationBuilder = NotificationCompat.Builder(service, CHANNEL_ID)

        addActions(notificationBuilder)
        notificationBuilder
                .setStyle(MediaStyle()
                        // show only play/pause in compact view
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopIntent)
                        .setMediaSession(sessionToken))
                .setDeleteIntent(stopIntent)
                .setColor(ContextCompat.getColor(service, R.color.colorPrimary))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setLargeIcon(art)

//        if (mController != null && mController.getExtras() != null) {
//            val castName = mController.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST)
//            if (castName != null) {
//                val castInfo = mService.getResources()
//                        .getString(R.string.casting_to_device, castName)
//                notificationBuilder.setSubText(castInfo)
//                notificationBuilder.addAction(R.drawable.ic_close_black_24dp,
//                        mService.getString(R.string.stop_casting), mStopCastIntent)
//            }
//        }

        setNotificationPlaybackState(notificationBuilder)
        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder)
        }

        return notificationBuilder.build()
    }

    private fun addActions(notificationBuilder: NotificationCompat.Builder) {
        val label: String
        val icon: Int
        val intent: PendingIntent

        if (playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            label = service.getString(R.string.pause)
            icon = R.drawable.ic_pause_black_24dp
            intent = pauseIntent
        } else {
            label = service.getString(R.string.play)
            icon = R.drawable.ic_play_arrow_black_24dp
            intent = playIntent
        }

        notificationBuilder.addAction(NotificationCompat.Action(icon, label, intent))
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (playbackState == null || !started) {
            service.stopForeground(true)
            return
        }

        builder.setOngoing(playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
    }

    private fun fetchBitmapFromURLAsync(bitmapUrl: String,
                                        builder: NotificationCompat.Builder) {
        // TODO load img
//        AlbumArtCache.getInstance().fetch(bitmapUrl, object : AlbumArtCache.FetchListener() {
//            fun onFetched(artUrl: String, bitmap: Bitmap, icon: Bitmap) {
//                if (mMetadata != null && mMetadata.getDescription().getIconUri() != null &&
//                        mMetadata.getDescription().getIconUri()!!.toString() == artUrl) {
//                    // If the media is still the same, update the notification:
//                    builder.setLargeIcon(bitmap)
//                    addActions(builder)
//                    notificationManager.notify(NOTIFICATION_ID, builder.build())
//                }
//            }
//        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    service.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW)

            notificationChannel.description = service.getString(R.string.notification_channel_description)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}