package com.qweex.eyebrows;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

//TODO: Notification for uploading


public class MainFragment extends Fragment implements Spinner.OnItemSelectedListener,
                                                      ListView.OnItemClickListener,
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
    private boolean spinnerShown = false;
    private List<String> spinnerVar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.main_fragment, container, false);

        listview = (ListView)view.findViewById(android.R.id.list);
        listview.setOnItemClickListener(this);
        listview.setOnItemLongClickListener(this);
        listview.setEmptyView(view.findViewById(android.R.id.empty));

        // Get the data for the first time
        getData();

        return view;
    }

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
        spinnerVar = new ArrayList<String>(uri_path);
        spinnerVar.add(0, getResources().getString(R.string.home));
        authString = Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT);

        Log.d("Eyebrows:MainFragment", "New with: " + uri_path);
    }

    private Handler uploadFiles = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.getData()==null || msg.getData().getStringArrayList("files")==null)
                return;
            ArrayList<String> newDownloads = msg.getData().getStringArrayList("files");
            String uploadPath = "http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" + getPathUrl();
            Log.d("Eyebrows:uploadFiles", uploadPath);
            for(String f : newDownloads) {
                Log.d("Eyebrows:UPLOADING", f);
                FileUploader.uploadFile(getActivity(), f, uploadPath);
            }
        }
    };

    public void onStart() {
        super.onStart();

        // Setup Spinner
        Spinner path_spinner = (Spinner) getView().findViewById(R.id.path_spinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.spinner_list,
                spinnerVar);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner);
        path_spinner.setAdapter(spinnerAdapter);
        path_spinner.setSelection(spinnerVar.size()-1);

        path_spinner.setOnItemSelectedListener(this);
        path_spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                spinnerShown = true;
                return false;
            }
        });
    }

    // Returns a string that is the full path
    public String getPathUrl() {
        List<String> temp_path = new ArrayList<String>();
        for(String s : uri_path)
            temp_path.add(Uri.encode(s));
        return TextUtils.join("/", temp_path);
    }

    // Gets data from the server & populates the listview
    public void getData() {
        imageListing = new ArrayList<String>();
        try {
            JSONArray folderListing;
            String path_to_load = host + ":" + Integer.toString(port) + "/" + getPathUrl();
            Log.d("Eyebrows:getData", "Loading URL: " + path_to_load);
            if(ssl)
                folderListing = JSONDownloader.http.readJsonFromUrl(authString, "https://" + path_to_load);
            else
                folderListing = JSONDownloader.https.readJsonFromUrl(authString, "http://" + path_to_load);

            for(int i=0; i<folderListing.length(); i++) {
                if(((JSONObject)folderListing.get(i)).get("icon")=="picture-o")
                    imageListing.add((String)((JSONObject)folderListing.get(i)).get("filename"));
            }

            EyebrowsAdapter eba = new EyebrowsAdapter(getActivity(), 0, JsonArrayToArrayList(folderListing));
            listview.setAdapter(eba);
            Log.d("Eyebrows:HERP3", folderListing.toString());
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
    }

    private void showErrorDialog(String msg) { ((MainActivity)getActivity()).showErrorDialog(msg);}



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
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                            getPathUrl()));
            startActivityForResult(myIntent, 0);
        }
    }

    //ListView
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long l) {
        return false;
    }

    //Spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        if(spinnerShown)
        {
            Log.d("Eyebrows:onItemSelected", pos + "!" + uri_path.size());
            ((MainActivity)getActivity()).popFragmentsOrFinish(uri_path.size() - pos - 1);
            spinnerShown = false;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        spinnerShown = false;
    }
}