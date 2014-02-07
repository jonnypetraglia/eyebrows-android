package com.qweex.eyebrows;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.qweex.utils.FilePickerDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//TODO: Notification for uploading


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
    private ArrayList<String> imageListing;


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
        this.findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FilePickerDialog(MainActivity.this, uploadFiles);
            }
        });

        // Setup ListView
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        getListView().setEmptyView(findViewById(android.R.id.empty));

        // Get the data for the first time
        getData();
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
                MainActivity.this.uploadFile(f, uploadPath);
            }
        }
    };

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

    private String getPathUrl() {
        List<String> temp_path = new ArrayList<String>();
        for(String s : uri_path)
            temp_path.add(Uri.encode(s));
        temp_path.remove(0);
        return TextUtils.join("/", temp_path);
    }

    private void getData() {
        imageListing = new ArrayList<String>();
        try {
            JSONArray folderListing;
            String path_to_load = host + ":" + Integer.toString(port) + "/" + getPathUrl();
            Log.d("Eyebrows:getData", "Loading URL: " + path_to_load);
            if(ssl)
                folderListing = readJsonFromSslUrl("https://" + path_to_load);
            else
                folderListing = readJsonFromUrl("http://" + path_to_load);

            for(int i=0; i<folderListing.length(); i++) {
                if(((JSONObject)folderListing.get(i)).get("icon")=="picture-o")
                    imageListing.add((String)((JSONObject)folderListing.get(i)).get("filename"));
            }

            setListAdapter(new EyebrowsAdapter(this, 0, JsonArrayToArrayList(folderListing)));
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
        Log.w("Eyebrows:AKLDHIUFLDEH", imageListing.toString());
    }


    private void showErrorDialog(String msg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error);
        builder.setMessage(msg);
        builder.setNeutralButton(android.R.string.ok, null);
        builder.show().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                MainActivity.this.finish();
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu u) {
        MenuItem mu = u.add(0,0,0,"Download as ZIP");
        mu = u.add(0,1,0, "Refresh");
        mu = u.add(0,2,0, "Exit to Server List");
        return super.onCreateOptionsMenu(u);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case 0:
                //download as zip
                return true;
            case 1:
                getData();
                return true;
            case 2:
                // If we are asked to finish ourselves, pass it on ...
                setResult(RESULT_OK, new Intent().setAction("butts"));
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);

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
            // DOWNLOAD
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http" + (ssl ? "s" : "") + "://" + host + ":" + port + "/" +
                            getPathUrl()));
            startActivityForResult(myIntent, 0);
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
        ((Activity)context).startActivityForResult(intent, 0);
        //context.startActivity(intent);
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

    // When it has been asked to finish;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ACTIVITY RESULT: ", requestCode + " " + resultCode);
        if(data!=null && "butts".equals(data.getAction()))
        {
            setResult(RESULT_OK, new Intent().setAction("butts"));
            finish();
        }
    }

    // Shows a toast. TOAAAAAAST
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT);
            }
        });
    }

    // Uploads a file to the server
    public int uploadFile(String sourceFileUri, String upLoadServerUri) {

        ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "Uploading...", sourceFileUri);

        String fileName = new File(sourceFileUri).getName();
        int serverResponseCode = 0;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            dialog.dismiss();
            Log.e("uploadFile", "Source File not exist :" + sourceFileUri);
            showToast("Source File does not exist");
            return 0;
        }
        else
        {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                // qqfilename
                dos.writeBytes("Content-Disposition: form-data; name=\"qqfilename\"" + lineEnd
                         + lineEnd  + fileName + lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                // qqtotalfilesize
                dos.writeBytes("Content-Disposition: form-data; name=\"qqtotalfilesize\"" + lineEnd
                        + lineEnd + String.valueOf(sourceFile.length()) + lineEnd);
                // qquuid
                dos.writeBytes("Content-Disposition: form-data; name=\"qquuid\"" + lineEnd
                        + lineEnd + String.valueOf(UUID.randomUUID()) + lineEnd);
                // qqfile
                dos.writeBytes("Content-Disposition: form-data; name=\"qqfile\";filename=\"" + fileName + "\""
                        + lineEnd + lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                long filesize = sourceFile.length();
                long totalRead = 0;
                while (bytesRead > 0) {
                    totalRead += bytesRead;
                    Log.w("Eyebrows:bytesRead", (int)(totalRead*100/filesize) + " = " + totalRead + " of " + filesize);
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){
                    showToast("File Upload Completed.");
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();
                showToast("MalformedURLException");
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                showToast("Got Exception : see logcat ");
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }
}