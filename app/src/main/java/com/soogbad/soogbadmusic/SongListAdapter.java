package com.soogbad.soogbadmusic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongListViewHolder> {

    private ArrayList<Song> songs;
    private Dictionary<View, Song> dictionary = new Hashtable<>();

    public static class SongListViewHolder extends RecyclerView.ViewHolder {
        public SongListViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.songListItemNameTextView);
            infoTextView = itemView.findViewById(R.id.songListItemInfoTextView);
            durationTextView = itemView.findViewById(R.id.songListItemDurationTextView);
            durationTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int limitTextRight = durationTextView.getLeft() - 100;
                    MyApplication.Utility.shortenTextViewText(nameTextView, limitTextRight);
                    MyApplication.Utility.shortenTextViewText(infoTextView, limitTextRight);
                }
            });
        }
        public TextView nameTextView;
        public TextView infoTextView;
        public TextView durationTextView;
        public void setTextColor(int color) {
            int c = ContextCompat.getColor(MyApplication.getAppContext(), color);
            nameTextView.setTextColor(c);
            infoTextView.setTextColor(c);
            durationTextView.setTextColor(c);
        }
    }

    public SongListAdapter(ArrayList<Song> songs) {
        this.songs = songs;
    }

    @Override
    public SongListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SongListViewHolder viewHolder = new SongListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false));
        MyApplication.Utility.getActivity(viewHolder.itemView).registerForContextMenu(viewHolder.itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(SongListViewHolder viewHolder, int position) {
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
            viewHolder.nameTextView.setText(data.Artist + " - " + data.Title);
            viewHolder.infoTextView.setText(data.Album + " (" + data.Year + ")");
            viewHolder.durationTextView.setText(MyApplication.Utility.formatTime(song.getDuration()));
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MainActivity)MyApplication.Utility.getActivity(view)).searchEditTextClearFocus();
                    PlayerManager.switchSong(song);
                }
            });
            viewHolder.setTextColor(PlayerManager.getPlayer() != null && PlayerManager.getPlayer().getSong().getPath().equals(song.getPath()) ? R.color.yellow : R.color.white);
        }
        viewHolder.itemView.setBackgroundResource(position % 2 == 0 ? R.color.lightSong : R.color.darkSong);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public Song getSong(View itemView) {
        return dictionary.get(itemView);
    }

}
