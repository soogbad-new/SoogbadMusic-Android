package com.soogbad.soogbadmusic;

import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;

public final class Song {

    private final File file;
    private double duration;
    private SongData data;
    private boolean refreshedAlbumCoverAndLyrics = false;

    public Song(File file) {
        this.file = file;
        refresh();
    }

    public void refresh() {
        if(!file.exists())
            return;
        try(MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(getPath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = durationStr != null ? Integer.parseInt(durationStr) / 1000.0 : 0;
            String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            data = new SongData(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST), retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), year != null ? Integer.parseInt(year) : 0, null, "");
        }
        catch(IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void refreshAlbumCoverAndLyrics(boolean refreshSongList) {
        AudioFile file = null;
        try {
            file = AudioFileIO.read(getFile());
        }
        catch(Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        if(file == null)
            return;
        Tag tag = file.getTag();
        Artwork artwork = tag.getFirstArtwork();
        data.AlbumCover = artwork != null ? BitmapFactory.decodeByteArray(artwork.getBinaryData(), 0, artwork.getBinaryData().length) : null;
        data.Lyrics = tag.getFirst(FieldKey.LYRICS);
        refreshedAlbumCoverAndLyrics = true;
        if(PlaybackManager.getPlayer() != null && PlaybackManager.getPlayer().getSong() == this && (data.AlbumCover != null || !data.Lyrics.isEmpty()) && refreshSongList)
            PlaybackManager.raiseOnSongChanged();
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public double getDuration() {
        return duration;
    }

    public SongData getData() {
        return data;
    }

    public boolean hasNotRefreshedAlbumCoverAndLyrics() {
        return !refreshedAlbumCoverAndLyrics;
    }

}
