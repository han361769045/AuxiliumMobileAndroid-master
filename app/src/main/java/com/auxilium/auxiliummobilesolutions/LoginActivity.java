package com.auxilium.auxiliummobilesolutions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private static SharedPreferences preferences;
    private static Api api;

    private TextView clientView;
    private TextView usernameView;
    private TextView passwordView;

    private Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get fields
        clientView = (TextView) findViewById(R.id.client);
        usernameView = (TextView) findViewById(R.id.username);
        passwordView = (TextView) findViewById(R.id.password);

        // Set FCM Token and begin listening for messages
        startService(new Intent(this, FCMTokenService.class));

        // Load bundle
        extras = getIntent().getExtras();

        // Load data to make requests
        api = new Api("https://api.datalynk.ca/dispatch.php");
        preferences = this.getSharedPreferences("AMS", 0);
        int client = preferences.getInt("client", -1);
        String username = preferences.getString("username", null);
        String password = preferences.getString("password", null);
        api.setSpoke(preferences.getString("spoke", null));
        if (extras != null && extras.getString("cookie") != null) {
            api.setCookie(extras.getString("cookie"));
        } else {
            api.setCookie(preferences.getString("cookie", null));
        }

        if (preferences.getBoolean("autoLoginEnabled", true)) {
            // Resume saved session if valid
            if (api.getCookie() != null && api.getSpoke() != null) {
                if (api.isLoggedIn()) {
                    launchWebView();
                }
            }

            // If we have all credentials attempt a login
            if (client != -1 && username != null && password != null) {
                if (api.getSpoke() == null) {
                    String spoke = api.getSpokeFromId(client);
                    if (spoke == null) {
                        clientView.setError("Invalid client ID");
                    } else {
                        api.setSpoke(spoke);
                    }
                }
                if (login(username, password)) {
                    launchWebView();
                }
            }
        }

        // If we can't log you in, autocomplete what we can
        if (client != -1) { clientView.setText(String.valueOf(client)); }
        usernameView.setText(username);
        passwordView.setText(password);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Intercept back button and go back on the browser
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean login(String username, String password) {
        // Attempt login and highlight any wrong information

        int result = api.login(username, password);
        switch (result) {
            case 0:
                usernameView.setError("This user does not exist");
                return false;
            case -1:
                passwordView.setError("Incorrect password");
                return false;
        }
        return true;
    }

    @SuppressWarnings("UnusedParameters")
    public void login(View view) {
        // Check for empty fields
        if (clientView.getText().toString().isEmpty()) {
            clientView.setError("Please enter an ID");
            return;
        }
        if (usernameView.getText().toString().isEmpty()) {
            usernameView.setError("Please enter an ID");
            return;
        }
        if (passwordView.getText().toString().isEmpty()) {
            passwordView.setError("Please enter an ID");
            return;
        }

        // Get spoke
        String spoke = api.getSpokeFromId(Integer.parseInt(clientView.getText().toString()));
        if (spoke == null) {
            clientView.setError("Invalid client ID");
        } else {
            api.setSpoke(spoke);
        }

        // Attempt login
        if (login(usernameView.getText().toString(), passwordView.getText().toString())) {
            // Create a heart beat
            Intent intent = new Intent(this, HeartbeatService.class);
            intent.putExtra("api", api);
            startService(intent);

            // Save prefrences
            savePreferences();

            // Launch webview
            launchWebView();
        }
    }

    private void launchWebView() {
        updateMessageToken();
        Intent webActivity = new Intent(getBaseContext(), WebActivity.class);
        //noinspection ConstantConditions
        if (extras != null && extras.getString("url") != null && !extras.getString("url").isEmpty())
            webActivity.putExtra("url", extras.getString("url"));
        else webActivity.putExtra("url", api.getLandingPage());
        webActivity.putExtra("api", api);
        if (extras != null) webActivity.putExtra("gps", Integer.parseInt(extras.getString("gps", "0")));
        startActivity(webActivity);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("client", Integer.parseInt(clientView.getText().toString()));
        editor.putString("username", api.getUsername());
        editor.putString("spoke", api.getSpoke());
        if (preferences.getBoolean("autoLoginEnabled", true)) {
            editor.putString("cookie", api.getCookie());
            editor.putString("password", api.getPassword());
        }
        editor.apply();
    }

    private void updateMessageToken() {
        SharedPreferences preferences = this.getSharedPreferences("AMS", 0);
        api.updateFCMToken(preferences.getString("FCMToken", FirebaseInstanceId.getInstance().getToken()));
    }
}
