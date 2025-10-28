package com.soogbad.soogbadmusic;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

public class Player {

    private Song song;
    private float volume = 1.0f;
    private boolean paused;
    private boolean stopped = false;
    private MediaPlayer mediaPlayer;

    private ArrayList<EmptyListener> onPlaybackCompletedListeners = new ArrayList<>();
    public void addOnPlaybackCompletedListener(EmptyListener listener) {
        onPlaybackCompletedListeners.add(listener);
    }

    public Player(Song song) {
        this.song = song;
        paused = true;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopped = true;
                for(EmptyListener listener : onPlaybackCompletedListeners)
                    listener.onListenerInvoked();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int errorType, int extraErrorCode) {
                for(EmptyListener listener : onPlaybackCompletedListeners)
                    listener.onListenerInvoked();
                return false;
            }
        });
        try {
            mediaPlayer.setDataSource(MyApplication.getAppContext(), Uri.fromFile(song.getFile()));
            mediaPlayer.prepare();
        }
        catch(IOException e) { e.printStackTrace(); }
    }

    public Song getSong() {
        return song;
    }
    public boolean getStopped() { return stopped; }
    public boolean getPaused() {
        return paused;
    }
    public double getCurrentTime() {
        if(stopped)
            return getSong().getDuration();
        else
            return mediaPlayer.getCurrentPosition() / 1000.0;
    }
    public void setCurrentTime(double currentTime) {
        if(!stopped && currentTime >= 0 && currentTime < getSong().getDuration())
            mediaPlayer.seekTo((int)Math.round(currentTime * 1000));
    }
    public float getVolume() {
        return volume;
    }
    public void setVolume(float volume) {
        this.volume = volume;
        mediaPlayer.setVolume(volume, volume);
    }

    public void play() {
        paused = false;
        mediaPlayer.start();
    }
    public void pause() {
        paused = true;
        mediaPlayer.pause();
    }

    public void release() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

}
