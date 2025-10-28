package com.soogbad.soogbadmusic;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Arrays;

public class SongData {

    public SongData(String title, String artist, String album, int year, Bitmap albumCover, String lyrics) {
        Title = title != null ? title : ""; Artist = artist != null ? artist : ""; Album = album != null ? album : ""; Year = year; AlbumCover = albumCover; Lyrics = lyrics != null ? lyrics : "";
    }

    public String Title;
    public String Artist;
    public String Album;
    public int Year;
    public Bitmap AlbumCover;
    public String Lyrics;

    public boolean contains(String key, boolean advanced) {
        String realKey = removeSpecialCharacters(key.toLowerCase());
        if(!advanced)
            return contains(removeSpecialCharacters(Artist.toLowerCase()), realKey) || contains(removeSpecialCharacters(Title.toLowerCase()), realKey);
        else
            return contains(removeSpecialCharacters(Artist.toLowerCase()), realKey) || contains(removeSpecialCharacters(Title.toLowerCase()), realKey) || contains(removeSpecialCharacters(Album.toLowerCase()), realKey) || contains(removeSpecialCharacters(Integer.toString(Year).toLowerCase()), realKey) || contains(removeSpecialCharacters(Lyrics.toLowerCase()), realKey);
    }
    private boolean contains(String str, String key)
    {
        for(int i = str.indexOf(key); ; i = str.indexOf(key, i + 1))
        {
            if(i == -1)
                break;
            else if(i == 0 || str.charAt(i - 1) == ' ')
                return true;
        }
        return false;
    }
    private final ArrayList<Byte> CHARACTERS = new ArrayList<>(Arrays.asList(Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.CONNECTOR_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL));
    private String removeSpecialCharacters(String str)
    {
        String ret = "";
        for(char chr : str.toCharArray())
            if(!CHARACTERS.contains((byte)Character.getType(chr)))
                ret += chr;
        ret = ret.replace((char)10, ' ');
        ret = ret.replace((char)13, '~');
        ret = ret.replace("~", " ");
        return ret;
    }

}
