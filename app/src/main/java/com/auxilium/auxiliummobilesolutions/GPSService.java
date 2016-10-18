package com.auxilium.auxiliummobilesolutions;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

public class GPSService extends Service implements LocationListener {

    private static final String TAG = "GPSService";
    private final IBinder binder = new LocalBinder();
    private Api api;
    private LocationManager locationManager;
    private boolean tracking;

    @Override
    public IBinder onBind(Intent intent) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Service started");

        // Build API
        Bundle extras = intent.getExtras();
        this.api = (Api)extras.getSerializable("api");

        tracking = false;
        return binder;
    }

    public boolean isTracking() { return tracking; }

    @Override
    public void onLocationChanged(Location location) {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "lat: " + location.getLatitude() + "\nlong: " + location.getLongitude());

        // Send new location
        api.request("{'$/env/gps/xinsert':{rows:[{latitude:" + location.getLatitude() +
                ",longitude:" + location.getLongitude() +
                ",lastupdated:{'$/tools/date':'now'},direction:" + location.getBearing() +
                ",speed:" + location.getSpeed() + ",accuracy:" + location.getAccuracy() +
                ",userRef:" + api.getUserId() + "}]}}");
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    public void start(int seconds) {
        if (seconds == -2) FirebaseCrash.logcat(Log.DEBUG, TAG, "Service started forever");
        else FirebaseCrash.logcat(Log.DEBUG, TAG, "Service started for " + seconds + " seconds");

        // Check permisions and start location listener
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            die();
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Helpers.createToast(this, "Please enable location services");
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(myIntent);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15 * 1000L, 0, this);

        // Terminate self after X minutes
        if (seconds != -2) {
            tracking = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    tracking = false;
                    die();
                }
            }, seconds * 1000);
        }
    }

    public void die() {
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Service terminating");

        // Kill the location listener and stop service
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (locationManager != null) locationManager.removeUpdates(this);
        stopSelf();
    }

    public class LocalBinder extends Binder {
        GPSService getService() {
            return GPSService.this;
        }
    }
}
