package com.soogbad.soogbadmusic;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;
import androidx.media3.session.SessionError;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaLibraryService {

    @SuppressWarnings({"FieldCanBeLocal", "RedundantSuppression"})
    public static final String NOTIFICATION_CHANNEL_ID = "soogbadmusic", MEDIA_ROOT_ID = "media_root";

    private static MusicService instance = null;
    public static MusicService getInstance() { return instance; }

    private MediaLibrarySession mediaSession = null;
    private final ArrayList<MediaSession.ControllerInfo> subscribers = new ArrayList<>();
    public ArrayList<MediaSession.ControllerInfo> getSubscribers() { return subscribers; }

    private final int NOTIFICATION_ID = 6969;
    private boolean isForeground = false, isLoadingSongs = false;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        if(MainActivity.getInstance() == null)
            killService();
    }

    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        if(mediaSession == null)
            createMediaSession();
        return mediaSession;
    }

    private void createMediaSession() {
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, "SoogbadMusic", NotificationManager.IMPORTANCE_DEFAULT));
        ExoPlayer player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaLibrarySession.Builder(this, wrapPlayer(player), new MusicLibrarySessionCallback()).setId("SoogbadMusic").build();
    }
    @OptIn(markerClass = UnstableApi.class)
    private ForwardingPlayer wrapPlayer(ExoPlayer player) {
        return new ForwardingPlayer(player) {
            @Override public void play() { PlaybackManager.setPaused(false); }
            @Override public void pause() { PlaybackManager.setPaused(true); }
            @Override public void seekTo(long positionMs) { PlaybackManager.setCurrentTime(positionMs); }
            @Override public boolean isCommandAvailable(int command) { return command != COMMAND_GET_TIMELINE && (command == COMMAND_SET_SHUFFLE_MODE || command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM || command == COMMAND_SEEK_TO_PREVIOUS || command == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM || super.isCommandAvailable(command)); }
            @NonNull @Override public Commands getAvailableCommands() { return super.getAvailableCommands().buildUpon().remove(COMMAND_GET_TIMELINE).add(COMMAND_SET_SHUFFLE_MODE).add(COMMAND_SEEK_TO_NEXT).add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(COMMAND_SEEK_TO_PREVIOUS).add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build(); }
            @Override public void seekToNext() { PlaybackManager.nextSong(); }
            @Override public void seekToNextMediaItem() { PlaybackManager.nextSong(); }
            @Override public void seekToPrevious() { PlaybackManager.previousSong(); }
            @Override public void seekToPreviousMediaItem() { PlaybackManager.previousSong(); }
            @Override public void setMediaItems(@NonNull List<MediaItem> mediaItems) { }
            @Override public void setMediaItems(@NonNull List<MediaItem> mediaItems, boolean resetPosition) { }
            @Override public void setMediaItems(@NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) { }
        };
    }

    public void setSessionPlayer(ExoPlayer player) {
        if(mediaSession != null) {
            if(mediaSession.getPlayer().getMediaItemCount() == 0)
                mediaSession.getPlayer().release();
            mediaSession.setPlayer(wrapPlayer(player));
        }
    }

    @Override
    public void onUpdateNotification(@NonNull MediaSession session, boolean startInForegroundRequired) { }

    @OptIn(markerClass = UnstableApi.class)
    private Notification buildMediaNotification(SongData data) {
        Intent prevActionIntent = new Intent(this, MusicService.class).setAction("com.app.soogbadmusic.ACTION_PREV");
        Intent nextActionIntent = new Intent(this, MusicService.class).setAction("com.app.soogbadmusic.ACTION_NEXT");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)).setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(true)
                .setSmallIcon(R.drawable.icon).setLargeIcon(data.AlbumCover).setContentTitle(data.Artist + " - " + data.Title).setContentText(data.Album + " (" + data.Year + ")")
                .addAction(new NotificationCompat.Action(R.drawable.previous, "Previous", PendingIntent.getService(this, 0, prevActionIntent, PendingIntent.FLAG_IMMUTABLE)))
                .addAction(new NotificationCompat.Action(R.drawable.next, "Next", PendingIntent.getService(this, 0, nextActionIntent, PendingIntent.FLAG_IMMUTABLE)));
        return builder.build();
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

    public void updateMediaSessionData() {
        if(PlaybackManager.getPlayer() != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildMediaNotification(PlaybackManager.getPlayer().getSong().getData()));
    }
    public void updateMediaSessionPlaybackState(boolean pausedState) {
        if(mediaSession == null) return;
        if(pausedState)
            bringServiceToBackground();
        else if(PlaybackManager.getPlayer() != null)
            bringServiceToForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(intent != null && intent.getAction() != null) {
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
        releaseMediaSession();
        if(MainActivity.getInstance() != null)
            MainActivity.getInstance().finishAndRemoveTask();
    }

    private void releaseMediaSession() {
        if(mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
    }

    public void loadPlaylistMediaItems() {
        if(!isLoadingSongs) {
            Playlist.loadMediaItems();
            isLoadingSongs = true;
        }
    }

    public void notifyMediaItemsChanged() {
        if(mediaSession != null)
            for(MediaSession.ControllerInfo browser : subscribers)
                mediaSession.notifyChildrenChanged(browser, MEDIA_ROOT_ID, Playlist.getSongs().size(), null);
    }

    private class MusicLibrarySessionCallback implements MediaLibrarySession.Callback {

        @NonNull @Override
        public ListenableFuture<LibraryResult<Void>> onSubscribe(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId, @Nullable LibraryParams params) {
            subscribers.add(browser);
            return Futures.immediateFuture(LibraryResult.ofVoid());
        }
        @NonNull @Override
        public ListenableFuture<LibraryResult<Void>> onUnsubscribe(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId) {
            subscribers.remove(browser);
            return Futures.immediateFuture(LibraryResult.ofVoid());
        }

        @OptIn(markerClass = UnstableApi.class)
        @NonNull @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @Nullable LibraryParams params) {
            if(params != null && (params.isRecent || params.isSuggested))
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED));
            MediaItem rootItem = new MediaItem.Builder().setMediaId(MEDIA_ROOT_ID).setMediaMetadata(new MediaMetadata.Builder().setIsPlayable(false).setIsBrowsable(true).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()).build();
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
        }

        @NonNull @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId, int page, int pageSize, @Nullable LibraryParams params) {
            if(parentId.equals("none") || MainActivity.getInstance() == null)
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params));
            else if(!isLoadingSongs && Playlist.getMediaItems() != null && Playlist.getMediaItems().size() == Playlist.getSongs().size())
                return Futures.immediateFuture(LibraryResult.ofItemList(Playlist.getMediaItems(), params));
            else {
                loadPlaylistMediaItems();
                return waitForPlaylistMediaItems(params);
            }
        }
        /** @noinspection StatementWithEmptyBody*/
        private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> waitForPlaylistMediaItems(@Nullable LibraryParams params) {
            return Futures.submit(() -> {
                while(!Playlist.getLoadMediaItemsComplete()) { }
                Playlist.setLoadMediaItemsComplete(false);
                isLoadingSongs = false;
                return LibraryResult.ofItemList(Playlist.getMediaItems(), params);
            }, command -> new Thread(command).start());
        }

        @NonNull @Override
        public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, @Nullable LibraryParams params) {
            int size = 0;
            for(Song song : Playlist.getSongs())
                if(song.getData().contains(query, false) && song.getMediaItem() != null)
                    size++;
            session.notifySearchResultChanged(browser, query, size, params);
            return Futures.immediateFuture(LibraryResult.ofVoid(params));
        }

        @NonNull @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, int page, int pageSize, @Nullable LibraryParams params) {
            ArrayList<MediaItem> results = new ArrayList<>();
            for(Song song : Playlist.getSongs())
                if(song.getData().contains(query, false) && song.getMediaItem() != null)
                    results.add(song.getMediaItem());
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(results), params));
        }

        @OptIn(markerClass = UnstableApi.class)
        @NonNull @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
            if(!mediaItems.isEmpty()) {
                MediaItem item = mediaItems.get(0);
                if(item.requestMetadata.searchQuery != null) {
                    String query = item.requestMetadata.searchQuery;
                    Playlist.getSongs().stream().filter(song -> song.getData().contains(query, false) && song.getMediaItem() != null).findFirst().ifPresent(PlaybackManager::switchSong);
                }
                else {
                    String mediaId = item.mediaId;
                    Playlist.getSongs().stream().filter(song -> song.getPath().equals(mediaId) && song.getMediaItem() != null).findFirst().ifPresent(PlaybackManager::switchSong);
                }
            }
            return Futures.immediateFuture(new MediaSession.MediaItemsWithStartPosition(ImmutableList.of(), -1, 0));
        }

    }

}
