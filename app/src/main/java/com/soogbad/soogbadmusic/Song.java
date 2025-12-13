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

    public Song(File file) {
        this.file = file;
        refreshData();
    }

    public void refreshData() {
        if(!file.exists())
            return;
        try(MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(getPath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = durationStr != null ? Integer.parseInt(durationStr) / 1000.0 : 0;
            int year = 0;
            String yearStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            if(yearStr != null)
                year = Integer.parseInt(yearStr);
            data = new SongData(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST), retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), year, null, "");
        }
        catch(IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void loadAlbumCoverAndLyrics() {
        try {
            AudioFile file = AudioFileIO.read(getFile());
            if(file == null)
                return;
            Tag tag = file.getTag();
            Artwork artwork = tag.getFirstArtwork();
            data.AlbumCover = artwork != null ? BitmapFactory.decodeByteArray(artwork.getBinaryData(), 0, artwork.getBinaryData().length) : null;
            data.Lyrics = tag.getFirst(FieldKey.LYRICS);
        }
        catch(Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
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

}
