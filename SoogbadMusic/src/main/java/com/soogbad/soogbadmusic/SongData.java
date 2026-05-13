package com.soogbad.soogbadmusic;

import android.graphics.Bitmap;

import androidx.media3.common.MediaMetadata;

import java.util.ArrayList;
import java.util.Arrays;

public class SongData {

    public SongData(String title, String artist, String album, int year, String genre, Bitmap albumCover, String lyrics) {
        Title = title != null ? title : ""; Artist = artist != null ? artist : ""; Album = album != null ? album : ""; Year = year; Genre = genre != null ? genre : ""; AlbumCover = albumCover; Lyrics = lyrics != null ? lyrics : "";
    }

    public final String Title;
    public final String Artist;
    public final String Album;
    public final int Year;
    public final String Genre;
    public Bitmap AlbumCover;
    public String Lyrics;

    public boolean contains(String key, boolean advanced) {
        String realKey = removeSpecialCharacters(key.toLowerCase());
        if(!advanced)
            return contains(removeSpecialCharacters(Artist.toLowerCase()), realKey) || contains(removeSpecialCharacters(Title.toLowerCase()), realKey);
        else
            return contains(removeSpecialCharacters(Artist.toLowerCase()), realKey) || contains(removeSpecialCharacters(Title.toLowerCase()), realKey) || contains(removeSpecialCharacters(Album.toLowerCase()), realKey) || contains(removeSpecialCharacters(Integer.toString(Year).toLowerCase()), realKey) || contains(removeSpecialCharacters(Genre.toLowerCase()), realKey) || contains(removeSpecialCharacters(Lyrics.toLowerCase()), realKey);
    }
    public static boolean contains(MediaMetadata metadata, String key) {
        String realKey = removeSpecialCharacters(key.toLowerCase());
        String title = metadata.title != null ? removeSpecialCharacters(metadata.title.toString().toLowerCase()) : "";
        String artist = metadata.artist != null ? removeSpecialCharacters(metadata.artist.toString().toLowerCase()) : "";
        return contains(artist, realKey) || contains(title, realKey);
    }
    private static final ArrayList<Byte> CHARACTERS = new ArrayList<>(Arrays.asList(Character.DASH_PUNCTUATION, Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.CONNECTOR_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION, Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL));
    private static String removeSpecialCharacters(String str)
    {
        StringBuilder ret = new StringBuilder();
        for(char chr : str.toCharArray())
            if(!CHARACTERS.contains((byte)Character.getType(chr)))
                ret.append(chr);
        String retStr = ret.toString().replace((char)10, ' ');
        retStr = retStr.replace((char)13, '~');
        retStr = retStr.replace("~", " ");
        return retStr;
    }
    private static boolean contains(String str, String key)
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

}
