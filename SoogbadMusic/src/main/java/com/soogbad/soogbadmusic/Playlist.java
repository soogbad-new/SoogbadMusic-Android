package com.soogbad.soogbadmusic;

import android.net.Uri;
import android.os.Environment;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MediaItem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Playlist {

    private static ArrayList<Song> songs = new ArrayList<>();
    private static Thread lastRefreshSongsThread = null;
    private static boolean refreshSongsComplete = false;
    private static double refreshSongsProgress = 0;
    private static boolean isAccessingRefreshSongsProgress = false;
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
        if(lastRefreshSongsThread != null) {
            stopLastRefresh = true;
            while(lastRefreshSongsThread.isAlive()) { }
            stopLastRefresh = false;
        }
        lastRefreshSongsThread = new Thread(() -> {
            ArrayList<Path> files = new ArrayList<>();
            try(DirectoryStream<Path> stream = PlaybackManager.getFilter()
                    ? Files.newDirectoryStream(directory, filePath -> {
                String filename = filePath.getFileName().toString(); return !filename.startsWith("_") && filename.endsWith(".mp3");
            })
                    : Files.newDirectoryStream(directory, "*.mp3")) {
                stream.forEach(files::add);
            } catch(IOException e) { throw new RuntimeException(e); }
            HashMap<String, Song> previousSongs = new HashMap<>(songs.size() * 2);
            for(Song song : songs) previousSongs.put(song.getFile().getAbsolutePath(), song);
            int newSongsTotal = files.size() - previousSongs.size();
            int newSongsProcessed = 0;
            ArrayList<Song> newSongs = new ArrayList<>(files.size());
            for(Path file : files) {
                if(stopLastRefresh) {
                    songs = newSongs;
                    return;
                }
                String filePath = file.toFile().getAbsolutePath();
                if(previousSongs.containsKey(filePath))
                    newSongs.add(previousSongs.get(filePath));
                else {
                    newSongs.add(new Song(file.toFile()));
                    isAccessingRefreshSongsProgress = true;
                    refreshSongsProgress = (double)newSongsProcessed / newSongsTotal;
                    isAccessingRefreshSongsProgress = false;
                    newSongsProcessed++;
                }
            }
            songs = newSongs;
            sortSongs();
            refreshSongsComplete = true;
        });
        lastRefreshSongsThread.start();
    }

    public static void sortSongs() {
        songs.sort(new SongComparator());
    }

    private static ArrayList<MediaItem> mediaItems = null;
    private static Thread lastLoadMediaItemsThread = null;
    private static boolean loadMediaItemsComplete = false;
    private static boolean stopLastLoadMediaItems = false;

    public static ArrayList<MediaItem> getMediaItems() { return mediaItems; }
    public static boolean getLoadMediaItemsComplete() { return loadMediaItemsComplete; }
    public static void setLoadMediaItemsComplete(boolean loadMediaItemsComplete) { Playlist.loadMediaItemsComplete = loadMediaItemsComplete; }

    /** @noinspection StatementWithEmptyBody*/
    public static void loadMediaItems() {
        if(lastLoadMediaItemsThread != null && lastLoadMediaItemsThread.isAlive())
            return;
        if(lastLoadMediaItemsThread != null) {
            stopLastLoadMediaItems = true;
            while(lastLoadMediaItemsThread.isAlive()) { }
            stopLastLoadMediaItems = false;
        }
        lastLoadMediaItemsThread = new Thread(() -> {
            while((songs.isEmpty() || (lastRefreshSongsThread != null && lastRefreshSongsThread.isAlive())) && !stopLastLoadMediaItems) { }
            if(stopLastLoadMediaItems) return;
            ArrayList<MediaItem> mediaItems = new ArrayList<>();
            for(Song song : songs) {
                if(stopLastLoadMediaItems)
                    return;
                song.loadAlbumCoverAndLyrics();
                MediaMetadata metadata = new MediaMetadata.Builder().setDisplayTitle(song.getData().Title).setSubtitle(song.getData().Artist).setDescription(song.getData().Album)
                        .setTitle(song.getData().Title).setArtist(song.getData().Artist).setAlbumTitle(song.getData().Album).setReleaseYear(song.getData().Year)
                        .setDurationMs(song.getDuration()).setIsPlayable(true).setIsBrowsable(false).build();
                MediaItem mediaItem = new MediaItem.Builder().setMediaId(song.getPath()).setUri(Uri.fromFile(song.getFile())).setMediaMetadata(metadata).build();
                song.setMediaItem(mediaItem);
                mediaItems.add(mediaItem);
            }
            Playlist.mediaItems = mediaItems;
            loadMediaItemsComplete = true;
            System.out.println("AAA loadMediaItems complete");
        });
        lastLoadMediaItemsThread.start();
    }

    public static void reset() {
        songs = new ArrayList<>(); mediaItems = null; refreshSongsComplete = false; loadMediaItemsComplete = false; refreshSongsProgress = 0; isAccessingRefreshSongsProgress = false; stopLastRefresh = false;
    }

}
