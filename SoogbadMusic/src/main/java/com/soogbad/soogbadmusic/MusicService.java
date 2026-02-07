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
import androidx.media.utils.MediaConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicService extends MediaBrowserServiceCompat {

    private static MusicService instance = null;
    public static MusicService getInstance() { return instance; }

    private MediaSessionCompat mediaSession = null;
    @SuppressWarnings({"FieldCanBeLocal", "RedundantSuppression"})
    private final String NOTIFICATION_CHANNEL_ID = "soogbadmusic", MEDIA_ROOT_ID = "media_root", MEDIA_SUGGESTED_ID = "media_suggested";
    private final int NOTIFICATION_ID = 6969;
    private boolean isForeground = false;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, "SoogbadMusic", NotificationManager.IMPORTANCE_DEFAULT));
        initMediaSession();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Bundle extras = new Bundle();
        extras.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true); extras.putBoolean(MediaConstants.SESSION_EXTRAS_KEY_SLOT_RESERVATION_SKIP_TO_NEXT, true); extras.putBoolean(MediaConstants.SESSION_EXTRAS_KEY_SLOT_RESERVATION_SKIP_TO_PREV, true); extras.putBoolean(MediaConstants.TRANSPORT_CONTROLS_EXTRAS_KEY_SHUFFLE, true);
        if(rootHints != null && rootHints.getBoolean(BrowserRoot.EXTRA_SUGGESTED, false))
            return new BrowserRoot(MEDIA_SUGGESTED_ID, extras);
        else
            return new BrowserRoot(MEDIA_ROOT_ID, extras);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if(parentId.equals("none")) {
            result.sendResult(null);
            return;
        }
        if(Playlist.getMediaItems() == null) {
            result.detach();
            waitForPlaylistMediaItems(parentId, result);
        }
        else
            onPlaylistMediaItemsLoaded(parentId, result);
    }
    /** @noinspection StatementWithEmptyBody*/
    private void waitForPlaylistMediaItems(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Playlist.loadMediaItems();
        new Thread(() -> {
            while(!Playlist.getLoadMediaItemsComplete()) { }
            Playlist.setLoadMediaItemsComplete(false);
            onPlaylistMediaItemsLoaded(parentId, result);
        }).start();
    }
    private void onPlaylistMediaItemsLoaded(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if(parentId.equals(MEDIA_ROOT_ID))
            result.sendResult(Playlist.getMediaItems());
        else if(parentId.equals(MEDIA_SUGGESTED_ID)) {
            ArrayList<MediaBrowserCompat.MediaItem> forYou = new ArrayList<>();
            ArrayList<MediaBrowserCompat.MediaItem> mediaItems = Playlist.getMediaItems();
            if(mediaItems.size() >= 4) {
                Random random = new Random();
                for(int i = 1; i <= 4; i++) {
                    MediaBrowserCompat.MediaItem suggestedItem = mediaItems.get(random.nextInt(mediaItems.size()));
                    while(forYou.contains(suggestedItem))
                        suggestedItem = mediaItems.get(new Random().nextInt(mediaItems.size()));
                    forYou.add(suggestedItem);
                }
            }
            result.sendResult(forYou);
        }
        else
            result.sendResult(null);
    }

    @Override
    public void onSearch(@NonNull String query, Bundle extras, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        ArrayList<MediaBrowserCompat.MediaItem> results = new ArrayList<>();
        for(MediaBrowserCompat.MediaItem mediaItem : Playlist.getMediaItems())
            if(SongData.contains(mediaItem, query))
                results.add(mediaItem);
        result.sendResult(results);
    }

    private final MediaSessionCompat.Callback mediaSessionCallbacks = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() { super.onPlay(); PlaybackManager.setPaused(false); }
        @Override
        public void onPause() { super.onPause(); PlaybackManager.setPaused(true); }
        @Override
        public void onSkipToNext() { super.onSkipToNext(); PlaybackManager.nextSong(); }
        @Override
        public void onSkipToPrevious() { super.onSkipToPrevious(); PlaybackManager.previousSong(); }
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) { super.onPlayFromMediaId(mediaId, extras); Playlist.getSongs().forEach((song) -> { if(song.getPath().equals(mediaId)) PlaybackManager.switchSong(song); }); }
        @Override
        public void onPlayFromSearch(String query, Bundle extras) { super.onPlayFromSearch(query, extras); Playlist.getSongs().forEach((song) -> { if(song.getData().contains(query, false)) PlaybackManager.switchSong(song); }); }
    };

    private void initMediaSession() {
        if(mediaSession != null)
            mediaSession.release();
        mediaSession = new MediaSessionCompat(this, "SoogbadMusic");
        mediaSession.setCallback(mediaSessionCallbacks);
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
            else if(intent.getAction().equals("com.app.soogbadmusic.ACTION_KILL")) {
                killService();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    private void killService() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
        if(mediaSession != null)
            mediaSession.release();
        if(MainActivity.getInstance() != null)
            MainActivity.getInstance().finishAndRemoveTask();
    }

}
