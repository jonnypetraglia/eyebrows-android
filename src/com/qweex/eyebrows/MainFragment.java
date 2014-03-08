package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;
import com.qweex.eyebrows.did_not_write.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.*;

// This fragment is a single folder view; it is inserted into MainActivity

public class MainFragment extends Fragment implements ListView.OnItemClickListener,
                                                      ListView.OnItemLongClickListener {
    // Variables retrieved from MainActivity, for the connection
    String host, auth;
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
        auth = extras.getString("auth");

        //Get the path for just this fragment
        uri_path = getArguments().getStringArrayList("uri_path");
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
                    folderListingJSON = (JSONArray) (new JSONDownloader().new http()).readJsonFromUrl(auth, "https://" + path_to_load, null);
                else
                    folderListingJSON = (JSONArray) (new JSONDownloader().new https()).readJsonFromUrl(auth, "http://" + path_to_load, null);

                for(int i=0; i<folderListingJSON.length(); i++) {
                    JSONObject j = (JSONObject) folderListingJSON.get(i);
                    folderListing.add(j);
                    if("picture-o".equals(j.get("icon")))
                        imageListing.add((j.getString("name")));
                }
                if(!PreferenceManager.getDefaultSharedPreferences(MainFragment.this.getActivity()).getBoolean("sort_folders_first", true))
                    Collections.sort(folderListing, new Comparator<JSONObject>() {
                        @Override
                        public int compare(JSONObject o1, JSONObject o2) {
                            try {
                            return o1.getString("name").toLowerCase().compareTo(o2.getString("name").toLowerCase());
                            } catch(JSONException e) {}
                            return 0;
                        }
                    });
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
            }  catch (NullPointerException e) {
                Log.e("Eyebrows:HERPERR i", e.getMessage() + "!");
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

        TableLayout layout = new TableLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(getResources().getColor(android.R.color.white));
        layout.setPadding(30,30,30,30);

        String title = null;
        Button download = null;
        try {
            title = item.getString("name");
        } catch (JSONException e) {}
        try {
            if(EyebrowsAdapter.iconHash.get(item.getString("icon")) != R.drawable.ic_action_folder_closed)
            {
                layout.addView(popupRow("Size:", MainActivity.formatBytes(item.getLong("size"))));

                download = new Button(getActivity());
                download.setText(R.string.download);
                download.setTag(title);
                download.setPadding(0,0,0,0);
                download.setTag(title);
                download.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ArrayList<String> derp = new ArrayList<String>();
                        derp.add((String) view.getTag());
                        download(derp, true);
                    }
                });
            }
        } catch (JSONException e) {}
          catch (NullPointerException e) {}


        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
        try {
            layout.addView(popupRow("Modified:", sdf.format(new Date(item.getLong("mtime")*1000))));
        } catch (JSONException e) {}
        try {
            layout.addView(popupRow("Accessed:", sdf.format(new Date(item.getLong("atime")*1000))));
        } catch (JSONException e) {}
        try {
            layout.addView(popupRow("Created:", sdf.format(new Date(item.getLong("ctime")*1000))));
        } catch (JSONException e) {}

        if(download!=null)
            layout.addView(download);

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setInverseBackgroundForced(true)
                .setNegativeButton(android.R.string.ok, null)
                .setView(layout)
                .show();
        return true;
    }

    TableRow popupRow(String colA, String colB) {
        TableRow row = new TableRow(getActivity());

        TextView header = new TextView(getActivity());
        header.setTextColor(getResources().getColor(android.R.color.black));
        header.setTextSize(getResources().getDisplayMetrics().density * 9f);
        header.setText(colA);
        header.setPadding(0, 0, 30, 0);
        header.setTypeface(null, Typeface.BOLD);
        row.addView(header);

        TextView contents = new TextView(getActivity());
        contents.setTextColor(getResources().getColor(android.R.color.black));
        contents.setTextSize(getResources().getDisplayMetrics().density * 9f);
        contents.setText(colB);
        row.addView(contents);

        return row;
    }



    //ListView
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
        final String filename = ((TextView)view.findViewById(R.id.filename)).getText().toString();
        Log.d("Eyebrows:onItemClick", filename);

        int res_id = (Integer)view.findViewById(R.id.fileicon).getTag();
        Log.d("Eyebrows", "Downloading? " + res_id + "=?" + R.drawable.ic_action_picture);
        switch(res_id) {
            case R.drawable.ic_action_folder_closed:
                ArrayList<String> new_path = new ArrayList<String>(uri_path);
                new_path.add(filename);
                ((MainActivity)getActivity()).addFragment(new_path);
                break;
            case R.drawable.ic_action_picture:
                Intent i = new Intent(getActivity(), PictureViewer.class);
                Bundle b = new Bundle();
                b.putStringArrayList("images", imageListing);
                b.putString("path", getBaseUrl());
                b.putString("filename", filename);
                b.putString("authString", auth);
                b.putBoolean("ssl", ssl);
                i.putExtras(b);
                startActivity(i);
                break;
            default:
                Log.d("Eyebrows", "Downloading: " + getBaseUrl() + " | " + getPathUrl() + " | " + filename);
                ArrayList<String> derp = new ArrayList<String>();
                derp.add(filename);
                download(derp, true);
        }
    }

    public synchronized void downloadZip() {
        MainActivity a = ((MainActivity)getActivity());
        String subfolder;
        if(uri_path.size()>0)
            subfolder = uri_path.get(uri_path.size()-1);
        else
            subfolder = "";
        a.zipsToDownload.add(
                Pair.create(subfolder, getAllFiles())
        );
        if(a.zipDownloader==null) {
            a.zipDownloader = new ZipDownloader(a);
            a.zipDownloader.execute();
        }
    }

    public void download(ArrayList<String> files, boolean showInUi) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString("downloadPath", getPathUrl());
        b.putStringArrayList("files", files);
        b.putBoolean("showInUi", showInUi);
        m.setData(b);
        ((MainActivity)getActivity()).downloadFilesHandler.sendMessage(m);
    }

    public List<String> getAllFiles() {
        try {
            ArrayList<String> derp = new ArrayList<String>(listview.getAdapter().getCount());
            for(int i=0; i<listview.getAdapter().getCount(); i++) {
                String file = ((JSONObject)listview.getAdapter().getItem(i)).getString("name");
                derp.add(file);
            }
            return derp;
        } catch(JSONException e) {
            return null;
        }
    }

    String getBaseUrl() {
        return "http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/";
    }
}