package com.auxilium.auxiliummobilesolutions;

import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

class Api implements Serializable {

    private static final String TAG = "API";

    private String url;
    private String spoke;
    private String cookie;
    private int userId = -1;
    private String landingPage;
    private String username;
    private String password;

    private int reqCount = (int)(Math.random() * 1000);

    Api(String url) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        this.url = url;
    }

    String getUrl() { return this.url; }

    String getCookie() {
        return this.cookie;
    }

    String getSpoke() { return this.spoke; }

    String getSpokeFromId(int id) {
        try {
            // Send request
            JSONObject response = new JSONObject(request("{\"$/client/spokes\":{}}"));

            // Check first index for spoke name and return it
            JSONArray rows = response.getJSONArray("rows");
            for (int i = 0; i < rows.length(); i++) {
                if (((JSONObject) rows.get(i)).getInt("id") == id)
                    return ((JSONObject) rows.get(i)).getString("spoke");
            }
        } catch (JSONException e) {
            FirebaseCrash.report(e);
        }
        return null;
    }

    int getUserId() {
        return this.userId;
    }

    String getLandingPage() { return this.landingPage; }

    String getUsername() { return this.username; }

    String getPassword() { return this.password; }

    boolean isLoggedIn() {
        try {
            // Send request
            JSONObject response = new JSONObject(request("{\"$/env/me\":{}}"));

            // If the response email is guest@datalynk.ca, its a guest account and not logged in
            if (!response.getString("email").equals("guest@datalynk.ca")) {
                this.userId = response.getInt("id");
                // Get the landing page or redirect to spoke
                if (response.has("landingpage")) {
                    this.landingPage = response.getString("landingpage");
                } else {
                    this.landingPage = "https://" + spoke + ".auxiliumgroup.com";
                }
                return true;
            }
        } catch (JSONException e) {
            FirebaseCrash.report(e);
        }
        this.userId = -1;
        return false;
    }

    void setSpoke(String spoke) {
        this.spoke = spoke;
    }

    void setCookie(String cookie) { this.cookie = cookie; }

    int login(String username, String password) {
        try {
            // Send request
            JSONObject request = new JSONObject(request("{\"$/auth/login\":{login:\"" + username + "\",password:\"" + password + "\"}}", true));

            // If there is no error then success!
            if (!request.has("error")) {
                this.username = username;
                this.password = password;
                isLoggedIn();
                return 1;
                // If we get an unknown user send back 0 for not found
            } else if (request.get("error").toString().contains("Unknown user")) {
                return 0;
            }
        } catch (JSONException e) {
            FirebaseCrash.report(e);
        }
        // If we have an unknown error send -1 (Interpreted as incorrect password)
        return -1;
    }

    void updateFCMToken(String token) {
        request("{'$/env/users/xupdate':{rows:[{communicationid:'" + token + "',phonetype:'Android',id:" + userId + "}]}}");
    }

    @SuppressWarnings("unused")
    void logout() {
        request("{\"$/auth/logout\":{}}", true);
    }

    String request(String request) {
        return request(request, false);
    }

    private String request(String request, boolean cookie) {
        reqCount++;
        try {
            FirebaseCrash.logcat(Log.DEBUG, TAG, "Request (" + reqCount + "): " + request);
            HttpURLConnection con = (HttpURLConnection) new URL(this.url).openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Cookie", this.cookie);
            con.setRequestMethod("POST");

            JSONObject parent = new JSONObject();
            parent.put("req", new JSONObject(request).toString());
            parent.put("spoke", spoke);
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(parent.toString());
            wr.flush();
            wr.close();

            if (cookie) {
                this.cookie = con.getHeaderField("set-cookie");
                FirebaseCrash.logcat(Log.DEBUG, TAG, "Cookie: " + this.cookie);
            }

            StringBuilder sb = new StringBuilder();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                FirebaseCrash.logcat(Log.DEBUG, TAG, "Response (" + reqCount + "): " + sb.toString());
                return sb.toString();
            } else if (con.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                login(username, password);
                return request(request, true);
            } else {
                FirebaseCrash.report(new Exception("Status (" + reqCount + "): " + con.getResponseMessage()));
                return "Status: " + con.getResponseCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}