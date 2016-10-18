package com.auxilium.auxiliummobilesolutions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.google.firebase.crash.FirebaseCrash;

@SuppressWarnings("unused")
public class WebInterface {

    private static final String TAG = "WebInterface";
    private Context context;

    WebInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void createToast(String message) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Creating toast via web interface");
        Helpers.createToast(context, message);
    }

    @JavascriptInterface
    public void createNotification(String title, String body, String url, boolean alarm, int gps) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Creating notification via web interface");
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("alarm", alarm);
        intent.putExtra("gps", gps);
        Helpers.createNotification(context, intent, title, body);
    }

    @JavascriptInterface
    public void startTracking(int minutes) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Starting tracker via web interface");
        Activity activity = (Activity) context;
        ((WebActivity)activity).startTracking(minutes, true);
    }

    @JavascriptInterface
    public void stopTracking() {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Stopped tracking via web interface");
        Activity activity = (Activity) context;
        ((WebActivity)activity).stopTracking();
    }

    @JavascriptInterface
    public boolean isTracking() {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Web intreface checking if tracking");
        Activity activity = (Activity) context;
        return ((WebActivity)activity).isTracking();
    }

    @JavascriptInterface
    public void setVolume(float volume) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Changing volume via web interface");
        Helpers.setVolume(context, volume);
    }

    @JavascriptInterface
    public void soundAlarm() {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Sounding alarm via web interface");
        Helpers.soundAlarm(context);
    }
}
