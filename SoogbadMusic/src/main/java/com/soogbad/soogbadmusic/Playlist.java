package com.soogbad.soogbadmusic;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Playlist {

    private static File directory;
    private static ArrayList<Song> songs = new ArrayList<>();
    private static boolean refreshSongsComplete = false;
    private static double refreshSongsProgress = 0;
    private static boolean isAccessingRefreshSongsProgress = false;
    private static Thread lastRefreshThread = null;
    private static boolean stopLastRefresh = false;

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


    /** @noinspection StatementWithEmptyBody*/
    public static void refreshSongs() {
        if(lastRefreshThread != null) {
            stopLastRefresh = true;
            while(lastRefreshThread.isAlive()) { }
            stopLastRefresh = false;
        }
        lastRefreshThread = new Thread(() -> {
            songs = new ArrayList<>();
            ArrayList<Path> files = new ArrayList<>();
            try(DirectoryStream<Path> stream = PlaybackManager.getFilter()
                    ? Files.newDirectoryStream(Paths.get(directory.getAbsolutePath()), filePath -> {
                String filename = filePath.getFileName().toString(); return !filename.startsWith("_") && filename.endsWith(".mp3");
            })
                    : Files.newDirectoryStream(Paths.get(directory.getAbsolutePath()), "*.mp3")) {
                stream.forEach(files::add);
            } catch(IOException e) { throw new RuntimeException(); }
            int i = 0;
            for(Path filePath : files) {
                if(stopLastRefresh)
                    return;
                songs.add(new Song(filePath.toFile()));
                isAccessingRefreshSongsProgress = true;
                refreshSongsProgress = (double)i / files.size();
                isAccessingRefreshSongsProgress = false;
                i++;
            }
            sortSongs();
            refreshSongsComplete = true;
        });
        lastRefreshThread.start();
    }

    public static void sortSongs() {
        songs.sort(new SongComparator());
    }

}
