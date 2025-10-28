package com.soogbad.soogbadmusic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class Playlist {

    private static File directory;
    private static ArrayList<Song> songs = new ArrayList<>();
    private static boolean refreshSongsComplete = false;
    private static double refreshSongsProgress = 0;
    private static boolean isAccessingRefreshSongsProgress = false;
    private static Thread lastRefreshThread = null;
    private static boolean stopLastRefresh = false;

    public static File getDirectory() {
        return directory;
    }
    public static void setDirectory(File directory) {
        Playlist.directory = directory;
    }
    public static ArrayList<Song> getSongs() {
        return songs;
    }
    public static boolean getRefreshSongsComplete() {
        return refreshSongsComplete;
    }
    public static void setRefreshSongsComplete(boolean refreshSongsComplete) {
        Playlist.refreshSongsComplete = refreshSongsComplete;
    }
    public static double getRefreshSongsProgress() {
        return refreshSongsProgress;
    }
    public static void setRefreshSongsProgress(double refreshSongsProgress) {
        Playlist.refreshSongsProgress = refreshSongsProgress;
    }
    public static boolean isAccessingRefreshSongsProgress() { return isAccessingRefreshSongsProgress; }


    public static void refreshSongs(boolean calculateProgress) {
        if(lastRefreshThread != null) {
            stopLastRefresh = true;
            while(lastRefreshThread.isAlive()) { }
            stopLastRefresh = false;
        }
        lastRefreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                songs = new ArrayList<>();
                File[] files = directory.listFiles();
                if(files != null) {
                    int songsTotal = files.length;
                    if(PlayerManager.getFilter()) {
                        songsTotal = 0;
                        for(File file : files) {
                            if(stopLastRefresh)
                                return;
                            if(file.getAbsolutePath().toLowerCase().endsWith(".mp3") && !file.getName().toLowerCase().startsWith("_"))
                                songsTotal++;
                        }
                    }
                    for(int i = 0, j = 0; i < files.length; i++) {
                        if(stopLastRefresh)
                            return;
                        if(files[i].getAbsolutePath().toLowerCase().endsWith(".mp3") && (!files[i].getName().toLowerCase().startsWith("_") || !PlayerManager.getFilter())) {
                            songs.add(new Song(files[i]));
                            if(calculateProgress) {
                                isAccessingRefreshSongsProgress = true;
                                refreshSongsProgress = (double) j / songsTotal;
                                isAccessingRefreshSongsProgress = false;
                            }
                            j++;
                        }
                    }
                }
                sortSongs();
                refreshSongsComplete = true;
                for(Song song : songs) {
                    if(stopLastRefresh)
                        return;
                    if(!song.getRefreshedAlbumCoverAndLyrics())
                        song.refreshAlbumCoverAndLyrics(false);
                }
            }
        });
        lastRefreshThread.start();
    }

    public static void sortSongs() {
        songs.sort(new SongComparator());
    }

}
