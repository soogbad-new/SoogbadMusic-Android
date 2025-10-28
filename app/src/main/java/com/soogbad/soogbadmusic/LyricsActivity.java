package com.soogbad.soogbadmusic;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.textclassifier.TextClassificationManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LyricsActivity extends AppCompatActivity {

    private ConstraintLayout constraintLayout;
    private TextView songNameTextView, songInfoTextView, songLyricsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);
        setSupportActionBar(findViewById(R.id.lyricsToolbar));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        constraintLayout = findViewById(R.id.lyricsConstraintLayout); songNameTextView = findViewById(R.id.lyricsSongNameTextView); songInfoTextView = findViewById(R.id.lyricsSongInfoTextView); songLyricsTextView = findViewById(R.id.songLyrics);
        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        PlayerManager.addOnSongChangedListener(new EmptyListener() {
            @Override
            public void onListenerInvoked() {
                onSongChanged();
            }
        });
        findViewById(R.id.lyricsConstraintLayout).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
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
                    startActivity(new Intent().setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, selectedText).setType("text/plain").setPackage("com.google.android.apps.translate").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
        if (PlayerManager.getPlayer() != null) {
            SongData data = PlayerManager.getPlayer().getSong().getData();
            songNameTextView.setText(data.Artist + " - " + data.Title);
            songInfoTextView.setText(data.Album + " (" + data.Year + ")");
            songLyricsTextView.setText("\n" + data.Lyrics + "\n");
        }
        scrollToTop();
    }
    private void scrollToTop() {
        songLyricsTextView.setScrollY(0);
    }

}
