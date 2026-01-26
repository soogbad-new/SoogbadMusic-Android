package com.soogbad.soogbadmusic;

import android.os.Environment;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Playlist {

    private static ArrayList<Song> songs = new ArrayList<>();
    private static boolean refreshSongsComplete = false;
    private static double refreshSongsProgress = 0;
    private static boolean isAccessingRefreshSongsProgress = false;
    private static Thread lastRefreshThread = null;
    private static boolean stopLastRefresh = false;

    public static ArrayList<Song> getSongs() { return songs; }
    public static boolean getRefreshSongsComplete() { return refreshSongsComplete; }
    public static void setRefreshSongsComplete(boolean refreshSongsComplete) { Playlist.refreshSongsComplete = refreshSongsComplete; }
    public static double getRefreshSongsProgress() { return refreshSongsProgress; }
    public static void setRefreshSongsProgress(double refreshSongsProgress) { Playlist.refreshSongsProgress = refreshSongsProgress; }
    public static boolean isAccessingRefreshSongsProgress() { return isAccessingRefreshSongsProgress; }

    /** @noinspection StatementWithEmptyBody*/
    public static void refreshSongs() {
        Path directory = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
        if(lastRefreshThread != null) {
            stopLastRefresh = true;
            while(lastRefreshThread.isAlive()) { }
            stopLastRefresh = false;
        }
        lastRefreshThread = new Thread(() -> {
            ArrayList<Path> files = new ArrayList<>();
            try(DirectoryStream<Path> stream = PlaybackManager.getFilter()
                    ? Files.newDirectoryStream(directory, filePath -> {
                String filename = filePath.getFileName().toString(); return !filename.startsWith("_") && filename.endsWith(".mp3");
            })
                    : Files.newDirectoryStream(directory, "*.mp3")) {
                stream.forEach(files::add);
            } catch(IOException e) { throw new RuntimeException(e); }
            int i = 0;
            ArrayList<Song> songs = new ArrayList<>();
            for(Path filePath : files) {
                if(stopLastRefresh)
                    return;
                songs.add(new Song(filePath.toFile()));
                isAccessingRefreshSongsProgress = true;
                refreshSongsProgress = (double)i / files.size();
                isAccessingRefreshSongsProgress = false;
                i++;
            }
            Playlist.songs = songs;
            sortSongs();
            refreshSongsComplete = true;
        });
        lastRefreshThread.start();
    }

    public static void sortSongs() {
        songs.sort(new SongComparator());
    }

}
