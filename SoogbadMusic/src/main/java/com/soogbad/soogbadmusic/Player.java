package com.soogbad.soogbadmusic;

import androidx.annotation.NonNull;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import android.net.Uri;

import java.util.ArrayList;

public class Player {

    private final Song song;
    private boolean stopped = false;
    private final ExoPlayer mediaPlayer;

    private final ArrayList<Utility.EmptyListener> onPlaybackCompletedListeners = new ArrayList<>();
    public void addOnPlaybackCompletedListener(Utility.EmptyListener listener) {
        onPlaybackCompletedListeners.add(listener);
    }

    public Player(Song song) {
        this.song = song;
        mediaPlayer = new ExoPlayer.Builder(SoogbadMusicApplication.getAppContext()).build();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        mediaPlayer.addListener(new androidx.media3.common.Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                androidx.media3.common.Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == androidx.media3.common.Player.STATE_ENDED)
                    notifyCompletion();
            }
            @Override public void onPlayerError(@NonNull PlaybackException error) {
                androidx.media3.common.Player.Listener.super.onPlayerError(error);
                notifyCompletion();
            }
        });
        /*if(song.getMediaItem() != null)
            mediaPlayer.setMediaItem(song.getMediaItem());
        else
            */mediaPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(song.getFile())));
        mediaPlayer.prepare();
    }

    public Song getSong() {
        return song;
    }

    public ExoPlayer getExoPlayer() { return mediaPlayer; }

    public boolean getStopped() { return stopped; }
    
    public long getCurrentTime() {
        if(stopped)
            return getSong().getDuration();
        else
            return mediaPlayer.getCurrentPosition();
    }
    public void setCurrentTime(long currentTime) {
        if(!stopped && currentTime >= 0 && currentTime < getSong().getDuration())
            mediaPlayer.seekTo(currentTime);
    }

    public void play() {
        mediaPlayer.play();
    }
    public void pause() {
        mediaPlayer.pause();
    }

    public void release() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    private void notifyCompletion() {
        stopped = true;
        for(Utility.EmptyListener listener : onPlaybackCompletedListeners)
            listener.onListenerInvoked();
    }

}
