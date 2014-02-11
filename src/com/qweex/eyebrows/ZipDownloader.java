package com.qweex.eyebrows;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.qweex.eyebrows.MainActivity.formatBytes;


//TODO: WHAT IF activity IS NULL AT ANY POINT
//TODO: WHAT DO IF FILE EXISTS

public class ZipDownloader extends AsyncTask<Void, File, Void> {
    MainActivity activity;
    NotificationManager notificationManager;
    Notification notification;
    int NOTIFICATION_ID = 1337;
    String[] scale = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "PBJ TIME"};
    int scaleIndex = 0;
    long[] scaleScale;
    long totalSize;
    List<Pair<String, Exception>> errors = new ArrayList<Pair<String, Exception>>();

    public ZipDownloader(MainActivity a) {
        activity = a;
        scaleScale = new long[scale.length];
        scaleScale[0] = 1;
        for(int i=1; i<scale.length; i++)
            scaleScale[i] = scaleScale[i-1] * 1024;
    }

    @Override
    public void onProgressUpdate(File... file) {
        //If it changes by a 100th of whatever unit we are at, update the notification
        if((int)(totalSize*100/scaleScale[scaleIndex]) > (int)(totalSize*100/scaleScale[scaleIndex-1]))
        {
            if(totalSize>=scaleScale[scaleIndex+1])
                scaleIndex+=1;
            updateNotification(file[0].getName(), formatBytes(totalSize));
        }
    }

    @Override
    protected void onPreExecute() {
        notificationManager = (NotificationManager) activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        updateNotification(null, null);
    }

    @Override
    protected void onPostExecute(Void v) {
        notificationManager.cancel(NOTIFICATION_ID);
        activity.zipDownloader = null;
        activity.showErrorDialog(errors.size() + " errors occured while trying to download");
        //TODO: Do something (better) with errors
    }

    @Override
    protected Void doInBackground(Void... voids) {

        Pair<String, List<String>> entity = activity.getZipUrlToDownload();
        String subfolder = null;
        List<String> items;

        while(entity!=null) {
            try {
                // Create the params for the POST
                List<NameValuePair> params = new ArrayList<NameValuePair>(2);
                subfolder = entity.first;
                items = entity.second;
                params.add(new BasicNameValuePair("d", "1"));
                params.add(new BasicNameValuePair("subfolder", subfolder));
                for(String item : items)
                    params.add(new BasicNameValuePair("items", item));

                // Initialized the notification for this file
                Log.d("Downloading", subfolder + " " + items);
                updateNotification(subfolder + ".zip", "0 bytes");

                // Create the output file's object
                File outputFile = new File(Environment.getExternalStorageDirectory(),
                        PreferenceManager.getDefaultSharedPreferences(activity).getString("download_dir", "Eyebrows"));
                if(subfolder.length()>0)
                    outputFile = new File(outputFile, "Home.zip");
                else
                    outputFile = new File(outputFile, subfolder + ".zip");

                // Make the post
                InputStream i = makePost(params);

                // Write the file
                writeFile(outputFile, i);

                // Add the Completed notification to the DownloadManager
                activity.downloader.addCompletedDownload(
                        subfolder + ".zip", //Title
                        activity.getResources().getString(R.string.download_complete), //Desc
                        false, //isMediaScannable
                        URLConnection.guessContentTypeFromName(outputFile.getName()), //mimetype
                        outputFile.getAbsolutePath(), // path
                        outputFile.length(), //length
                        true); //ShowNotification
            } catch(Exception e) {
                errors.add(Pair.create(subfolder, e));
                e.printStackTrace();
            }
            entity = activity.getZipUrlToDownload();
        }
        return null;
    }

    // Update notification for the current file & amount downloaded
    private void updateNotification(String fileName, String fileSize) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(activity)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(fileName)
                        .setContentInfo(fileSize)
                        .setOngoing(true)
                        .setProgress(0, 1, true);
        notification = mBuilder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    // I got this from somewhere on the internet
    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    // Make the post request & return its resultant stream
    private InputStream makePost(List<NameValuePair> params) throws IOException {
        Log.d("Downloading", "Post: " + activity.getBaseUrl() + "~");
        URL url = new URL(activity.getBaseUrl() + "~");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //conn.setDoOutput(true);
        //conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authentication", "Basic " + activity.extras.getString("auth"));
        conn.setUseCaches(false);
        Log.d("Downloading", "Post: 2~");

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
        conn.connect();

        Log.d("Downloading", "Post: 3~");
        return conn.getInputStream();
    }

    // Write the file from the stream to the disk
    private void writeFile(File outputFile, InputStream is) throws IOException {
        Log.d("Downloading", "Writing file to " + outputFile.getAbsoluteFile());


        BufferedInputStream bufferinstream = new BufferedInputStream(is);
        int bufferSize = 1024;
        ByteArrayBuffer baf = new ByteArrayBuffer(bufferSize);
        int current = 0;
        totalSize = 0;
        while((current = bufferinstream.read()) != -1){
            totalSize += 1;
            baf.append((byte) current);
            publishProgress(outputFile);

            //TODO: Need a way to determine if server has suddenly died
        }
        bufferinstream.close();
        Log.d("Downloading", "Writing fos");
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(baf.toByteArray());
        fos.flush();
        fos.close();
    }
}