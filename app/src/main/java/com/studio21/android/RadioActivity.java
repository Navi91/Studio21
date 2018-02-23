package com.studio21.android;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.TimedMetaData;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class RadioActivity extends AppCompatActivity {
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private SimpleExoPlayer exoPlayer;

    SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_radio);
        Log.d("trace", "Create");

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        final String url = "http://icecast-studio21.cdnvideo.ru/S21_1";
        Uri uri = Uri.parse(url);
//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                mediaPlayer.setOnTimedMetaDataAvailableListener(new MediaPlayer.OnTimedMetaDataAvailableListener() {
//                    @Override
//                    public void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data) {
//                        Log.d("trace", "Data: " + data);
//                    }
//                });
//            }
//            mediaPlayer.setDataSource("http://icecast-studio21.cdnvideo.ru/S21_1");
//            mediaPlayer.prepare(); // might take long! (for buffering, etc)
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mediaPlayer.start();

//        prepareExoPlayerFromFileUri(Uri.parse(url));


        DataSource.Factory dataSourceFactory;
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl();

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        bandwidthMeter = new DefaultBandwidthMeter();

        dataSourceFactory = new IcyDataSourceFactory(this,
                Util.getUserAgent(this, "Studio21"), true, new PlayerCallback() {
            @Override
            public void playerStarted() {
                Log.d("meta_trace", "playerStarted");
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void playerPCMFeedBuffer(boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs) {

            }

            @Override
            public void playerStopped(int perf) {
                Log.d("meta_trace", "playerStopped");
            }

            @Override
            public void playerException(Throwable t) {
                Log.d("meta_trace", "playerException");
            }

            @Override
            public void playerMetadata(String key, String value) {
                Log.d("meta_trace", "playerMetadata " + value);
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void playerAudioTrackCreated(AudioTrack audioTrack) {

            }
        });
//        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Studio21"), (TransferListener<? super DataSource>) bandwidthMeter);

        ExtractorMediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);

        HlsMediaSource hlsMediaSource = new HlsMediaSource(uri, dataSourceFactory, null, null);

        player.prepare(mediaSource);
        player.addListener(eventListener);

        player.addMetadataOutput(new MetadataOutput() {
            @Override
            public void onMetadata(Metadata metadata) {
                Log.d("meta_trace", "onMetadata " + metadata);
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }
        });
        player.addTextOutput(new TextOutput() {
            @Override
            public void onCues(List<Cue> cues) {
                Log.d("meta_trace", "onMetadata " + cues);
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }
        });
        player.setTextOutput(new TextOutput() {
            @Override
            public void onCues(List<Cue> cues) {
                Log.d("meta_trace", "onMetadata " + cues);
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }
        });
        player.setMetadataOutput(new MetadataOutput() {
            @Override
            public void onMetadata(Metadata metadata) {
                Log.d("meta_trace", "onMetadata " + metadata);
                Toast.makeText(RadioActivity.this, "onMetadata", Toast.LENGTH_SHORT).show();
            }
        });


        player.setPlayWhenReady(true);
    }

    Player.EventListener eventListener = new Player.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            Toast.makeText(RadioActivity.this, "onTimelineChanged", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Toast.makeText(RadioActivity.this, "onPlayerStateChanged " + playWhenReady, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d("meta_trace", "onPlayerError");
            Toast.makeText(RadioActivity.this, "onPlayerError", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Toast.makeText(RadioActivity.this, "onPlaybackParametersChanged", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSeekProcessed() {

        }
    };

    private void prepareExoPlayerFromFileUri(Uri uri) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(), new DefaultLoadControl());
        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Toast.makeText(RadioActivity.this, "onPlayerStateChanged " + playWhenReady, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toast.makeText(RadioActivity.this, "onPlayerError", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });

        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (FileDataSource.FileDataSourceException e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };
        MediaSource audioSource = new ExtractorMediaSource(fileDataSource.getUri(),
                factory, new DefaultExtractorsFactory(), null, null);

        exoPlayer.prepare(audioSource);
        exoPlayer.setPlayWhenReady(true);
    }
}
