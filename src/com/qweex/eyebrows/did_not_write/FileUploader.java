package com.qweex.eyebrows.did_not_write;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.qweex.eyebrows.MainActivity;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class FileUploader extends AsyncTask<String, Object, Integer>{

    MainActivity activity;
    String uploadServerUri;
    Runnable pre, post;
    Handler prog;

    public FileUploader(MainActivity a, String u,
                        Runnable pre, Handler prog, Runnable post)
    {
        activity = a;
        uploadServerUri = u;
        this.pre  = pre;
        this.prog = prog;
        this.post = post;
    }

    protected void onPreExecute() {
        Log.d("Eyebrows", "OK UPLOADING FILES pre");
        pre.run();
    }

    protected void onProgressUpdate(Object... progress) {
        Log.d("Eyebrows", "OK UPLOADING FILES prog");

        Bundle b = new Bundle();
        b.putLong("totalRead", (Long) progress[0]);
        b.putLong("fileSize", (Long) progress[1]);
        b.putString("fileName", (String) progress[2]);
        Message m = new Message();
        m.setData(b);
        prog.dispatchMessage(m);
    }

    protected void onPostExecute(Integer result) {
        if(result!=200 && result !=0) // Success or Canceled (No Error)
            activity.showErrorDialog("Error occurred: code " + result);

        Log.d("Eyebrows", "OK UPLOADING FILES post");
        post.run();
    }

    protected Integer doInBackground(String... args) {
        Log.d("Eyebrows", "OK UPLOADING FILES do");
        for(String sourceFileUri : args)
        {
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

                Log.e("uploadFile", "Source File not exist :" + sourceFileUri);
                return -1;
            }
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(uploadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setChunkedStreamingMode(1024); //Avoid Out of Memory error
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
                    if(this.isCancelled())
                    {
                        //close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                        return 0;
                    }

                    totalRead += bytesRead;
                    Log.w("Eyebrows:bytesRead", (int) (totalRead * 100 / filesize) + " = " + totalRead + " of " + filesize);
                    this.publishProgress(totalRead, filesize, fileName);
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

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
                return serverResponseCode;

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
        }
        return -1;
    }
}
