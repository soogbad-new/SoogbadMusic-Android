package com.soogbad.soogbadmusic;

import java.text.Collator;
import java.util.Comparator;

public class SongComparator implements Comparator<Song> {
    public int compare(Song a, Song b) {
        int artist = Collator.getInstance().compare(a.getData().Artist, b.getData().Artist);
        if(artist != 0)
            return artist;
        int year = Integer.compare(a.getData().Year, b.getData().Year);
        if(year != 0)
            return year;
        int album = Collator.getInstance().compare(a.getData().Album, b.getData().Album);
        if(album != 0)
            return album;
        int title = Collator.getInstance().compare(a.getData().Title, b.getData().Title);
        if(title != 0)
            return title;
        return Collator.getInstance().compare(a.getPath(), b.getPath());
    }
}
