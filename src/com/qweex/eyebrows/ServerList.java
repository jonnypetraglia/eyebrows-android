package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;


public class ServerList extends ListActivity implements AdapterView.OnItemLongClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        SavedServers.initialize(this);

        getListView().setBackgroundColor(getResources().getColor(android.R.color.white));
        ((View)getListView().getParent()).setBackgroundColor(getResources().getColor(android.R.color.white));


        getListView().setEmptyView(getLayoutInflater().inflate(R.layout.empty_servers, (ViewGroup) getListView().getParent()));
        getListView().setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                SavedServers.getAllNames())
        );
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String name = ((TextView)view.findViewById(android.R.id.text1)).getText().toString();

        ListView list = new ListView(this);
        String[] stringArray = new String[] { getResources().getString(R.string.edit),
                                              getResources().getString(R.string.delete) };
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
        list.setAdapter(modeAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch(i) {
                    case 1: //edit
                        Intent intent = new Intent(ServerList.this, CreateServer.class);
                        intent.putExtras(SavedServers.get(name));
                        startActivity(intent);
                        break;
                    case 2: //delete
                        new AlertDialog.Builder(ServerList.this)
                                .setTitle(name)
                                .setMessage(R.string.confirm_delete)
                                .setNegativeButton(R.string.no, null)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        SavedServers.remove(name);
                                    }
                                })
                                .show();
                        break;
                }
            }
        });
        builder.setView(list);

        builder.show();
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        String name = ((TextView)v).getText().toString();

        Intent intent = new Intent(ServerList.this, MainActivity.class);
        intent.putExtras(SavedServers.get(name));
        startActivity(intent);
    }

    // Creates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu u) {
        u.add(0,0,0, R.string.connect).setOnMenuItemClickListener(connect);
        u.add(0,1,0, R.string.about).setOnMenuItemClickListener(about);
        return super.onCreateOptionsMenu(u);
    }


    MenuItem.OnMenuItemClickListener connect = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent i = new Intent(ServerList.this, CreateServer.class);
            startActivity(i);
            return true;
        }
    };



    MenuItem.OnMenuItemClickListener about = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent i = new Intent(ServerList.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
    };
}
