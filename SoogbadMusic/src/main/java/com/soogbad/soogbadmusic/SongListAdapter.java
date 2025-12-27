package com.soogbad.soogbadmusic;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongListViewHolder> {

    private ArrayList<Song> songs;
    private final Dictionary<View, Song> dictionary = new Hashtable<>();

    public static class SongListViewHolder extends RecyclerView.ViewHolder {
        public SongListViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.songListItemNameTextView);
            infoTextView = itemView.findViewById(R.id.songListItemInfoTextView);
            durationTextView = itemView.findViewById(R.id.songListItemDurationTextView);
            durationTextView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Utility.shortenTextViewText(nameTextView, durationTextView.getLeft() - 50);
                Utility.shortenTextViewText(infoTextView, durationTextView.getLeft() - 50);
            });
        }
        public final TextView nameTextView;
        public final TextView infoTextView;
        public final TextView durationTextView;
        public void setTextColor(int color) {
            int c = ContextCompat.getColor(SoogbadMusicApplication.getAppContext(), color);
            nameTextView.setTextColor(c);
            infoTextView.setTextColor(c);
            durationTextView.setTextColor(c);
        }
    }

    public SongListAdapter(ArrayList<Song> songs) {
        this.songs = songs;
    }

    @NonNull
    @Override
    public SongListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SongListViewHolder viewHolder = new SongListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false));
        Activity activity = Utility.getActivity(viewHolder.itemView);
        if(activity != null)
            activity.registerForContextMenu(viewHolder.itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SongListViewHolder viewHolder, int position) {
        final Song song = songs.get(position);
        if(song == null) {
            dictionary.remove(viewHolder.itemView);
            viewHolder.nameTextView.setText("");
            viewHolder.infoTextView.setText("");
            viewHolder.durationTextView.setText("");
        }
        else {
            dictionary.put(viewHolder.itemView, song);
            SongData data = song.getData();
            viewHolder.nameTextView.setText(MessageFormat.format("{0} - {1}", data.Artist, data.Title));
            viewHolder.infoTextView.setText(MessageFormat.format("{0} ({1})", data.Album, String.valueOf(data.Year)));
            viewHolder.durationTextView.setText(Utility.formatTime(song.getDuration()));
            viewHolder.itemView.setOnClickListener(view -> {
                MainActivity mainActivity = ((MainActivity)Utility.getActivity(view));
                if(mainActivity != null)
                    mainActivity.searchEditTextClearFocus();
                PlaybackManager.switchSong(song);
            });
            viewHolder.setTextColor(PlaybackManager.getPlayer() != null && PlaybackManager.getPlayer().getSong().getPath().equals(song.getPath()) ? R.color.yellow : R.color.white);
        }
        viewHolder.itemView.setBackgroundResource(viewHolder.getBindingAdapterPosition() % 2 == 0 ? R.color.lightSong : R.color.darkSong);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
    public ArrayList<Song> getSongs() {
        return songs;
    }
    public void setSongs(ArrayList<Song> songs) { this.songs = songs; }
    public Song getSong(View itemView) { return dictionary.get(itemView); }

}
