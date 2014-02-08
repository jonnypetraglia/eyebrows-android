package com.qweex.eyebrows;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EyebrowsAdapter extends ArrayAdapter<JSONObject> {
    List<JSONObject> fileList;
    Context context;
    static final int RES_ID = R.layout.item;

    private static final Map<String, Integer> iconHash;
    static
    {
        iconHash = new HashMap<String, Integer>();
        iconHash.put("archive", R.drawable.ic_action_business);
        iconHash.put("code", R.drawable.ic_action_gear); //?
        iconHash.put("file-text-o", R.drawable.ic_action_document);
        iconHash.put("film", R.drawable.ic_action_movie);
        iconHash.put("music", R.drawable.ic_action_music_1);
        iconHash.put("picture-o", R.drawable.ic_action_picture);
        iconHash.put("file-o", R.drawable.ic_action_tablet);
        iconHash.put("folder", R.drawable.ic_action_folder_closed);
    }

    public EyebrowsAdapter(Context context, int textViewResourceId, List<JSONObject> objects) {
        super(context, textViewResourceId, objects);

        this.fileList = objects;
        this.context = context;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        String name = null, icon = null;
        long time, size = 0;


        JSONObject want = fileList.get(pos);
        try {
            name = want.getString("name");
            icon = want.getString("icon");
            time = want.getLong("time");
            size = want.getLong("size");
        } catch(JSONException e){
            e.printStackTrace();
        }
        if(convertView==null)
            convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(RES_ID, null, false);

        ((TextView)convertView.findViewById(R.id.filename)).setText(name);
        ((TextView)convertView.findViewById(R.id.filesize)).setText(MainActivity.formatBytes(size));


        ImageView iv = ((ImageView)convertView.findViewById(R.id.fileicon));
        if(iconHash.containsKey(icon)) {
            iv.setTag(iconHash.get(icon));
            iv.setImageDrawable(context.getResources().getDrawable(iconHash.get(icon)));

            if(iconHash.get(icon)==R.drawable.ic_action_folder_closed)
                convertView.findViewById(R.id.filesize).setVisibility(View.GONE);
            else
                convertView.findViewById(R.id.filesize).setVisibility(View.VISIBLE);
        }
        else {
            iv.setTag(R.drawable.ic_action_tablet);
            iv.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_tablet));
            convertView.findViewById(R.id.filesize).setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
