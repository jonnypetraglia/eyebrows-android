package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import com.qweex.utils.Crypt;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

class AsyncCrypt extends AsyncTask<String, Void, Exception> {
    Dialog dialog;
    enum Task {ENCRYPTING, DECRYPTING, CHANGING}
    Task t;
    Context c;
    Handler callback;

    public AsyncCrypt(Context c, Task t, Handler callback) throws IllegalArgumentException {
        this.t = t;
        this.c = c;
        this.callback = callback;
        if(callback==null || c==null)
            throw new IllegalArgumentException();
    }

    @Override
    protected void onPreExecute() {
        ProgressBar herp = new ProgressBar(c);
        if(SavedServers.getAll().getCount()>0)
            dialog = new AlertDialog.Builder(c)
                    .setTitle(t.toString())
                    .setView(herp)
                    .setCancelable(false)
                    .show();
    }

    @Override
    protected void onPostExecute(Exception error) {
        if(dialog!=null)
            dialog.dismiss();
        Message m = new Message();
        Bundle b = new Bundle();
        b.putSerializable("error", error);
        callback.sendMessage(m);
    }

    // arg[] = {password[, old_password]}
    @Override
    protected Exception doInBackground(String... strings) {
        try {
            if(t==Task.CHANGING) {
                doTask(Task.DECRYPTING, strings[1]);
                doTask(Task.ENCRYPTING, strings[0]);
            } else {
                doTask(t, strings[0]);
            }
            return null;
        } catch(Exception e) { return e; }
    }

    private void doTask(Task task, String password) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        UserConfig.masterKey = Crypt.getKeyFromPassword(password);
        Cursor c = SavedServers.getAll();
        if(c.getCount()==0)
            return;
        c.moveToFirst();

        while(!c.isAfterLast()) {
            String auth = c.getString(c.getColumnIndex("auth"));
            if(task==Task.ENCRYPTING)
                auth = Crypt.encrypt(auth, UserConfig.masterKey);
            else if(task==Task.DECRYPTING)
                auth = Crypt.decrypt(auth, UserConfig.masterKey);
            else
                throw new RuntimeException("Task was CHANGING inside of doTask");
            Bundle b = new Bundle();
            b.putString("name", c.getString(c.getColumnIndex("name")));
            b.putString("auth", auth);
            SavedServers.update(this.c, c.getLong(c.getColumnIndex("_id")), b);
            c.moveToNext();
        }
    }
}
