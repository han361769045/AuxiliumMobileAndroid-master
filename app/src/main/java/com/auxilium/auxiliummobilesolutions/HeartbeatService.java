package com.auxilium.auxiliummobilesolutions;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

public class HeartbeatService extends Service {

    private static final String TAG = "HeartbeatService";
    private static Api api;
    public HeartbeatService() { }
    public IBinder onBind(Intent intent){ return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        this.api = (Api)extras.getSerializable("api");
        heartBeat();
        return Service.START_REDELIVER_INTENT;
    }

    private void heartBeat() {
        api.request("{'$/env/users/xupdate':{rows:[{lastreported:{'$/tools/date':'now'},id:" + api.getUserId() + "}]}}");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                heartBeat();
            }
        }, 900000);
    }
}
