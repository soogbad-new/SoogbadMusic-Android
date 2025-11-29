package com.soogbad.soogbadmusic;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

public class Player {

    private final Song song;
    private boolean stopped = false;
    private final MediaPlayer mediaPlayer;

    private final ArrayList<EmptyListener> onPlaybackCompletedListeners = new ArrayList<>();
    public void addOnPlaybackCompletedListener(EmptyListener listener) {
        onPlaybackCompletedListeners.add(listener);
    }

    /** @noinspection CallToPrintStackTrace*/
    public Player(Song song) {
        this.song = song;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            stopped = true;
            for(EmptyListener listener : onPlaybackCompletedListeners)
                listener.onListenerInvoked();
        });
        mediaPlayer.setOnErrorListener((mediaPlayer, errorType, extraErrorCode) -> {
            for(EmptyListener listener : onPlaybackCompletedListeners)
                listener.onListenerInvoked();
            return false;
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

    public void play() {
        mediaPlayer.start();
    }
    public void pause() {
        mediaPlayer.pause();
    }

    public void release() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

}
