package com.auxilium.auxiliummobilesolutions;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;

import com.google.firebase.crash.FirebaseCrash;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "Settings";

    private Switch alarm;
    private Switch autoLogin;
    private Switch gps;
    private NumberPicker np;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_icon_small);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("Settings");
        }

        // Get views
        alarm = (Switch) findViewById(R.id.switch1);
        autoLogin = (Switch) findViewById(R.id.switch2);
        gps = (Switch) findViewById(R.id.switch3);
        np = (NumberPicker) findViewById(R.id.numberPicker1);
        gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    findViewById(R.id.textView5).setVisibility(View.VISIBLE);
                    np.setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.textView5).setVisibility(View.INVISIBLE);
                    np.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Set defaults for views
        SharedPreferences preferences = getSharedPreferences("AMS", 0);
        alarm.setChecked(preferences.getBoolean("alarmEnabled", true));
        autoLogin.setChecked(preferences.getBoolean("autoLoginEnabled", true));
        gps.setChecked(preferences.getBoolean("gpsEnabled", true));
        np.setMinValue(1);
        np.setMaxValue(60);
        np.setValue(preferences.getInt("maxTrackLength", 15));

        FirebaseCrash.logcat(Log.DEBUG, TAG, "Settings openned");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Hook into the back button and save
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    save();
                    FirebaseCrash.logcat(Log.DEBUG, TAG, "Settings closed");
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void save() {
        SharedPreferences.Editor editor = getSharedPreferences("AMS", 0).edit();
        editor.putBoolean("alarmEnabled", alarm.isChecked());
        editor.putBoolean("autoLoginEnabled", autoLogin.isChecked());
        editor.putBoolean("gpsEnabled", gps.isChecked());
        editor.putInt("maxTrackLength", np.getValue());
        if (!autoLogin.isChecked()) {
            editor.putString("password", null);
            editor.putString("cookie", null);
        }
        editor.apply();
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Settings saved:\nalarm - " + alarm.isChecked() +
                "\nauto login - " + autoLogin.isChecked() + "\nGPS - " + gps.isChecked() +
                "\nGPS Max - " + np.getValue());
    }
}
