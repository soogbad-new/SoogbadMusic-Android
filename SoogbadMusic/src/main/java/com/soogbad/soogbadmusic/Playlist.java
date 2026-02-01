package com.soogbad.soogbadmusic;

import android.os.Environment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        lastRefreshSongsThread.start();
    }

    public static void sortSongs() {
        songs.sort(new SongComparator());
    }

    private static ArrayList<MediaBrowserCompat.MediaItem> mediaItems = null;
    private static Thread lastLoadMediaItemsThread = null;
    private static boolean loadMediaItemsComplete = false;

    public static ArrayList<MediaBrowserCompat.MediaItem> getMediaItems() { return mediaItems; }
    public static boolean getLoadMediaItemsComplete() { return loadMediaItemsComplete; }
    public static void setLoadMediaItemsComplete(boolean loadMediaItemsComplete) { Playlist.loadMediaItemsComplete = loadMediaItemsComplete; }

    /** @noinspection StatementWithEmptyBody*/
    public static void loadMediaItems() {
        lastLoadMediaItemsThread = new Thread(() -> {
            while(songs.isEmpty() || lastRefreshSongsThread.isAlive() || lastLoadMediaItemsThread.isAlive()) { }
            ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for(Song song : Playlist.songs) {
                MediaMetadataCompat metadata = new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.getFile().getAbsolutePath()).putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getData().Title).putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getData().Artist).putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getData().Album).putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.getData().Year).putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long)(song.getDuration() * 1000)).putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, song.getData().AlbumCover).build();
                mediaItems.add(new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
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
