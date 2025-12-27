package com.soogbad.soogbadmusic;

import android.app.Application;

public class SoogbadMusicApplication extends Application {

    private static SoogbadMusicApplication app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static SoogbadMusicApplication getAppContext() {
        return app;
    }

}
