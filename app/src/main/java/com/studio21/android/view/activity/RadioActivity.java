package com.studio21.android.view.activity;

import android.content.ComponentName;
import android.media.AudioManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.studio21.android.R;
import com.studio21.android.api.radio.RadioService;
import com.studio21.android.view.fragment.RadioFragment;

public class RadioActivity extends AppCompatActivity {
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private MediaBrowserCompat mediaBrowser;
    private RadioFragment radioFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.a_radio);
        Log.d("trace", "Create");

        radioFragment = (RadioFragment) getSupportFragmentManager().findFragmentById(R.id.radio);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, RadioService.class), mConnectionCallback, null);
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);
//        mediaController.registerCallback(mediaControllerCallback);

        if (radioFragment != null) {
            radioFragment.onConnected();
        }

        onMediaControllerConnected();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(this);
        if (controllerCompat != null) {
            controllerCompat.unregisterCallback(mediaControllerCallback);
        }
        mediaBrowser.disconnect();
    }

    protected void onMediaControllerConnected() {
        // empty implementation, can be overridden by clients.
    }

    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                }
            };

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        // TODO show toast
                    }
                }
            };

}
