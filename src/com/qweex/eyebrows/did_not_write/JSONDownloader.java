package com.qweex.eyebrows.did_not_write;


import android.util.Log;
import com.qweex.eyebrows.EyebrowsError;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

// T is either HttpURLConnection or HttpsURLConnection
public class JSONDownloader {

    public class http {

        HttpURLConnection connection;

        //http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
        //Returns either a JSONArray or JSONObject
        public Object readJsonFromUrl(String authString, String url, Map<String, String> headers) throws IOException, JSONException, EyebrowsError {
            createConnection(url);
            Log.d("JSONDownloader", "Class is: " + connection.getClass().toString());
            if(headers!=null)
                for(String key : headers.keySet()) {
                    connection.setRequestProperty(key, headers.get(key));
                }
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            if(authString!=null)
                connection.setRequestProperty("Authorization", "Basic " + authString);
            int statusCode = connection.getResponseCode();
            if(statusCode!=200 && statusCode!=111)
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
                try {
                    return new JSONArray(jsonText);
                } catch(JSONException e) {}
                return new JSONObject(jsonText);
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
