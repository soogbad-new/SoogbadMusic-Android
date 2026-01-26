package com.soogbad.soogbadmusic;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Random;

public class PlaybackManager {

    private static ArrayList<Song> history = new ArrayList<>();
    private static int currentlyPlayedSongIndex = -1;
    private static Player player = null;
    private static boolean paused = true;
    private static boolean shuffle = false;
    private static boolean filter = true;
    private static ArrayList<Song> queue = new ArrayList<>();
    private static MediaSession mediaSession = null;

    private static final ArrayList<EmptyListener> onPausedValueChangedListeners = new ArrayList<>();
    public static void addOnPausedValueChangedListener(EmptyListener listener) {
        onPausedValueChangedListeners.add(listener);
    }
    private static final ArrayList<EmptyListener> onSongChangedListeners = new ArrayList<>();
    public static void addOnSongChangedListener(EmptyListener listener) {
        onSongChangedListeners.add(listener);
    }
    public static void raiseOnSongChanged() {
        if(getPlayer().getSong().getData().AlbumCover == null || getPlayer().getSong().getData().Lyrics.isEmpty())
            getPlayer().getSong().loadAlbumCoverAndLyrics();
        for(EmptyListener listener : onSongChangedListeners)
            listener.onListenerInvoked();
        updateMediaSessionNotificationData();
    }
    private static void raiseOnPausedValueChanged() {
        for(EmptyListener listener : onPausedValueChangedListeners)
            listener.onListenerInvoked();
    }

    public static MediaSession getMediaSession() { return mediaSession; }
    public static Player getPlayer() { return player; }

    public static boolean getShuffle() { return shuffle; }
    public static void setShuffle(boolean shuffle) {
        PlaybackManager.shuffle = shuffle;
    }
    public static boolean getFilter() { return filter; }
    public static void setFilter(boolean filter) { PlaybackManager.filter = filter; }
    public static boolean getPaused() {
        return paused;
    }
    public static void setPaused(boolean paused) {
        if(!shuffle && player == null && queue.isEmpty())
            return;
        else if(!paused && player != null && player.getStopped())
            return;
        PlaybackManager.paused = paused;
        if(player == null)
            nextSong();
        else {
            if(paused)
                player.pause();
            else
                player.play();
        }
        if(mediaSession != null)
            mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(paused ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING, (long)(1000 * player.getCurrentTime()), paused ? 0 : 1).build());
        raiseOnPausedValueChanged();
    }


    private static void createPlayer() {
        if(player != null)
            player.release();
        player = new Player(history.get(currentlyPlayedSongIndex));
        player.addOnPlaybackCompletedListener(PlaybackManager::nextSong);
        if(!paused)
            player.play();
    }

    public static void nextSong() {
        if(Playlist.getSongs().isEmpty()) {
            setPaused(true);
            return;
        }
        if(queue.isEmpty()) {
            if(currentlyPlayedSongIndex + 1 > history.size() - 1) {
                if(shuffle) {
                    Song song = Playlist.getSongs().get(new Random().nextInt(Playlist.getSongs().size()));
                    if(!song.getFile().exists()) {
                        nextSong();
                        return;
                    }
                    history.add(song);
                    currentlyPlayedSongIndex = history.size() - 1;
                    createPlayer();
                    raiseOnSongChanged();
                }
                else if(!paused && getPlayer().getStopped())
                    PlaybackManager.setPaused(true);
            }
            else {
                currentlyPlayedSongIndex++;
                if(!history.get(currentlyPlayedSongIndex).getFile().exists()) {
                    history.remove(currentlyPlayedSongIndex);
                    nextSong();
                    return;
                }
                createPlayer();
                raiseOnSongChanged();
            }
        }
        else {
            if(!queue.get(0).getFile().exists()) {
                queue.remove(0);
                nextSong();
                return;
            }
            history.add(queue.get(0));
            currentlyPlayedSongIndex = history.size() - 1;
            createPlayer();
            queue.remove(0);
            raiseOnSongChanged();
        }
    }
    public static void previousSong() {
        if(Playlist.getSongs().isEmpty())
            return;
        if(currentlyPlayedSongIndex > 0) {
            currentlyPlayedSongIndex--;
            if(!history.get(currentlyPlayedSongIndex).getFile().exists()) {
                history.remove(currentlyPlayedSongIndex);
                previousSong();
                return;
            }
            createPlayer();
            raiseOnSongChanged();
        }
    }

    public static void switchSong(Song song) {
        if(!song.getFile().exists())
            return;
        history.add(song);
        currentlyPlayedSongIndex = history.size() - 1;
        createPlayer();
        setPaused(false);
        raiseOnSongChanged();
    }

    public static void addToQueue(Song song) {
        queue.add(song);
    }
    public static void removeFromQueue(Song song) {
        while(queue.contains(song))
            queue.remove(song);
    }
    public static boolean queueContains(Song song) {
        return queue.contains(song);
    }

    public static void setCurrentTime(double time) {
        getPlayer().setCurrentTime(time);
        if(mediaSession != null)
            mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlaybackManager.getPaused() ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING, (long) (1000 * PlaybackManager.getPlayer().getCurrentTime()), PlaybackManager.getPaused() ? 0 : 1).build());
    }

    public static void initMediaSessionNotification(MainActivity mainActivity) {
        NotificationManager notificationManager = SoogbadMusicApplication.getAppContext().getSystemService(NotificationManager.class);
        mediaSession = new MediaSession(SoogbadMusicApplication.getAppContext(), "SoogbadMusic");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() { PlaybackManager.setPaused(false); }
            @Override
            public void onPause() { PlaybackManager.setPaused(true); }
            @Override
            public void onSkipToNext() { PlaybackManager.nextSong(); }
            @Override
            public void onSkipToPrevious() { PlaybackManager.previousSong(); }
            @Override
            public void onStop() { notificationManager.cancel(6969); mainActivity.finishAndRemoveTask(); }
        });
        mediaSession.setActive(true);
        new MediaController(SoogbadMusicApplication.getAppContext(), mediaSession.getSessionToken()).registerCallback(new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackState state) { super.onPlaybackStateChanged(state); }
            @Override
            public void onMetadataChanged(@Nullable MediaMetadata metadata) { super.onMetadataChanged(metadata); }
        });
        mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlaybackState.STATE_NONE, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0).build());
        notificationManager.createNotificationChannel(new NotificationChannel("soogbadmusic", "SoogbadMusic", NotificationManager.IMPORTANCE_DEFAULT));
    }
    private static void updateMediaSessionNotificationData() {
        SongData data = getPlayer().getSong().getData();
        mediaSession.setMetadata(new MediaMetadata.Builder().putLong(MediaMetadata.METADATA_KEY_DURATION, (long)(PlaybackManager.getPlayer().getSong().getDuration() * 1000)).putString(MediaMetadata.METADATA_KEY_TITLE, data.Title).putString(MediaMetadata.METADATA_KEY_ARTIST, data.Artist).putString(MediaMetadata.METADATA_KEY_ALBUM, data.Album).putLong(MediaMetadata.METADATA_KEY_YEAR, data.Year).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, data.AlbumCover).build());
        mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlaybackManager.getPaused() ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING, (long)(1000 * PlaybackManager.getPlayer().getCurrentTime()), PlaybackManager.getPaused() ? 0 : 1).build());
        Intent prevActionIntent = new Intent(SoogbadMusicApplication.getAppContext(), MusicService.class).setAction("com.app.soogbadmusic.ACTION_PREV");
        Intent nextActionIntent = new Intent(SoogbadMusicApplication.getAppContext(), MusicService.class).setAction("com.app.soogbadmusic.ACTION_NEXT");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(SoogbadMusicApplication.getAppContext(), "soogbadmusic")
                .setSmallIcon(R.drawable.ic_launcher).setLargeIcon(data.AlbumCover).setContentTitle(data.Artist + " - " + data.Title).setContentText(data.Album + " (" + data.Year + ")")
                .setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(true).setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                .addAction(new NotificationCompat.Action(R.drawable.previous, "Previous", PendingIntent.getService(SoogbadMusicApplication.getAppContext(), 0, prevActionIntent, PendingIntent.FLAG_IMMUTABLE)))
                .addAction(new NotificationCompat.Action(R.drawable.next, "Next", PendingIntent.getService(SoogbadMusicApplication.getAppContext(), 0, nextActionIntent, PendingIntent.FLAG_IMMUTABLE)));
        if(ActivityCompat.checkSelfPermission(SoogbadMusicApplication.getAppContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            NotificationManagerCompat.from(SoogbadMusicApplication.getAppContext()).notify(6969, builder.build());
    }

    public static void reset() {
        history = new ArrayList<>(); currentlyPlayedSongIndex = -1; player = null; paused = true; shuffle = false; filter = true; queue = new ArrayList<>();
    }

}
