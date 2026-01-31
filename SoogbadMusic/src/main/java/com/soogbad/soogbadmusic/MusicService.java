package com.soogbad.soogbadmusic;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;

import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private static MusicService instance = null;
    public static MusicService getInstance() { return instance; }

    private MediaSessionCompat mediaSession = null;
    private final String NOTIFICATION_CHANNEL_ID = "soogbadmusic";
    private final String MEDIA_ROOT_ID = "soogbadmusic";
    private final int NOTIFICATION_ID = 6969;
    private boolean isForeground = false;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, "SoogbadMusic", NotificationManager.IMPORTANCE_DEFAULT));
        initMediaSession();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(MainActivity.getInstance() == null)
            startActivity(new Intent(getApplicationContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return super.onBind(intent);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    /** @noinspection StatementWithEmptyBody*/
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if(parentId.equals(MEDIA_ROOT_ID)) {
            if(Playlist.getMediaItems() != null)
                result.sendResult(Playlist.getMediaItems());
            else {
                result.detach();
                new Thread(() -> {
                    while(!Playlist.getLoadMediaItemsComplete()) { }
                    Playlist.setLoadMediaItemsComplete(false);
                    result.sendResult(Playlist.getMediaItems());
                }).start();
            }
        }
        else
            result.sendResult(null);
    }

    private void initMediaSession() {
        if(mediaSession != null)
            mediaSession.release();
        mediaSession = new MediaSessionCompat(this, "SoogbadMusic");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { PlaybackManager.setPaused(false); }
            @Override
            public void onPause() { PlaybackManager.setPaused(true); }
            @Override
            public void onSkipToNext() { PlaybackManager.nextSong(); }
            @Override
            public void onSkipToPrevious() { PlaybackManager.previousSong(); }
            @Override
            public void onPlayFromSearch(String query, Bundle extras) { super.onPlayFromSearch(query, extras); onPlay(); }
        });
        mediaSession.setActive(true);
        new MediaControllerCompat(this, mediaSession.getSessionToken()).registerCallback(new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackStateCompat state) { super.onPlaybackStateChanged(state); }
            @Override
            public void onMetadataChanged(@Nullable MediaMetadataCompat metadata) { super.onMetadataChanged(metadata); }
        });
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT).setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0).build());
        setSessionToken(mediaSession.getSessionToken());
    }

    public void updateMediaSessionData() {
        SongData data = PlaybackManager.getPlayer().getSong().getData();
        mediaSession.setMetadata(new MediaMetadataCompat.Builder().putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long)(PlaybackManager.getPlayer().getSong().getDuration() * 1000)).putString(MediaMetadataCompat.METADATA_KEY_TITLE, data.Title).putString(MediaMetadataCompat.METADATA_KEY_ARTIST, data.Artist).putString(MediaMetadataCompat.METADATA_KEY_ALBUM, data.Album).putLong(MediaMetadataCompat.METADATA_KEY_YEAR, data.Year).putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, data.AlbumCover).build());
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT).setState(PlaybackManager.getPaused() ? PlaybackStateCompat.STATE_PAUSED : PlaybackStateCompat.STATE_PLAYING, (long)(1000 * PlaybackManager.getPlayer().getCurrentTime()), PlaybackManager.getPaused() ? 0 : 1).build());
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildMediaNotification(data));
    }
    private Notification buildMediaNotification(SongData data) {
        Intent prevActionIntent = new Intent(this, MusicService.class).setAction("com.app.soogbadmusic.ACTION_PREV");
        Intent nextActionIntent = new Intent(this, MusicService.class).setAction("com.app.soogbadmusic.ACTION_NEXT");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.getSessionToken())).setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher).setLargeIcon(data.AlbumCover).setContentTitle(data.Artist + " - " + data.Title).setContentText(data.Album + " (" + data.Year + ")")
                .addAction(new NotificationCompat.Action(R.drawable.previous, "Previous", PendingIntent.getService(this, 0, prevActionIntent, PendingIntent.FLAG_IMMUTABLE)))
                .addAction(new NotificationCompat.Action(R.drawable.next, "Next", PendingIntent.getService(this, 0, nextActionIntent, PendingIntent.FLAG_IMMUTABLE)));
        return builder.build();
    }

    public void updateMediaSessionPlaybackState(boolean pausedState, double currentTime) {
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT).setState(pausedState ? PlaybackStateCompat.STATE_PAUSED : PlaybackStateCompat.STATE_PLAYING, (long)(1000 * currentTime), pausedState ? 0 : 1).build());
        if(pausedState)
            bringServiceToBackground();
        else if(PlaybackManager.getPlayer() != null)
            bringServiceToForeground();
    }
    private void bringServiceToBackground() {
        stopForeground(STOP_FOREGROUND_DETACH);
        if(PlaybackManager.getPlayer() != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildMediaNotification(PlaybackManager.getPlayer().getSong().getData()));
        isForeground = false;
    }
    private void bringServiceToForeground() {
        if(!isForeground)
            startForeground(NOTIFICATION_ID, buildMediaNotification(PlaybackManager.getPlayer().getSong().getData()), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildMediaNotification(PlaybackManager.getPlayer().getSong().getData()));
        isForeground = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if(intent.getAction().equals("com.app.soogbadmusic.ACTION_PREV"))
                PlaybackManager.previousSong();
            else if(intent.getAction().equals("com.app.soogbadmusic.ACTION_NEXT"))
                PlaybackManager.nextSong();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        stopForeground(STOP_FOREGROUND_REMOVE);
        getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
        if(mediaSession != null)
            mediaSession.release();
        if(MainActivity.getInstance() != null)
            MainActivity.getInstance().finishAndRemoveTask();
    }

}
