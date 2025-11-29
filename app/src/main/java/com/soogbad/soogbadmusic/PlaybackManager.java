package com.soogbad.soogbadmusic;

import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import java.util.ArrayList;
import java.util.Random;

public class PlaybackManager {

    private static final ArrayList<Song> history = new ArrayList<>();
    private static int currentlyPlayedSongIndex = -1;
    private static Player player = null;
    private static boolean paused = true;
    private static boolean shuffle = false;
    private static boolean filter = true;
    private static final ArrayList<Song> queue = new ArrayList<>();

    private static final ArrayList<EmptyListener> onPausedValueChangedListeners = new ArrayList<>();
    public static void addOnPausedValueChangedListener(EmptyListener listener) {
        onPausedValueChangedListeners.add(listener);
    }
    private static final ArrayList<EmptyListener> onSongChangedListeners = new ArrayList<>();
    public static void addOnSongChangedListener(EmptyListener listener) {
        onSongChangedListeners.add(listener);
    }
    public static void raiseOnSongChanged() {
        for(EmptyListener listener : onSongChangedListeners)
            listener.onListenerInvoked();
    }
    private static void raiseOnPausedValueChanged() {
        for(EmptyListener listener : onPausedValueChangedListeners)
            listener.onListenerInvoked();
    }

    private static MediaSession mediaSession = null;
    public static void setMediaSession(MediaSession mediaSession) { PlaybackManager.mediaSession = mediaSession; }


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
        if(player.getSong().hasNotRefreshedAlbumCoverAndLyrics())
            player.getSong().refreshAlbumCoverAndLyrics(true);
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

}
