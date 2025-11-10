package com.soogbad.soogbadmusic;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

public class MyApplication extends Application {

    private static MyApplication app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static MyApplication getAppContext() {
        return app;
    }


    public static class Utility {

        public static Activity getActivity(View view) {
            Context context = view.getContext();
            while(context instanceof ContextWrapper) {
                if(context instanceof Activity)
                    return (Activity)context;
                context = ((ContextWrapper)context).getBaseContext();
            }
            return null;
        }

        public static int dpToPixels(int dp, DisplayMetrics displayMetrics) {
            return (int)(dp * (displayMetrics.densityDpi / 160.0));
        }

        public static Bitmap getBitmap(Drawable drawable) {
            return drawable == null ? null : ((BitmapDrawable)drawable).getBitmap();
        }

        public static String formatTime(double seconds) {
            int mins = (int)(seconds / 60);
            int secs = (int)(seconds % 60);
            return mins + ":" + (secs >= 10 ? "" : "0") + secs;
        }

        public static void shortenTextViewText(TextView textView, int limitRight) {
            String initText = textView.getText().toString();
            String text = initText;
            for(int i = 1; ; i++) {
                if(text == "")
                    return;
                Rect bounds = new Rect();
                textView.getPaint().getTextBounds(text, 0, text.length() - 1, bounds);
                if(textView.getLeft() + bounds.width() <= limitRight)
                    break;
                text = initText.substring(0, initText.length() - i - 1) + "...";
            }
            textView.setText(text.replaceAll(" \\.\\.\\.", "..."));
        }

    }

}
