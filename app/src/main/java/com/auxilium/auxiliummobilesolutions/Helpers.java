package com.auxilium.auxiliummobilesolutions;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

public class Helpers {

    private static final String TAG = "Helpers";

    public static void createNotification(Context context, Intent intent, String title, String body) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Notification created");

        @SuppressWarnings("deprecation")
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification.build());
    }

    public static void createToast(Context context, String message) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Toast created");

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void setVolume(Context context, float volume) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Volume adjusted to: " + volume);

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int v = Math.round(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * volume);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    public static void soundAlarm(Context context) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Alarm sounded");

        if (!context.getSharedPreferences("AMS", 0).getBoolean("alarmEnabled", true)) return;
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.alarm);
        mediaPlayer.start();
    }
}
