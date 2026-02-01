package com.soogbad.soogbadmusic;

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
        if(MusicService.getInstance() != null)
            MusicService.getInstance().updateMediaSessionData();
    }
    private static void raiseOnPausedValueChanged() {
        for(EmptyListener listener : onPausedValueChangedListeners)
            listener.onListenerInvoked();
    }

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
        if(MusicService.getInstance() != null)
            MusicService.getInstance().updateMediaSessionPlaybackState(paused, player.getCurrentTime());
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
        if(song == null || !song.getFile().exists())
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
        if(MusicService.getInstance() != null)
            MusicService.getInstance().updateMediaSessionPlaybackState(getPaused(), player.getCurrentTime());
    }

    public static void reset() {
        history = new ArrayList<>(); currentlyPlayedSongIndex = -1; player = null; paused = true; shuffle = false; filter = true; queue = new ArrayList<>();
    }

}
