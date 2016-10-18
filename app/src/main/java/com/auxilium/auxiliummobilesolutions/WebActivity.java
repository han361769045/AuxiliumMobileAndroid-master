package com.auxilium.auxiliummobilesolutions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.crash.FirebaseCrash;

public class WebActivity extends AppCompatActivity {

    private static final String TAG = "WebActivity";

    private final int PERMS = 16525;
    private WebView webView;
    private GPSService gpsService;
    private boolean gpsBound = false;
    private int tempMinute;
    private Bundle extras;
    private Snackbar snackbar;
    private Api api;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        // Set up action bar authestics
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_icon_small);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(R.string.app_title);
        }

        // Retrieve WebView
        webView = (WebView)findViewById(R.id.webView);

        // Logic to decide whether url should be handled by webview or externally
        // todo: Webclient for <input type="file" />
        webView.setWebViewClient(new WebViewClient() {
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(final WebView view, final String url){
                return false;
            }
        });

        // Add Javascript binding to allow webpage to interact with the app
        webView.addJavascriptInterface(new WebInterface(this), "AMS");

        // Get URL passed into activity
        extras = getIntent().getExtras();
        String url = extras.getString("url");
        api = (Api)extras.getSerializable("api");

        // Allow and set cookies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        CookieManager.getInstance().setCookie("https://api.datalynk.ca", api.getCookie());
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Set cookie to: " + api.getCookie());

        // Allow Javascript
        webView.getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        }

        // Tell WebView to load URL
        webView.loadUrl(url);
        FirebaseCrash.logcat(Log.DEBUG, TAG, "Loading URL: " + url);

        // Setup GPS service
        Intent intent = new Intent(getBaseContext(), GPSService.class);
        intent.putExtra("api", api);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        fireEvent("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        fireEvent("onPause");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create menu in action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item selected
        switch (item.getItemId()) {
            case R.id.fifteen:
                startTracking(15 * 60, false);
                return true;
            case R.id.thirty:
                startTracking(30 * 60, false);
                return true;
            case R.id.hour:
                startTracking(60 * 60, false);
                return true;
            case R.id.indef:
                startTracking(-2, false);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.refresh:
                if (BuildConfig.DEBUG) webView.clearCache(true);
                webView.reload();
                return true;
            case R.id.logout:
                // Logout of the API
                api.logout();
                gpsService.die();
                unbindService(connection);

                // Remove saved info to prevent auto login
                SharedPreferences.Editor editor = this.getSharedPreferences("AMS", 0).edit();
                editor.putString("cookie", null);
                editor.putString("password", null);
                editor.apply();

                // Return to login screen
                startActivity(new Intent(getBaseContext(), LoginActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Intercept back button and go back on the browser
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void fireEvent(final String event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("if(mobile != null) mobile.fireEvent('" + event + "')", null);
                } else {
                    webView.loadUrl("javascript:if(mobile != null) mobile.fireEvent('" + event + "')");
                }
            }
        });
    }

    public void startTracking(int seconds, boolean automated) {
        SharedPreferences preferences = getSharedPreferences("AMS", 0);
        int max = preferences.getInt("maxTrackLength", 15) * 60;
        if (seconds == -1) seconds = max;
        if (automated) {
            if (!getSharedPreferences("AMS", 0).getBoolean("gpsEnabled", true)) return;
            seconds = seconds > max ? max : seconds;
        }

        // If we have GPS permisions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Set default minutes if requested

            // Create a snackbar for the tracker to be dismissed from
            View view = findViewById(R.id.coordinatorLayout);
            //noinspection ConstantConditions
            snackbar = Snackbar.make(view, null, Snackbar.LENGTH_INDEFINITE).setAction("STOP TRACKING", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopTracking();
                    Snackbar.make(view, "Tracking has been stopped.", Snackbar.LENGTH_LONG).show();
                }
            });
            snackbar.show();

            // update snackbar text every minute and dismiss if no time is left
            updateSnackbar(snackbar, seconds);

            // Start service for x minutes
            if (gpsBound) {
                gpsService.start(seconds);
            }

            fireEvent("gpsStarted");
        } else {
            // If we dont have permision to use the GPS save the request and ask for permission
            tempMinute = seconds;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMS);
        }
    }

    public void stopTracking() {
        if (!gpsService.isTracking()) return;
        if (snackbar != null) {
            snackbar.dismiss();
        }

        gpsService.die();
        fireEvent("gpsStopped");
    }

    public boolean isTracking() {
        return gpsService.isTracking();
    }

    private void updateSnackbar(final Snackbar snackbar, final int seconds) {
        // Update text
        if (seconds == -2) snackbar.setText("Tracking: ∞");
        else snackbar.setText(String.format(getResources().getString(R.string.tracking_snackbar), seconds/60, seconds % 60));
        // After 1 minute
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (seconds > 0) {
                            // Make a recursive call to update text with a minute less
                            updateSnackbar(snackbar, seconds - 1);
                        } else if (seconds == 0) {
                            // No time is left so just dismiss the snackbar
                            snackbar.dismiss();
                        }
                    }
                });
            }
        }, 1000);
    }

    // Set up binding for GPS service
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            gpsService = binder.getService();
            gpsBound = true;

            // Once service is started, check if a GPS tracking request has been made
            int gpsVal = extras.getInt("gps");
            if (gpsVal != 0) {
                if (gpsVal != -1) {
                    gpsVal *= 60;
                }
                startTracking(gpsVal, true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gpsBound = false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        // If we got permission retry the request
        switch (requestCode) {
            case PERMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTracking(tempMinute, false);
                    tempMinute = 0;
                } else {
                    // Alert the user we cant use the GPS until we get permission
                    Helpers.createToast(this, getResources().getString(R.string.gps_permision));
                }
            }
        }
    }
}
