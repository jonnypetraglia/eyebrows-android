package com.qweex.eyebrows.did_not_write;


import android.util.Log;
import com.qweex.eyebrows.EyebrowsError;
import org.json.JSONArray;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

// T is either HttpURLConnection or HttpsURLConnection
public class JSONDownloader {

    public class http {

        HttpURLConnection connection;

        //http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
        public JSONArray readJsonFromUrl(String authString, String url) throws IOException, JSONException, EyebrowsError {
            createConnection(url);
            Log.d("JSONDownloader", "Class is: " + connection.getClass().toString());
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            if(authString!=null)
                connection.setRequestProperty("Authorization", "Basic " + authString);
            int statusCode = connection.getResponseCode();
            if(statusCode!=200)
                throw new EyebrowsError(statusCode, authString!=null);
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }
                String jsonText = sb.toString();
                Log.v("Eyebrows:HERP", jsonText);
                JSONArray json = new JSONArray(jsonText);
                return json;
            } finally {
                is.close();
            }
        }
        private void createConnection(String url) throws IOException {
            connection = (HttpURLConnection) new URL(url).openConnection();
        }
    }

    public class https extends http {
        HttpsURLConnection connection;
        private void createConnection(String url) throws IOException {
            connection = (HttpsURLConnection)(new URL(url).openConnection());
        }
    }
}
