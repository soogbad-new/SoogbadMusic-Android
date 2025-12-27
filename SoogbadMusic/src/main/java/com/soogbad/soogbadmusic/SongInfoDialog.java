package com.soogbad.soogbadmusic;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;

import java.io.ByteArrayOutputStream;

/** @noinspection CallToPrintStackTrace */
public class SongInfoDialog {

    private EditText titleEditText, artistEditText, albumEditText, yearEditText, lyricsEditText;
    private ImageView albumCoverImageButton;

    public SongInfoDialog(final MainActivity mainActivity, final Song song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setCancelable(false);
        builder.setTitle("Song Info - " + song.getPath());
        builder.setView(LayoutInflater.from(mainActivity).inflate(R.layout.song_info_dialog, mainActivity.findViewById(R.id.constraintLayout)));
        builder.setPositiveButton("Save", (dialogInterface, i) -> {
            boolean currentlyPlaying = PlaybackManager.getPlayer() != null && PlaybackManager.getPlayer().getSong() == song;
            AudioFile file = null;
            try {
                file = AudioFileIO.read(song.getFile());
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            if(file == null)
                return;
            Tag tag = file.getTag();
            try {
                tag.setField(FieldKey.TITLE, titleEditText.getText().toString());
            }
            catch(FieldDataInvalidException e) {
                e.printStackTrace();
            }
            String artist = artistEditText.getText().toString();
            try {
                tag.setField(FieldKey.ALBUM_ARTIST, artist);
                tag.setField(FieldKey.PERFORMER, artist);
            }
            catch(FieldDataInvalidException e) {
                e.printStackTrace();
            }
            try {
                tag.setField(FieldKey.ALBUM, albumEditText.getText().toString());
            }
            catch(FieldDataInvalidException e) {
                e.printStackTrace();
            }
            try {
                tag.setField(FieldKey.YEAR, yearEditText.getText().toString().isEmpty() ? "0" : yearEditText.getText().toString());
            }
            catch(FieldDataInvalidException e) {
                e.printStackTrace();
            }
            tag.deleteArtworkField();
            Bitmap bitmap = Utility.getBitmap(albumCoverImageButton.getDrawable());
            if(bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                AndroidArtwork artwork = new AndroidArtwork();
                artwork.setBinaryData(stream.toByteArray());
                try {
                    tag.setField(artwork);
                }
                catch(FieldDataInvalidException e) {
                    e.printStackTrace();
                }
                bitmap.recycle();
            }
            try {
                tag.setField(FieldKey.LYRICS, lyricsEditText.getText().toString().replaceAll("\n", "\r\n"));
            }
            catch(FieldDataInvalidException e) {
                e.printStackTrace();
            }
            try {
                file.commit();
            }
            catch(CannotWriteException e) {
                e.printStackTrace();
            }
            Playlist.getSongs().get(Playlist.getSongs().indexOf(song)).refreshData();
            Playlist.sortSongs();
            SongList songList = mainActivity.getSongList();
            songList.changeSongList(Playlist.getSongs(), false);
            songList.scrollToPosition(songList.findFirstCompletelyVisibleItemPosition());
            if(currentlyPlaying)
                PlaybackManager.raiseOnSongChanged();
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.show();
        titleEditText = dialog.findViewById(R.id.titleEditText); artistEditText = dialog.findViewById(R.id.artistEditText); albumEditText = dialog.findViewById(R.id.albumEditText); yearEditText = dialog.findViewById(R.id.yearEditText); albumCoverImageButton = dialog.findViewById(R.id.albumCoverImageButton); lyricsEditText = dialog.findViewById(R.id.lyricsEditText);
        titleEditText.setText(song.getData().Title);
        artistEditText.setText(song.getData().Artist);
        albumEditText.setText(song.getData().Album);
        yearEditText.setText(String.valueOf(song.getData().Year));
        yearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                int length = charSequence.length() - count + after;
                if(length > 4)
                    yearEditText.setText(yearEditText.getText());
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        albumCoverImageButton.setImageBitmap(song.getData().AlbumCover);
        albumCoverImageButton.setOnClickListener(view -> mainActivity.pickImageDialog(SongInfoDialog.this));
        lyricsEditText.setText(song.getData().Lyrics.replaceAll("\r", ""));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SongData data = song.getData();
                Bitmap bitmap = Utility.getBitmap(albumCoverImageButton.getDrawable());
                if(titleEditText.getText().toString().equals(data.Title) && artistEditText.getText().toString().equals(data.Artist) && albumEditText.getText().toString().equals(data.Album) && yearEditText.getText().toString().equals(String.valueOf(data.Year)) && ((bitmap == null && data.AlbumCover == null) || (bitmap != null && bitmap.sameAs(data.AlbumCover))) && lyricsEditText.getText().toString().equals(data.Lyrics))
                    dialog.dismiss();
                else {
                    getBuilder().show();
                }
            }
            private AlertDialog.Builder getBuilder() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                builder.setCancelable(false);
                builder.setTitle("Exit Dialog");
                builder.setMessage("Are you sure you want to exit?");
                builder.setPositiveButton("Exit", (dialogInterface, i) -> dialog.dismiss());
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> { });
                return builder;
            }
        });
    }

    public void setPickImageResult(Uri path) {
        albumCoverImageButton.setImageURI(path);
    }

}
