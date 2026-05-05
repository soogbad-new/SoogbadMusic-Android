package com.soogbad.soogbadmusic;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

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
            int i = 0;
            HashMap<String, Song> previousSongs = new HashMap<>(songs.size() * 2);
            for(Song song : songs) previousSongs.put(song.getFile().getAbsolutePath(), song);
            ArrayList<Song> newSongs = new ArrayList<>(files.size());
            for(Path file : files) {
                if(stopLastRefresh)
                    return;
                String filePath = file.toFile().getAbsolutePath();
                if(previousSongs.containsKey(filePath))
                    newSongs.add(previousSongs.get(filePath));
                else
                    newSongs.add(new Song(file.toFile()));
                isAccessingRefreshSongsProgress = true;
                refreshSongsProgress = (double)i / files.size();
                isAccessingRefreshSongsProgress = false;
                i++;
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

    private static ArrayList<MediaBrowserCompat.MediaItem> mediaItems = null;
    private static Thread lastLoadMediaItemsThread = null;
    private static boolean loadMediaItemsComplete = false;
    private static boolean stopLastLoadMediaItems = false;

    public static ArrayList<MediaBrowserCompat.MediaItem> getMediaItems() { return mediaItems; }
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
            ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for(Song song : songs) {
                if(stopLastLoadMediaItems)
                    return;
                song.loadAlbumCoverAndLyrics();
                Bundle descriptionExtras = new Bundle();
                descriptionExtras.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getData().Title); descriptionExtras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getData().Artist); descriptionExtras.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getData().Album); descriptionExtras.putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.getData().Year); descriptionExtras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long)(song.getDuration() * 1000));
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder().setMediaId(song.getFile().getAbsolutePath()).setTitle(song.getData().Title).setSubtitle(song.getData().Artist).setDescription(song.getData().Album).setIconBitmap(song.getData().AlbumCover).setExtras(descriptionExtras).build();
                mediaItems.add(new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }
            Playlist.mediaItems = mediaItems;
            loadMediaItemsComplete = true;
        });
        lastLoadMediaItemsThread.start();
    }

    public static void reset() {
        songs = new ArrayList<>(); mediaItems = null; refreshSongsComplete = false; loadMediaItemsComplete = false; refreshSongsProgress = 0; isAccessingRefreshSongsProgress = false; stopLastRefresh = false;
    }

}
