package com.qweex.eyebrows;

import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import com.qweex.eyebrows.did_not_write.JSONDownloader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StatusUpdater extends AsyncTask<Void, Void, Void> {

    ServerList activity;
    Map<String, JSONObject> results = new HashMap<String, JSONObject>();

    public StatusUpdater(ServerList activity) {
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Cursor c = SavedServers.getAll();
        if(c.getCount()==0)
            return null;
        c.moveToFirst();
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("X-Info", "?");
        while(!c.isAfterLast()) {
            JSONObject j = null;
            try {
                String path_to_load = c.getString(c.getColumnIndex("host")) + ":" + c.getInt(c.getColumnIndex("port"));
                    Log.d("Loading", path_to_load);
                if(c.getInt(c.getColumnIndex("ssl"))>1)
                    j = (JSONObject) (new JSONDownloader().new http()).readJsonFromUrl(null, "https://" + path_to_load, headers);
                else
                    j = (JSONObject) (new JSONDownloader().new https()).readJsonFromUrl(null, "http://" + path_to_load, headers);
            Log.d("RADDA", j + "!");
            } catch(JSONException e) {
                e.printStackTrace();
            } catch(EyebrowsError e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            results.put(c.getString(c.getColumnIndex("name")), j);
            c.moveToNext();
        }

        return null;
    }

    @Override
    public void onPostExecute(Void v) {
        activity.updateStatuses(results);
    }
}
