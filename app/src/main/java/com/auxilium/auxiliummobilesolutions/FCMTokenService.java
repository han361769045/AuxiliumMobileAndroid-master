package com.auxilium.auxiliummobilesolutions;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FCMTokenService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMTokenService";

    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
        SharedPreferences.Editor editor = this.getSharedPreferences("AMS", 0).edit();
        editor.putString("FCMToken", token);
        editor.apply();
        FirebaseCrash.logcat(Log.DEBUG, TAG, "FCM Token: " + token);
    }
}
