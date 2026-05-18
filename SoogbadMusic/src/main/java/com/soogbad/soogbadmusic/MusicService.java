package com.soogbad.soogbadmusic;

import android.annotation.SuppressLint;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends MediaLibraryService {

    public static final String MEDIA_ROOT_ID = "media_root", MEDIA_SUGGESTED_ID = "media_suggested";

    private static MusicService instance = null;
    public static MusicService getInstance() { return instance; }

    private MediaLibrarySession mediaSession = null;
    private boolean isLoadingSongs = false;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        if(MainActivity.getInstance() == null)
            stopSelf();
    }

    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        if(mediaSession == null) {
            ExoPlayer player = new ExoPlayer.Builder(this).build();
            mediaSession = new MediaLibrarySession.Builder(this, wrapPlayer(player), new MusicLibrarySessionCallback()).setId("SoogbadMusic").build();
        }
        return mediaSession;
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
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        instance = null;
        super.onDestroy();
        if(MainActivity.getInstance() != null)
            MainActivity.getInstance().finishAndRemoveTask();
    }

    public void notifyChildrenChanged(String parentId) {
        if(mediaSession != null)
            mediaSession.notifyChildrenChanged(parentId, 0, null);
    }

    public void setSessionPlayer(ExoPlayer player) {
        if(mediaSession != null) {
            if(mediaSession.getPlayer().getMediaItemCount() == 0)
                mediaSession.getPlayer().release();
            mediaSession.setPlayer(wrapPlayer(player));
        }
    }
    @SuppressLint("UnsafeOptInUsageError")
    private ForwardingPlayer wrapPlayer(ExoPlayer player) {
        return new ForwardingPlayer(player) {
            @Override public void play() { PlaybackManager.setPaused(false); }
            @Override public void pause() { PlaybackManager.setPaused(true); }
            @Override public void seekTo(long positionMs) { PlaybackManager.setCurrentTime(positionMs); }
            @Override public boolean isCommandAvailable(int command) { return command == COMMAND_SEEK_TO_NEXT || command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM || command == COMMAND_SEEK_TO_PREVIOUS || command == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM || super.isCommandAvailable(command); }
            @NonNull @Override public Commands getAvailableCommands() { return super.getAvailableCommands().buildUpon().add(COMMAND_SEEK_TO_NEXT).add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(COMMAND_SEEK_TO_PREVIOUS).add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build(); }
            @Override public void seekToNext() { PlaybackManager.nextSong(); }
            @Override public void seekToNextMediaItem() { PlaybackManager.nextSong(); }
            @Override public void seekToPrevious() { PlaybackManager.previousSong(); }
            @Override public void seekToPreviousMediaItem() { PlaybackManager.previousSong(); }
        };
    }

    private class MusicLibrarySessionCallback implements MediaLibrarySession.Callback {

        @NonNull @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @Nullable LibraryParams params) {
            MediaMetadata metadata = new MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build();
            MediaItem rootItem = new MediaItem.Builder().setMediaId(MEDIA_ROOT_ID).setMediaMetadata(metadata).build();
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
        }

        
        @NonNull @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String parentId, int page, int pageSize, @Nullable LibraryParams params) {
            if(parentId.equals("none") || MainActivity.getInstance() == null)
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params));
            if(Playlist.getMediaItems() == null || isLoadingSongs)
                return waitForPlaylistMediaItems(parentId, params);
            else
                return Futures.immediateFuture(getChildrenResult(parentId, params));
        }

        @NonNull @Override
        public ListenableFuture<LibraryResult<Void>> onSearch(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, @Nullable LibraryParams params) {
            ArrayList<MediaItem> results = getSearchResults(query);
            session.notifySearchResultChanged(browser, query, results.size(), params);
            return Futures.immediateFuture(LibraryResult.ofVoid(params));
        }
        @NonNull @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetSearchResult(@NonNull MediaLibrarySession session, @NonNull MediaSession.ControllerInfo browser, @NonNull String query, int page, int pageSize, @Nullable LibraryParams params) {
            ArrayList<MediaItem> results = getSearchResults(query);
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(results), params));
        }

        @SuppressLint("UnsafeOptInUsageError")
        @NonNull @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, @NonNull java.util.List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
            if(!mediaItems.isEmpty()) {
                String mediaId = mediaItems.get(0).mediaId;
                Playlist.getSongs().stream().filter(song -> song.getPath().equals(mediaId)).findFirst().ifPresent(PlaybackManager::switchSong);
            }
            return Futures.immediateFuture(new MediaSession.MediaItemsWithStartPosition(ImmutableList.of(), -1, 0));
        }
        
    }

    /** @noinspection StatementWithEmptyBody*/
    private ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> waitForPlaylistMediaItems(@NonNull String parentId, @Nullable MediaLibraryService.LibraryParams params) {
        if(!isLoadingSongs) {
            Playlist.loadMediaItems();
            isLoadingSongs = true;
        }
        return Futures.submit(() -> {
            while(!Playlist.getLoadMediaItemsComplete()) { }
            Playlist.setLoadMediaItemsComplete(false);
            isLoadingSongs = false;
            return getChildrenResult(parentId, params);
        }, command -> new Thread(command).start());
    }

    private LibraryResult<ImmutableList<MediaItem>> getChildrenResult(@NonNull String parentId, @Nullable MediaLibraryService.LibraryParams params) {
        if(parentId.equals(MEDIA_ROOT_ID)) {
            ArrayList<MediaItem> items = Playlist.getMediaItems();
            return LibraryResult.ofItemList(items != null ? ImmutableList.copyOf(items) : ImmutableList.of(), params);
        }
        else if(parentId.equals(MEDIA_SUGGESTED_ID)) {
            ArrayList<MediaItem> forYou = new ArrayList<>();
            ArrayList<MediaItem> mediaItems = Playlist.getMediaItems();
            if(mediaItems != null && mediaItems.size() >= 4) {
                Random random = new Random();
                for(int i = 1; i <= 4; i++) {
                    MediaItem suggestedItem = mediaItems.get(random.nextInt(mediaItems.size()));
                    while(forYou.contains(suggestedItem))
                        suggestedItem = mediaItems.get(new Random().nextInt(mediaItems.size()));
                    forYou.add(suggestedItem);
                }
            }
            return LibraryResult.ofItemList(ImmutableList.copyOf(forYou), params);
        }
        else
            return LibraryResult.ofItemList(ImmutableList.of(), params);
    }

    private ArrayList<MediaItem> getSearchResults(String query) {
        ArrayList<MediaItem> results = new ArrayList<>();
        ArrayList<MediaItem> mediaItems = Playlist.getMediaItems();
        if(mediaItems != null)
            for(MediaItem mediaItem : mediaItems)
                if(SongData.contains(mediaItem.mediaMetadata, query))
                    results.add(mediaItem);
        return results;
    }

}
