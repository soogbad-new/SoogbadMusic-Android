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
import java.util.Objects;

public class SongList extends RecyclerView {

    private int visibleItems;
    private int firstPosition = -1, lastPosition = -1;
    private int invisibleSongsAmount;

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
                PlayerManager.setPaused(false);
                int itemHeight = MyApplication.Utility.dpToPixels(50, getResources().getDisplayMetrics());
                visibleItems = getHeight() / itemHeight;
                getLayoutParams().height = visibleItems * itemHeight;
                ConstraintSet set = new ConstraintSet();
                ConstraintLayout layout = (ConstraintLayout)getParent();
                set.clone(layout);
                set.clear(R.id.songList, ConstraintSet.BOTTOM);
                set.applyTo(layout);
                changeSongList(new ArrayList<>(), false);
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

    public void changeSongList(ArrayList<Song> songs, boolean keepScroll) {
        ArrayList<Song> list = new ArrayList<>(songs);
        int songsCount = list.size();
        int necessaryInvisibleSongsAmount = invisibleSongsAmount;
        for(int i = 1; i <= visibleItems - songsCount; i++) {
            list.add(null);
            necessaryInvisibleSongsAmount--;
        }
        for(int i = 1; i <= necessaryInvisibleSongsAmount; i++)
            list.add(null);
        if(keepScroll && getAdapter() != null)
            ((SongListAdapter)getAdapter()).setSongs(list);
        else
            setAdapter(new SongListAdapter(list));
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return firstPosition;
    }
    public int findLastCompletelyVisibleItemPosition() {
        return lastPosition;
    }

    public void addInvisibleSongsToMakeUpForCoveredArea(int coveredAreaHeight) {
        if(getAdapter() == null)
            return;
        int itemHeight = MyApplication.Utility.dpToPixels(50, getResources().getDisplayMetrics());
        invisibleSongsAmount = coveredAreaHeight / itemHeight;
        changeSongList(((SongListAdapter)getAdapter()).getSongs(), true);
    }
    public void removeAllInvisibleSongs() {
        invisibleSongsAmount = 0;
        if(getAdapter() == null)
            return;
        ArrayList<Song> songs = ((SongListAdapter)getAdapter()).getSongs();
        songs.removeIf(Objects::isNull);
        changeSongList(songs, true);
    }

}
