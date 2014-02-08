package com.qweex.eyebrows;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class FileUploader {

    static Activity activity;

    // Uploads a file to the server
    public static int uploadFile(Activity activity, String sourceFileUri, String upLoadServerUri) {
        FileUploader.activity = activity;
        ProgressDialog dialog = ProgressDialog.show(activity, "Uploading...", sourceFileUri);

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

    // Shows a toast. TOAAAAAAST
    static void showToast(final String msg) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT);
            }
        });
    }
}
