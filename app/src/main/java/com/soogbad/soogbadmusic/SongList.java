package com.soogbad.soogbadmusic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SongList extends RecyclerView {

    private int songListItems;
    private int firstPosition = -1, lastPosition = -1;

    public SongList(@NonNull Context context) {
        super(context);
        constructor();
    }
    public SongList(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        constructor();
    }
    public SongList(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        constructor();
    }
    private void constructor() {
        setHasFixedSize(true);
        setLayoutManager(new LinearLayoutManager(getContext()));
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int itemHeight = MyApplication.Utility.dpToPixels(50, getResources().getDisplayMetrics());
                songListItems = getHeight() / itemHeight;
                getLayoutParams().height = songListItems * itemHeight;
                ConstraintSet set = new ConstraintSet();
                ConstraintLayout layout = (ConstraintLayout)getParent();
                set.clone(layout);
                set.clear(R.id.songList, ConstraintSet.BOTTOM);
                set.applyTo(layout);
                changeSongList(new ArrayList<>());
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LinearLayoutManager layoutManager = (LinearLayoutManager)getLayoutManager();
                firstPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                lastPosition = layoutManager.findLastCompletelyVisibleItemPosition();
            }
        });
    }

    public void changeSongList(ArrayList<Song> songs) {
        ArrayList<Song> list = new ArrayList<>(songs);
        int songsCount = list.size();
        for(int i = 1; i <= songListItems - songsCount; i++)
            list.add(null);
        setAdapter(new SongListAdapter(list));
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return firstPosition;
    }
    public int findLastCompletelyVisibleItemPosition() {
        return lastPosition;
    }

}
