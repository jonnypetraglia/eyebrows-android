package com.qweex.eyebrows;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements Spinner.OnItemSelectedListener,
                                                      ListView.OnItemClickListener,
                                                      ListView.OnItemLongClickListener {
    // Settings per server
    private String host;
    private int port;
    private boolean ssl;
    private String username;
    private String password;

    // Specific to this instance
    private List<String> uri_path;

    // internal variables
    private String authString;
    private boolean initializingSpinner = true;



    @Override
    public void finish() {
        super.finish();
        //overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        //Get data
        Bundle extras = getIntent().getExtras();
        host = extras.getString("host");
        port = extras.getInt("port");
        ssl = extras.getBoolean("ssl");
        username = extras.getString("username");
        password = extras.getString("password");
        uri_path = extras.getStringArrayList("uri_path");


        //Adjust data
        uri_path.add(0, "Home");
        authString = Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT);

        // Setup Views
        setContentView(R.layout.main);

        this.findViewById(R.id.up_level).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                //if(uri_path.size()>1)
                    //uri_path.remove(uri_path.size()-1);
                //getData();
            }
        });

        // Setup ListView
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setEmptyView(findViewById(android.R.id.empty));

        // Get the data for the first time
        getData();
    }

    public void onStart() {
        super.onStart();


        // Setup Spinner
        Spinner path_spinner = (Spinner) findViewById(R.id.path_spinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                R.layout.spinner_list,
                uri_path);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner);
        path_spinner.setAdapter(spinnerAdapter);
        path_spinner.setSelection(uri_path.size()-1);

        path_spinner.setOnItemSelectedListener(this);
    }


    private void getData() {
        try {
            JSONArray folderListing;
            List<String> temp_path = new ArrayList<String>();
            for(String s : uri_path)
                temp_path.add(Uri.encode(s));
            Log.d("Eyebrows:getData", temp_path.toString() + "!");
            temp_path.remove(0);
            Log.d("Eyebrows:getData", temp_path.toString() + "!");
            String path_to_load = host + ":" + Integer.toString(port) + "/" + TextUtils.join("/", temp_path);
            Log.d("Eyebrows:getData", "Loading URL: " + path_to_load);
            if(ssl)
                folderListing = readJsonFromSslUrl("https://" + path_to_load);
            else
                folderListing = readJsonFromUrl("http://" + path_to_load);

            setListAdapter(new EyebrowsAdapter(this, 0, JsonArrayToArrayList(folderListing)));
            Log.d("Eyebrows:HERP3", folderListing.toString());
        } catch (EyebrowsError e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
            Log.e("Eyebrows:HERPERR e", e.getMessage() + "!");
        } catch (ConnectException e) {
            Log.e("Eyebrows:HERPERR c", e.getMessage() + "!");
            e.printStackTrace(); // WRONG PASSWORD?
        } catch (IOException e) {
            Log.e("Eyebrows:HERPERR i", e.getMessage() + "!");
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e("Eyebrows:HERPERR J", e.getMessage());
            e.printStackTrace();
        }
    }


    //http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
    public JSONArray readJsonFromSslUrl(String url) throws IOException, JSONException, EyebrowsError {
        HttpsURLConnection connection = (HttpsURLConnection)(new URL(url).openConnection());
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Basic " + authString);
        int statusCode = connection.getResponseCode();
        if(statusCode!=200)
            throw new EyebrowsError(statusCode, this);
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

    public JSONArray readJsonFromUrl(String url) throws IOException, JSONException, EyebrowsError {
        HttpURLConnection connection = (HttpURLConnection)(new URL(url).openConnection());
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Basic " + authString);
        Log.d("Eyebrows:HERP", connection.getResponseMessage());
        int statusCode = connection.getResponseCode();

        if(statusCode!=200)
            throw new EyebrowsError(statusCode, this);
        InputStream is = connection.getInputStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    private List<JSONObject> JsonArrayToArrayList(JSONArray jsonArray) throws JSONException {
        List<JSONObject> list = new ArrayList<JSONObject>();
        for (int i=0; i<jsonArray.length(); i++) {
            list.add((JSONObject) jsonArray.get(i));
        }
        return list;
    }

    public boolean requiresCredentials() {
        return username != null && password != null;
    }

    //Utility
    private ListView getListView() {
        return (ListView)findViewById(android.R.id.list);
    }

    //Utility
    private void setListAdapter(EyebrowsAdapter ea) {
        getListView().setAdapter(ea);
    }

    //ListView
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
        String filename = ((TextView)view.findViewById(R.id.filename)).getText().toString();
        Log.d("Eyebrows:onItemClick", filename);

        int res_id = (Integer)findViewById(R.id.fileicon).getTag();
        if(res_id == R.drawable.ic_action_folder_closed)
        {
            ArrayList<String> new_path = new ArrayList<String>(uri_path);
            if(new_path.size()>0)
                new_path.remove(0);
            new_path.add(filename);
            startNew(this, host, port, ssl, username, password, new_path);
        } else {
            ArrayList<String> new_path = new ArrayList<String>(uri_path);
            if(new_path.size()>0)
                new_path.remove(0);
            new_path.add(filename);
            // DOWNLOAD
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                            TextUtils.join("/", new_path)));
            startActivity(myIntent);
        }
    }

    public static void startNew(Context context,
                           String _host, int _port, boolean _ssl,
                           String _username, String _password,
                           ArrayList<String> _uri_path) {
        Intent intent = new Intent(context, MainActivity.class);
        // Aw yeah, let's BUNDLE THAT SHIT
        Bundle extras = new Bundle();
        extras.putString("host", _host);
        extras.putInt("port", _port);
        extras.putBoolean("ssl", _ssl);
        extras.putString("username", _username);
        extras.putString("password", _password);
        extras.putStringArrayList("uri_path", _uri_path);
        Log.d("Eyebrows:StaringNew", _uri_path.toString() + "!");

        intent.putExtras(extras);
        context.startActivity(intent);
    }

    //ListView
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
        return false;
    }

    //Spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        if(!initializingSpinner)
        {
            Log.d("Eyebrows:onItemSelected", pos + "!" + uri_path.size());
            if(pos< uri_path.size()-1) {
                for(int i=0; i< uri_path.size()-pos; i++)
                    uri_path.remove(uri_path.size()-1);
            }
            getData();
        }
        initializingSpinner = false;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}
}