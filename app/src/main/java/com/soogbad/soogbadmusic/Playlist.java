package com.soogbad.soogbadmusic;

import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    public static void refreshSongs() {
        if(lastRefreshThread != null) {
            stopLastRefresh = true;
            while(lastRefreshThread.isAlive()) { }
            stopLastRefresh = false;
        }
        lastRefreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                songs = new ArrayList<>();
                ArrayList<Path> files = new ArrayList<>();
                try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directory.getAbsolutePath()), "*.mp3")) {
                    if(!PlayerManager.getFilter())
                        stream.forEach(files::add);
                    else
                        for(Path filePath : stream)
                            if(!filePath.getFileName().toString().startsWith("_"))
                                files.add(filePath);
                } catch(IOException e) { throw new RuntimeException(); }
                int i = 0;
                for(Path file : files) {
                    if(stopLastRefresh)
                        return;
                    songs.add(new Song(file.toFile()));
                    isAccessingRefreshSongsProgress = true;
                    refreshSongsProgress = (double)i / files.size();
                    isAccessingRefreshSongsProgress = false;
                    i++;
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
