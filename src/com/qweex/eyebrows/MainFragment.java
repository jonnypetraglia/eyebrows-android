package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.qweex.eyebrows.did_not_write.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// This fragment is a single folder view; it is inserted into MainActivity

public class MainFragment extends Fragment implements ListView.OnItemClickListener,
                                                      ListView.OnItemLongClickListener {
    // Variables retrieved from MainActivity, for the connection
    String host, username, password, authString;
    int port;
    boolean ssl;

    // Specific to this instance
    private List<String> uri_path;

    // internal variables
    private ArrayList<String> imageListing;
    private ListView listview;
    private View LOADING, EMPTY;

    // Called when the view is first created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.main_fragment, container, false);

        // Setup ListView
        LOADING = view.findViewById(android.R.id.progress);
        EMPTY = view.findViewById(android.R.id.empty);
        listview = (ListView)view.findViewById(android.R.id.list);
        listview.setOnItemClickListener(this);
        listview.setOnItemLongClickListener(this);
        listview.setEmptyView(LOADING);

        // Get the data for the first time
        getData();

        return view;
    }

    // Called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get data from server connection (stays the same for all fragments)
        Bundle extras = ((MainActivity)this.getActivity()).getExtras();
        Log.e("Eyebrows:Starting", "Bundle is " + extras);
        host = extras.getString("host");
        port = extras.getInt("port");
        ssl = extras.getBoolean("ssl");
        username = extras.getString("username");
        password = extras.getString("password");

        //Get the path for just this fragment
        uri_path = getArguments().getStringArrayList("uri_path");

        //Adjust data
        authString = Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT);
    }

    // Getter for the path
    public List<String> getPath() { return uri_path; }

    // Returns a string that is the full path for a URL
    public String getPathUrl() {
        List<String> temp_path = new ArrayList<String>();
        for(String s : uri_path)
            temp_path.add(Uri.encode(s));
        return TextUtils.join("/", temp_path);
    }

    public void getData() {
        new DataRetrieval().execute();
    }

    private class DataRetrieval extends AsyncTask<Object, Object, ArrayList<JSONObject>> {

        @Override
        protected ArrayList<JSONObject> doInBackground(Object... not_used) {
            imageListing = new ArrayList<String>();
            ArrayList<JSONObject> folderListing = new ArrayList<JSONObject>();
            try {
                JSONArray folderListingJSON;
                String path_to_load = host + ":" + Integer.toString(port) + "/" + getPathUrl();
                Log.d("Eyebrows:getData", "Loading URL: " + path_to_load);
                if(ssl)
                    folderListingJSON = (new JSONDownloader().new http()).readJsonFromUrl(authString, "https://" + path_to_load);
                else
                    folderListingJSON = (new JSONDownloader().new https()).readJsonFromUrl(authString, "http://" + path_to_load);

                for(int i=0; i<folderListingJSON.length(); i++) {
                    if(((JSONObject)folderListingJSON.get(i)).get("icon")=="picture-o")
                        imageListing.add((String)((JSONObject)folderListing.get(i)).get("filename"));
                    folderListing.add((JSONObject) folderListingJSON.get(i));
                }
                return folderListing;
            } catch (EyebrowsError e) {
                Log.e("Eyebrows:HERPERR e", e.getMessage() + "!");
                showErrorDialog(e.toString());
            } catch (ConnectException e) {
                Log.e("Eyebrows:HERPERR c", e.getMessage() + "!");
                e.printStackTrace();
                showErrorDialog(e.toString());
            } catch (IOException e) {
                Log.e("Eyebrows:HERPERR i", e.getMessage() + "!");
                e.printStackTrace();
                showErrorDialog(e.toString());
            } catch (JSONException e) {
                Log.e("Eyebrows:HERPERR J", e.getMessage());
                showErrorDialog(e.toString());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            listview.setEmptyView(LOADING);
            EMPTY.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(ArrayList<JSONObject> folderListing) {
            if(folderListing!=null && getActivity()!=null)
            {
                EyebrowsAdapter eba = new EyebrowsAdapter(getActivity(), 0, folderListing);
                listview.setAdapter(eba);
            }
            listview.setEmptyView(EMPTY);
            LOADING.setVisibility(View.GONE);
        }
    }

    // Shows an error dialog & quits to the server list
    private void showErrorDialog(final String msg) {
        if(true==true) return;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                ((MainActivity)getActivity()).showErrorDialog(msg);
            }
        });
    }

    //ListView
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {

        ArrayList<String> msg = new ArrayList<String>();
        JSONObject item = (JSONObject) listview.getAdapter().getItem(pos);

        String title = null;
        Button download = null;
        try {
            title = item.getString("name");
        } catch (JSONException e) {}
        try {
            if(EyebrowsAdapter.iconHash.get(item.getString("icon")) != R.drawable.ic_action_folder_closed)
            {
                msg.add("Size:    " + item.getString("size"));
                download = new Button(getActivity());
                download.setText(R.string.download);
                download.setPadding(10,10,10,10);
                download.setTag(title);
                download.setOnClickListener(downloadFromPopup);
            }
        } catch (JSONException e) {}
          catch (NullPointerException e) {}
        try {
            msg.add("Time:    " + new SimpleDateFormat("MMM dd, yyyy hh:mm a").format(new Date(item.getLong("time")*1000)));
        } catch (JSONException e) {}

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(TextUtils.join("\n",msg))
                .setNegativeButton(android.R.string.ok, null)
                .setView(download)
                .show();
        return true;
    }

    View.OnClickListener downloadFromPopup = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                            getPathUrl()));
            startActivityForResult(myIntent, 0);
        }
    };

    //ListView
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
        String filename = ((TextView)view.findViewById(R.id.filename)).getText().toString();
        Log.d("Eyebrows:onItemClick", filename);

        int res_id = (Integer)getView().findViewById(R.id.fileicon).getTag();
        if(res_id == R.drawable.ic_action_folder_closed)
        {
            ArrayList<String> new_path = new ArrayList<String>(uri_path);
            new_path.add(filename);
            ((MainActivity)getActivity()).addFragment(new_path);
            //startNew(this, host, port, ssl, username, password, new_path);
        } else {
            // DOWNLOAD
            Log.d("Eyebrows", "Downloading: " + "http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                    getPathUrl());
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                            getPathUrl()));
            startActivityForResult(myIntent, 0);
        }
    }
}