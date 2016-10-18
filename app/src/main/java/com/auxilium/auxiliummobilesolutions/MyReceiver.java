package com.auxilium.auxiliummobilesolutions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

public class MyReceiver extends BroadcastReceiver {

    private static final String TAG = "MyReciever";

    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Boot complete recieved");
        Intent myIntent = new Intent(context, MessagingService.class);
        context.startService(myIntent);
    }
}
