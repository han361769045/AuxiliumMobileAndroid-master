package com.auxilium.auxiliummobilesolutions;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Message recieved");

        // Assemble data
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : message.getData().entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        Intent intent = new Intent(getBaseContext(), LoginActivity.class);
        intent.putExtras(bundle);

        // Send notification
        Helpers.createNotification(getBaseContext(), intent, bundle.getString("title"), bundle.getString("body"));

        // If there is an alarm key that is true, sound off
        if (message.getData().containsKey("alarm") && message.getData().get("alarm").equals("true")) {
            Helpers.soundAlarm(getBaseContext());
        }
    }


}
