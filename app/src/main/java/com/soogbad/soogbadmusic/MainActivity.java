package com.soogbad.soogbadmusic;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout constraintLayout;
    private ImageButton lyricsButton, shuffleButton, filterButton, playPauseButton, previousButton, nextButton;
    private TextView songNameTextView, songInfoTextView, currentTimeTextView, durationTextView;
    private ImageView albumCoverImageView;
    private View progressBarBackground, progressBar;
    private EditText searchEditText;
    private ImageButton advancedSearchButton;
    private SongList songList;

    private MediaSession mediaSession;
    private NotificationManager notificationManager;
    private PhoneStateListener phoneStateListener;
    private boolean wasPausedBeforeCall = true;
    private int previousCallState = TelephonyManager.CALL_STATE_IDLE;
    private Drawable defaultSearchbarBackground;
    private int defaultSearchbarHeight = 0;

    private boolean advancedSearch = false;
    private boolean startedLyrics = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        constraintLayout = findViewById(R.id.constraintLayout); lyricsButton = findViewById(R.id.lyricsButton); shuffleButton = findViewById(R.id.shuffleButton); filterButton = findViewById(R.id.filterButton); playPauseButton = findViewById(R.id.playPauseButton); previousButton = findViewById(R.id.previousButton); nextButton = findViewById(R.id.nextButton); songNameTextView = findViewById(R.id.songNameTextView); songInfoTextView = findViewById(R.id.songInfoTextView); currentTimeTextView = findViewById(R.id.currentTimeTextView); durationTextView = findViewById(R.id.durationTextView); albumCoverImageView = findViewById(R.id.albumCoverImageView); progressBarBackground = findViewById(R.id.progressBarBackground); progressBar = findViewById(R.id.progressBar); searchEditText = findViewById(R.id.searchEditText); advancedSearchButton = findViewById(R.id.advancedSearchButton); songList = findViewById(R.id.songList);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        defaultSearchbarBackground = searchEditText.getBackground();
        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.toolbar).setPadding(0, systemBars.top, 0, 0);
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(0, 0, 0, 2 * Math.max(systemBars.bottom, keyboard.bottom) - systemBars.bottom);
            if(insets.isVisible(WindowInsetsCompat.Type.ime())) {
                MaterialShapeDrawable searchbarDrawable = new MaterialShapeDrawable(new ShapeAppearanceModel().toBuilder().setAllCorners(CornerFamily.ROUNDED, view.getWidth() / 2f).build());
                searchbarDrawable.setFillColor(ContextCompat.getColorStateList(this, R.color.semiTransparentSearchbar));
                searchbarDrawable.setStroke(5.0f, ContextCompat.getColor(this,R.color.semiTransparentSelected));
                ViewCompat.setBackground(searchEditText, searchbarDrawable);
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.searchEditText, ConstraintSet.RIGHT, R.id.constraintLayout, ConstraintSet.RIGHT);
                constraintSet.applyTo(constraintLayout);
                if(defaultSearchbarHeight != 0)
                    searchEditText.setHeight(Math.round(0.9f * defaultSearchbarHeight));
                searchEditText.setPadding(MyApplication.Utility.dpToPixels(10, getResources().getDisplayMetrics()), searchEditText.getPaddingTop(), MyApplication.Utility.dpToPixels(10, getResources().getDisplayMetrics()), searchEditText.getPaddingBottom());
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
        });
        playPauseButton.setEnabled(false); previousButton.setEnabled(false); nextButton.setEnabled(false); searchEditText.setEnabled(false);
        ArrayList<String> permissions = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        }
        else if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissions.size() > 0) {
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
    protected void onDestroy() {
        super.onDestroy();
        if(mediaSession != null)
            mediaSession.release();
        if(PlayerManager.getPlayer() != null)
            PlayerManager.getPlayer().release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0 && grantResults.length > 0) {
            boolean ok = true;
            for(int result : grantResults)
                if(result != PackageManager.PERMISSION_GRANTED)
                    ok = false;
            if(ok)
                    onPermissionsGranted();
        }
    }
    private void onPermissionsGranted() {
        notificationManager = getSystemService(NotificationManager.class);
        mediaSession = new MediaSession(this, "SoogbadMusic");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                PlayerManager.setPaused(false);
            }
            @Override
            public void onPause() {
                PlayerManager.setPaused(true);
            }
            @Override
            public void onSkipToNext() {
                PlayerManager.nextSong();
            }
            @Override
            public void onSkipToPrevious() {
                PlayerManager.previousSong();
            }
            @Override
            public void onStop() { notificationManager.cancel(6969); finishAndRemoveTask(); }
        });
        mediaSession.setActive(true);
        PlayerManager.setMediaSession(mediaSession);
        new MediaController(MainActivity.this, mediaSession.getSessionToken()).registerCallback(new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                super.onPlaybackStateChanged(state);
            }
            @Override
            public void onMetadataChanged(@Nullable MediaMetadata metadata) {
                super.onMetadataChanged(metadata);
            }
        });
        mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlaybackState.STATE_NONE, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0).build());
        notificationManager.createNotificationChannel(new NotificationChannel("soogbadmusic", "SoogbadMusic", NotificationManager.IMPORTANCE_DEFAULT));
        Playlist.setDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        Playlist.refreshSongs();
        progressBarBackground.setOnTouchListener(onProgressBarTouchListener);
        progressBar.setOnTouchListener(onProgressBarTouchListener);
        PlayerManager.addOnPausedValueChangedListener(new EmptyListener() {
            @Override
            public void onListenerInvoked() {
                playPauseButton.setImageResource(PlayerManager.getPaused() ? R.drawable.play : R.drawable.pause);
                if(PlayerManager.getPaused() && !PlayerManager.getShuffle() && progressBar.getWidth() >= progressBarBackground.getWidth()) {
                    progressBar.getLayoutParams().width = progressBarBackground.getWidth();
                    currentTimeTextView.setText(MyApplication.Utility.formatTime(PlayerManager.getPlayer().getSong().getDuration()));
                }
            }
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if(text.equals("") || searchEditText.getCurrentTextColor() == getResources().getColor(R.color.searchbarPlaceholder)) {
                    if(songList.getAdapter() != null && songList.getAdapter().getItemCount() < Playlist.getSongs().size()) {
                        songList.changeSongList(Playlist.getSongs(), false);
                        int firstPosition = songList.findFirstCompletelyVisibleItemPosition();
                        int lastPosition = songList.findLastCompletelyVisibleItemPosition();
                        if(PlayerManager.getPlayer() != null) {
                            int index = Playlist.getSongs().indexOf(PlayerManager.getPlayer().getSong());
                            if(!(index >= firstPosition && index <= lastPosition))
                                songList.scrollToPosition(index);
                        }
                    }
                }
                else {
                    ArrayList<Song> songs = new ArrayList<>();
                    for (Song song : Playlist.getSongs())
                        if (song.getData().contains(text, advancedSearch))
                            songs.add(song);
                    songList.changeSongList(songs, false);
                }
            }
        });
        PlayerManager.addOnSongChangedListener(new EmptyListener() {
            @Override
            public void onListenerInvoked() {
                Song song = PlayerManager.getPlayer().getSong();
                SongData data = song.getData();
                searchEditTextClearFocus();
                searchEditText.setTypeface(null, Typeface.ITALIC);
                searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder));
                searchEditText.setText("Search " + Playlist.getSongs().size() + " Songs");
                songList.changeSongList(Playlist.getSongs(), false);
                int firstPosition = songList.findFirstCompletelyVisibleItemPosition();
                int lastPosition = songList.findLastCompletelyVisibleItemPosition();
                progressBar.getLayoutParams().width = 1;
                int index = Playlist.getSongs().indexOf(song);
                if(!(index >= firstPosition && index <= lastPosition))
                    songList.scrollToPosition(index);
                else
                    songList.scrollToPosition(firstPosition);
                songNameTextView.setText(data.Artist + " - " + data.Title);
                songInfoTextView.setText(data.Album + " (" + data.Year + ")");
                albumCoverImageView.setImageBitmap(data.AlbumCover);
                mediaSession.setMetadata(new MediaMetadata.Builder().putLong(MediaMetadata.METADATA_KEY_DURATION, (long)(song.getDuration() * 1000)).putString(MediaMetadata.METADATA_KEY_TITLE, data.Title).putString(MediaMetadata.METADATA_KEY_ARTIST, data.Artist).putString(MediaMetadata.METADATA_KEY_ALBUM, data.Album).putLong(MediaMetadata.METADATA_KEY_YEAR, data.Year).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, data.AlbumCover).build());
                mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlayerManager.getPaused() ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING, (long)(1000 * PlayerManager.getPlayer().getCurrentTime()), PlayerManager.getPaused() ? 0 : 1).build());
                Intent prevActionIntent = new Intent(getApplicationContext(), MediaService.class).setAction("com.app.soogbadmusic.ACTION_PREV");
                Intent nextActionIntent = new Intent(getApplicationContext(), MediaService.class).setAction("com.app.soogbadmusic.ACTION_NEXT");
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "soogbadmusic")
                        .setSmallIcon(R.drawable.ic_launcher).setLargeIcon(data.AlbumCover)
                        .setContentTitle(data.Artist + " - " + data.Title).setContentText(data.Album + " (" + data.Year + ")")
                        .setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(true)
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                        .addAction(new NotificationCompat.Action(R.drawable.previous, "Previous", PendingIntent.getService(getApplicationContext(), 0, prevActionIntent, PendingIntent.FLAG_IMMUTABLE)))
                        .addAction(new NotificationCompat.Action(R.drawable.next, "Next", PendingIntent.getService(getApplicationContext(), 0, nextActionIntent, PendingIntent.FLAG_IMMUTABLE)));
                NotificationManagerCompat.from(getApplicationContext()).notify(6969, builder.build());
            }
        });
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
                    updateProgressBar();
                    if(Playlist.getRefreshSongsComplete()) {
                        Playlist.setRefreshSongsComplete(false);
                        Playlist.setRefreshSongsProgress(0);
                        songList.changeSongList(Playlist.getSongs(), false);
                        playPauseButton.setEnabled(true); previousButton.setEnabled(true); nextButton.setEnabled(true); searchEditText.setEnabled(true);
                        searchEditTextClearFocus();
                        searchEditText.setTypeface(null, Typeface.ITALIC);
                        searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder));
                        searchEditText.setText("Search " + Playlist.getSongs().size() + " Songs");
                        defaultSearchbarHeight = searchEditText.getHeight();
                    }
                });
            }
        }, 0, 10);
        View.OnClickListener searchEditTextClearFocusOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchEditTextClearFocus();
            }
        };
        constraintLayout.setOnClickListener(searchEditTextClearFocusOnClickListener);
        findViewById(R.id.toolbar).setOnClickListener(searchEditTextClearFocusOnClickListener);
        searchEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(searchEditText.getCurrentTextColor() == getResources().getColor(R.color.searchbarPlaceholder)) {
                    searchEditText.setTypeface(null, Typeface.NORMAL);
                    searchEditText.setTextColor(getResources().getColor(R.color.white));
                    searchEditText.setText("");
                }
                return false;
            }
        });
        phoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if(state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if(previousCallState == TelephonyManager.CALL_STATE_IDLE) {
                        wasPausedBeforeCall = PlayerManager.getPaused();
                        PlayerManager.setPaused(true);
                    }
                    previousCallState = state;
                }
                else if(state == TelephonyManager.CALL_STATE_IDLE && !wasPausedBeforeCall) {
                    previousCallState = state;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            PlayerManager.setPaused(false);
                        }
                    }, 2500);
                }
            }
        };
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                PlayerManager.setPaused(true);
            }
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                PlayerManager.setPaused(true);
            }
        };
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, new Handler());
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
        else if(PlayerManager.getPlayer() != null) {
            progressBar.setVisibility(View.VISIBLE);
            if(progressBar.getWidth() <= progressBarBackground.getWidth()) {
                int width = (int)Math.round(PlayerManager.getPlayer().getCurrentTime() / PlayerManager.getPlayer().getSong().getDuration() * progressBarBackground.getWidth());
                progressBar.getLayoutParams().width = width == 0 ? 1 : width;
                currentTimeTextView.setText(MyApplication.Utility.formatTime(PlayerManager.getPlayer().getCurrentTime()));
                durationTextView.setText(MyApplication.Utility.formatTime(PlayerManager.getPlayer().getSong().getDuration()));
            }
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
            currentTimeTextView.setText("");
            durationTextView.setText("");
        }
    }
    private View.OnTouchListener onProgressBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                searchEditTextClearFocus();
                if(PlayerManager.getPlayer() != null && !Playlist.isAccessingRefreshSongsProgress() && Playlist.getRefreshSongsProgress() == 0) {
                    double time = (double) motionEvent.getX() / progressBarBackground.getWidth() * PlayerManager.getPlayer().getSong().getDuration();
                    if (time > PlayerManager.getPlayer().getSong().getDuration() - 1)
                        time = PlayerManager.getPlayer().getSong().getDuration() - 1;
                    if (time < 0)
                        time = 0;
                    if(PlayerManager.getPlayer().getStopped())
                        PlayerManager.switchSong(PlayerManager.getPlayer().getSong());
                    PlayerManager.getPlayer().setCurrentTime(time);
                    if(mediaSession != null)
                        mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT).setState(PlayerManager.getPaused() ? PlaybackState.STATE_PAUSED : PlaybackState.STATE_PLAYING, (long)(1000 * PlayerManager.getPlayer().getCurrentTime()), PlayerManager.getPaused() ? 0 : 1).build());
                }
            }
            return true;
        }
    };

    public void searchEditTextClearFocus() {
        if(searchEditText.getText().toString().equals("")) {
            searchEditText.setTypeface(null, Typeface.ITALIC);
            searchEditText.setTextColor(getResources().getColor(R.color.searchbarPlaceholder));
            searchEditText.setText("Search " + Playlist.getSongs().size() + " Songs");
        }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        searchEditText.clearFocus();
    }

    public void onLyricsButtonClick(View view) {
        if(!startedLyrics) {
            startedLyrics = true;
            searchEditTextClearFocus();
            startActivity(new Intent(this, LyricsActivity.class));
        }
    }
    public void onShuffleButtonClick(View view) {
        searchEditTextClearFocus();
        PlayerManager.setShuffle(!PlayerManager.getShuffle());
        if(PlayerManager.getShuffle())
            shuffleButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shuffle_on));
        else
            shuffleButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.shuffle_off));
    }
    public void onFilterButtonClick(View view) {
        searchEditTextClearFocus();
        PlayerManager.setFilter(!PlayerManager.getFilter());
        if(PlayerManager.getFilter())
            filterButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.filter_on));
        else
            filterButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.filter_off));
        Playlist.refreshSongs();
    }

    public void onPlayPauseButtonClick(View view) {
        searchEditTextClearFocus();
        PlayerManager.setPaused(!PlayerManager.getPaused());
    }

    public void onNextButtonClick(View view) {
        searchEditTextClearFocus();
        PlayerManager.nextSong();
    }
    public void onPreviousButtonClick(View view) {
        searchEditTextClearFocus();
        PlayerManager.previousSong();
    }

    public void onAdvancedSearchButtonClick(View view) {
        searchEditTextClearFocus();
        advancedSearch = !advancedSearch;
        if(advancedSearch)
            advancedSearchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.advanced_search_on));
        else
            advancedSearchButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.advanced_search_off));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        searchEditTextClearFocus();
        Song song = ((SongListAdapter)songList.getAdapter()).getSong(view);
        if(song != null) {
            super.onCreateContextMenu(menu, view, menuInfo);
            menu.setHeaderTitle(song.getData().Artist + " - " + song.getData().Title);
            menu.add(0, view.getId(), 0, "Add To Queue").setActionView(view);
            if(PlayerManager.queueContains(song))
                menu.add(0, view.getId(), 0, "Remove From Queue").setActionView(view);
            menu.add(0, view.getId(), 0, "Song Info").setActionView(view).setEnabled(false);
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        Song song = ((SongListAdapter)songList.getAdapter()).getSong(menuItem.getActionView());
        if(menuItem.getTitle() == "Add To Queue")
            PlayerManager.addToQueue(song);
        else if(menuItem.getTitle() == "Remove From Queue")
            PlayerManager.removeFromQueue(song);
        else if(menuItem.getTitle() == "Song Info")
            new SongInfoDialog(this, song);
        else
            return false;
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startedLyrics = false;
        searchEditTextClearFocus();
    }

    public SongList getSongList() {
        return songList;
    }

    private SongInfoDialog caller = null;
    public void pickImageDialog(SongInfoDialog caller) {
        this.caller = caller;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,  7);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 7 && resultCode == RESULT_OK)
            caller.setPickImageResult(data.getData());
    }

    @Override
    public void onBackPressed() { }

}
