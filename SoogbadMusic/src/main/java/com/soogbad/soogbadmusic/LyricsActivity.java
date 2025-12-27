package com.soogbad.soogbadmusic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.text.MessageFormat;

public class LyricsActivity extends AppCompatActivity {

    private TextView songNameTextView, songInfoTextView, songLyricsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);
        setSupportActionBar(findViewById(R.id.lyricsToolbar));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        ConstraintLayout constraintLayout = findViewById(R.id.lyricsConstraintLayout);
        songNameTextView = findViewById(R.id.lyricsSongNameTextView); songInfoTextView = findViewById(R.id.lyricsSongInfoTextView); songLyricsTextView = findViewById(R.id.songLyrics);
        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        PlaybackManager.addOnSongChangedListener(this::onSongChanged);
        constraintLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onSongChanged();
                findViewById(R.id.lyricsConstraintLayout).getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        songLyricsTextView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(0, 1, 0, "Google Translate").setIcon(android.R.drawable.ic_menu_sort_by_size).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                return true;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if(item.getItemId() == 1) {
                    String selectedText = songLyricsTextView.getText().toString().substring(songLyricsTextView.getSelectionStart(), songLyricsTextView.getSelectionEnd());
                    startActivity(new Intent().setPackage("com.google.android.apps.translate").setAction(Intent.ACTION_PROCESS_TEXT).setType("text/plain").putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText).putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true));
                    mode.finish();
                    return true;
                }
                return false;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) { }
        });
    }

    private void onSongChanged() {
        if(PlaybackManager.getPlayer() != null) {
            SongData data = PlaybackManager.getPlayer().getSong().getData();
            songNameTextView.setText(MessageFormat.format("{0} - {1}", data.Artist, data.Title));
            songInfoTextView.setText(MessageFormat.format("{0} ({1})", data.Album, String.valueOf(data.Year)));
            songLyricsTextView.setText(MessageFormat.format("\n{0}\n", data.Lyrics));
        }
        scrollToTop();
    }
    private void scrollToTop() {
        songLyricsTextView.setScrollY(0);
    }

}
