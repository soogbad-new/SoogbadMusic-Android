package com.soogbad.soogbadmusic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout constraintLayout;
    private ImageButton shuffleButton, filterButton, playPauseButton, previousButton, nextButton, advancedSearchButton;
    private TextView songNameTextView, songInfoTextView, currentTimeTextView, durationTextView;
    private ImageView albumCoverImageView;
    private View progressBarBackground, progressBar;
    private SongList songList;
    private EditText searchEditText;

    private Drawable defaultSearchbarBackground;
    private int defaultSearchbarHeight = 0, previousCallState = TelephonyManager.CALL_STATE_IDLE;
    private boolean advancedSearch = false, startedLyrics = false, wasPausedBeforeCall = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowProperties();
        constraintLayout = findViewById(R.id.constraintLayout); shuffleButton = findViewById(R.id.shuffleButton); filterButton = findViewById(R.id.filterButton); playPauseButton = findViewById(R.id.playPauseButton); previousButton = findViewById(R.id.previousButton); nextButton = findViewById(R.id.nextButton); songNameTextView = findViewById(R.id.songNameTextView); songInfoTextView = findViewById(R.id.songInfoTextView); currentTimeTextView = findViewById(R.id.currentTimeTextView); durationTextView = findViewById(R.id.durationTextView); albumCoverImageView = findViewById(R.id.albumCoverImageView); progressBarBackground = findViewById(R.id.progressBarBackground); progressBar = findViewById(R.id.progressBar); searchEditText = findViewById(R.id.searchEditText); advancedSearchButton = findViewById(R.id.advancedSearchButton); songList = findViewById(R.id.songList);
        playPauseButton.setEnabled(false); previousButton.setEnabled(false); nextButton.setEnabled(false); searchEditText.setEnabled(false);
        progressBarBackground.setOnTouchListener(onProgressBarTouchListener); progressBar.setOnTouchListener(onProgressBarTouchListener); progressBarBackground.setOnClickListener(onProgressBarClickListener); progressBar.setOnClickListener(onProgressBarClickListener);
        defaultSearchbarBackground = searchEditText.getBackground();
        searchEditText.setOnTouchListener(this::onSearchEditTextTouch);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable editable) { onSearchEditTextTextChanged(editable.toString()); }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { }
        });
        checkPermissions();
    }
    private void setWindowProperties() {
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.constraintLayout), this::onApplyWindowInsetsListener);
    }

    private void onPermissionsGranted() {
        PlaybackManager.initMediaSessionNotification(this);
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).registerTelephonyCallback(getMainExecutor(), telephonyCallback);
        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) { super.onAudioDevicesAdded(addedDevices); PlaybackManager.setPaused(true); }
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) { super.onAudioDevicesRemoved(removedDevices); PlaybackManager.setPaused(true); }
        };
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, new Handler(Looper.getMainLooper()));
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickImageResult);
        Playlist.setDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        Playlist.refreshSongs();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() { new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> onTimerTick()); }
        }, 0, 10);
        PlaybackManager.addOnSongChangedListener(this::onSongChanged);
        PlaybackManager.addOnPausedValueChangedListener(this::onPausedValueChanged);
    }

    private void onTimerTick() {
        updateProgressBar();
        if(Playlist.getRefreshSongsComplete())
            onRefreshSongsComplete();
    }
    private void updateProgressBar() {
        if(Playlist.isAccessingRefreshSongsProgress())
            return;
        double progress = Playlist.getRefreshSongsProgress();
        if(progress > 0) {
            progressBar.setVisibility(View.VISIBLE);
            currentTimeTextView.setText("");
            durationTextView.setText("");
            if(progressBar.getWidth() <= progressBarBackground.getWidth())
                progressBar.getLayoutParams().width = (int)Math.round(progress * progressBarBackground.getWidth());
        }
        else if(PlaybackManager.getPlayer() != null) {
            progressBar.setVisibility(View.VISIBLE);
            if(progressBar.getWidth() <= progressBarBackground.getWidth()) {
                int width = (int)Math.round(PlaybackManager.getPlayer().getCurrentTime() / PlaybackManager.getPlayer().getSong().getDuration() * progressBarBackground.getWidth());
                progressBar.getLayoutParams().width = width == 0 ? 1 : width;
                currentTimeTextView.setText(Utility.formatTime(PlaybackManager.getPlayer().getCurrentTime()));
                durationTextView.setText(Utility.formatTime(PlaybackManager.getPlayer().getSong().getDuration()));
            }
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            currentTimeTextView.setText("");
            durationTextView.setText("");
        }
    }
    private void onRefreshSongsComplete() {
        Playlist.setRefreshSongsComplete(false);
        Playlist.setRefreshSongsProgress(0);
        songList.changeSongList(Playlist.getSongs(), false);
        playPauseButton.setEnabled(true); previousButton.setEnabled(true); nextButton.setEnabled(true); searchEditText.setEnabled(true);
        searchEditTextClearFocus();
        searchEditText.setTypeface(null, Typeface.ITALIC);
        searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder, getTheme()));
        searchEditText.setText(MessageFormat.format("Search {0} Songs", Playlist.getSongs().size()));
        defaultSearchbarHeight = searchEditText.getHeight();
    }

    private void onSongChanged() {
        if(PlaybackManager.getPlayer() == null)
            return;
        SongData data = PlaybackManager.getPlayer().getSong().getData();
        searchEditTextClearFocus();
        searchEditText.setTypeface(null, Typeface.ITALIC);
        searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder, getTheme()));
        searchEditText.setText(MessageFormat.format("Search {0} Songs", Playlist.getSongs().size()));
        songList.changeSongList(Playlist.getSongs(), false);
        int firstPosition = songList.findFirstCompletelyVisibleItemPosition();
        int lastPosition = songList.findLastCompletelyVisibleItemPosition();
        progressBar.getLayoutParams().width = 1;
        int index = Playlist.getSongs().indexOf(PlaybackManager.getPlayer().getSong());
        if(!(index >= firstPosition && index <= lastPosition))
            songList.scrollToPosition(index);
        else
            songList.scrollToPosition(firstPosition);
        songNameTextView.setText(MessageFormat.format("{0} - {1}", data.Artist, data.Title));
        songInfoTextView.setText(MessageFormat.format("{0} ({1})", data.Album, String.valueOf(data.Year)));
        albumCoverImageView.setImageBitmap(data.AlbumCover);
    }

    private float progressBarTouchLocation = -1;
    private final View.OnTouchListener onProgressBarTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                progressBarTouchLocation = motionEvent.getX();
            return false;
        }
    };
    private final View.OnClickListener onProgressBarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            searchEditTextClearFocus();
            if(PlaybackManager.getPlayer() != null && !Playlist.isAccessingRefreshSongsProgress() && Playlist.getRefreshSongsProgress() == 0 && progressBarTouchLocation != -1) {
                double time = (double) progressBarTouchLocation / progressBarBackground.getWidth() * PlaybackManager.getPlayer().getSong().getDuration();
                progressBarTouchLocation = -1;
                if(time > PlaybackManager.getPlayer().getSong().getDuration() - 1)
                    time = PlaybackManager.getPlayer().getSong().getDuration() - 1;
                if(time < 0)
                    time = 0;
                if(PlaybackManager.getPlayer().getStopped())
                    PlaybackManager.switchSong(PlaybackManager.getPlayer().getSong());
                PlaybackManager.setCurrentTime(time);
            }
        }
    };

    public void onLyricsButtonClick(View view) {
        if(!startedLyrics) {
            startedLyrics = true;
            searchEditTextClearFocus();
            startActivity(new Intent(this, LyricsActivity.class));
        }
    }
    public void onShuffleButtonClick(View view) {
        searchEditTextClearFocus();
        PlaybackManager.setShuffle(!PlaybackManager.getShuffle());
        if(PlaybackManager.getShuffle())
            shuffleButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shuffle_on));
        else
            shuffleButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shuffle_off));
    }
    public void onFilterButtonClick(View view) {
        searchEditTextClearFocus();
        PlaybackManager.setFilter(!PlaybackManager.getFilter());
        if(PlaybackManager.getFilter())
            filterButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.filter_on));
        else
            filterButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.filter_off));
        Playlist.refreshSongs();
    }

    public void onPlayPauseButtonClick(View view) {
        searchEditTextClearFocus();
        PlaybackManager.setPaused(!PlaybackManager.getPaused());
    }

    private void onPausedValueChanged() {
        playPauseButton.setImageResource(PlaybackManager.getPaused() ? R.drawable.play : R.drawable.pause);
        if(PlaybackManager.getPaused() && !PlaybackManager.getShuffle() && progressBar.getWidth() >= progressBarBackground.getWidth()) {
            progressBar.getLayoutParams().width = progressBarBackground.getWidth();
            currentTimeTextView.setText(Utility.formatTime(PlaybackManager.getPlayer().getSong().getDuration()));
        }
    }
    public void onNextButtonClick(View view) {
        searchEditTextClearFocus();
        PlaybackManager.nextSong();
    }
    public void onPreviousButtonClick(View view) {
        searchEditTextClearFocus();
        PlaybackManager.previousSong();
    }

    public void searchEditTextClearFocus() {
        if(searchEditText.getText().toString().isEmpty()) {
            searchEditText.setTypeface(null, Typeface.ITALIC);
            searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder, getTheme()));
            searchEditText.setText(MessageFormat.format("Search {0} Songs", Playlist.getSongs().size()));
        }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        searchEditText.clearFocus();
    }
    @SuppressWarnings("SameReturnValue")
    private boolean onSearchEditTextTouch(View ignoredView, MotionEvent ignoredMotionEvent) {
        searchEditText.requestFocus();
        return false;
    }
    public void onSearchEditTextClick(View view) {
        if(searchEditText.getCurrentTextColor() == getResources().getColor(R.color.searchbarPlaceholder, getTheme())) {
            searchEditText.setTypeface(null, Typeface.NORMAL);
            searchEditText.setTextColor(getResources().getColor(R.color.white, getTheme()));
            searchEditText.setText("");
        }
    }
    public void onSearchEditTextTextChanged(String text) {
        if(text.isEmpty() || searchEditText.getCurrentTextColor() == getResources().getColor(R.color.searchbarPlaceholder, getTheme())) {
            if(songList.getAdapter() != null && songList.getAdapter().getItemCount() < Playlist.getSongs().size()) {
                songList.changeSongList(Playlist.getSongs(), false);
                int firstPosition = songList.findFirstCompletelyVisibleItemPosition();
                int lastPosition = songList.findLastCompletelyVisibleItemPosition();
                if(PlaybackManager.getPlayer() != null) {
                    int index = Playlist.getSongs().indexOf(PlaybackManager.getPlayer().getSong());
                    if(!(index >= firstPosition && index <= lastPosition))
                        songList.scrollToPosition(index);
                }
            }
        } else {
            ArrayList<Song> songs = new ArrayList<>();
            for(Song song : Playlist.getSongs())
                if(song.getData().contains(text, advancedSearch))
                    songs.add(song);
            songList.changeSongList(songs, false);
        }
    }
    public void onAdvancedSearchButtonClick(View view) {
        searchEditTextClearFocus();
        advancedSearch = !advancedSearch;
        if(advancedSearch)
            advancedSearchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.advanced_search_on));
        else
            advancedSearchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.advanced_search_off));
        onSearchEditTextTextChanged(searchEditText.getText().toString());
    }
    public void onSearchEditTextLoseFocus(View view) {
        searchEditTextClearFocus();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        searchEditTextClearFocus();
        super.onCreateContextMenu(menu, view, menuInfo);
        SongListAdapter songListAdapter = ((SongListAdapter)songList.getAdapter());
        if(songListAdapter == null)
            return;
        Song song = songListAdapter.getSong(view);
        if(song == null)
            return;
        menu.setHeaderTitle(song.getData().Artist + " - " + song.getData().Title);
        menu.add(0, view.getId(), 0, "Add To Queue").setActionView(view);
        if(PlaybackManager.queueContains(song))
            menu.add(0, view.getId(), 0, "Remove From Queue").setActionView(view);
        menu.add(0, view.getId(), 0, "Song Info").setActionView(view).setEnabled(false);
    }
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem menuItem) {
        SongListAdapter songListAdapter = ((SongListAdapter)songList.getAdapter());
        if(songListAdapter == null)
            return false;
        Song song = songListAdapter.getSong(menuItem.getActionView());
        if(menuItem.getTitle() == "Add To Queue")
            PlaybackManager.addToQueue(song);
        else if(menuItem.getTitle() == "Remove From Queue")
            PlaybackManager.removeFromQueue(song);
        else if(menuItem.getTitle() == "Song Info")
            new SongInfoDialog(this, song);
        else
            return false;
        return true;
    }

    private WindowInsetsCompat onApplyWindowInsetsListener(View view, WindowInsetsCompat insets) {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        findViewById(R.id.toolbar).setPadding(0, systemBars.top, 0, 0);
        Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
        view.setPadding(0, 0, 0, 2 * Math.max(systemBars.bottom, keyboard.bottom) - systemBars.bottom);
        if(insets.isVisible(WindowInsetsCompat.Type.ime())) {
            MaterialShapeDrawable searchbarDrawable = new MaterialShapeDrawable(new ShapeAppearanceModel().toBuilder().setAllCorners(CornerFamily.ROUNDED, view.getWidth() / 2f).build());
            searchbarDrawable.setFillColor(ContextCompat.getColorStateList(this, R.color.semiTransparentSearchbar));
            searchbarDrawable.setStroke(5.0f, ContextCompat.getColor(this, R.color.semiTransparentSelected));
            searchEditText.setBackground(searchbarDrawable);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.searchEditText, ConstraintSet.RIGHT, R.id.constraintLayout, ConstraintSet.RIGHT);
            constraintSet.applyTo(constraintLayout);
            if(defaultSearchbarHeight != 0)
                searchEditText.setHeight(Math.round(0.9f * defaultSearchbarHeight));
            searchEditText.setPadding(Utility.dpToPixels(10, getResources().getDisplayMetrics()), searchEditText.getPaddingTop(), Utility.dpToPixels(10, getResources().getDisplayMetrics()), searchEditText.getPaddingBottom());
            songList.addInvisibleSongsToMakeUpForCoveredArea(keyboard.bottom - (constraintLayout.getBottom() - songList.getBottom()));
        }
        else {
            searchEditTextClearFocus();
            searchEditText.setBackground(defaultSearchbarBackground);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.searchEditText, ConstraintSet.RIGHT, R.id.advancedSearchButton, ConstraintSet.LEFT);
            constraintSet.applyTo(constraintLayout);
            if(defaultSearchbarHeight != 0)
                searchEditText.setHeight(defaultSearchbarHeight);
            searchEditText.setPadding(10, searchEditText.getPaddingTop(), 10, searchEditText.getPaddingBottom());
            songList.removeAllInvisibleSongs();
        }
        return insets;
    }

    private final MyTelephonyCallback telephonyCallback = new MyTelephonyCallback();
    private class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            if(state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                if(previousCallState == TelephonyManager.CALL_STATE_IDLE) {
                    wasPausedBeforeCall = PlaybackManager.getPaused();
                    PlaybackManager.setPaused(true);
                }
                previousCallState = state;
            } else if(state == TelephonyManager.CALL_STATE_IDLE && !wasPausedBeforeCall) {
                previousCallState = state;
                new Handler(Looper.getMainLooper()).postDelayed(() -> PlaybackManager.setPaused(false), 2500);
            }
        }
    }

    private SongInfoDialog caller = null;
    ActivityResultLauncher<String> pickImageLauncher = null;
    public void pickImageDialog(SongInfoDialog caller) {
        this.caller = caller;
        if(pickImageLauncher != null)
            pickImageLauncher.launch("image/*");
    }
    private void handlePickImageResult(Uri uri) {
        if(uri != null && caller != null)
            caller.setPickImageResult(uri);
    }

    @SuppressLint("BatteryLife")
    private void checkPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        if(!permissions.isEmpty()) {
            String[] arr = new String[permissions.size()];
            arr = permissions.toArray(arr);
            ActivityCompat.requestPermissions(MainActivity.this, arr, 0);
        }
        else {
            if(!((PowerManager)getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName()))
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName())));
            onPermissionsGranted();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0 && grantResults.length > 0) {
            for(int result : grantResults)
                if(result != PackageManager.PERMISSION_GRANTED)
                    return;
            onPermissionsGranted();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startedLyrics = false;
        searchEditTextClearFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(PlaybackManager.getMediaSession() != null)
            PlaybackManager.getMediaSession().release();
        if(PlaybackManager.getPlayer() != null)
            PlaybackManager.getPlayer().release();
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).unregisterTelephonyCallback(telephonyCallback);
    }

    public SongList getSongList() {
        return songList;
    }

}
