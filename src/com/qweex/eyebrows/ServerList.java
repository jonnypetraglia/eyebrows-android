package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.qweex.utils.Crypt;

public class ServerList extends ListActivity implements AdapterView.OnItemLongClickListener {
    LongClickMenu longClickMenu;

    private void initiliazeApp() {
        SavedServers.initialize(this);
        Crypt.setSalt(PreferenceManager.getDefaultSharedPreferences(this).getString("salt", null));
        Crypt.setAlgorithm(PreferenceManager.getDefaultSharedPreferences(this).getString("algorithm", "AES"));
        Crypt.setKeySize(PreferenceManager.getDefaultSharedPreferences(this).getInt("key_size", 256));
        Crypt.setSaltLength(PreferenceManager.getDefaultSharedPreferences(this).getInt("salt_length", 20));
        Crypt.setIterationCount(PreferenceManager.getDefaultSharedPreferences(this).getInt("iteration_count", 1000));

        if(UserConfig.hasMasterPass(this))
        {
            startActivityForResult(new Intent(this, UnlockApp.class), UnlockApp.class.hashCode());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initiliazeApp();


        getListView().setBackgroundColor(getResources().getColor(android.R.color.white));
        ((View)getListView().getParent()).setBackgroundColor(getResources().getColor(android.R.color.white));

        getListView().setOnItemLongClickListener(this);
        getListView().setEmptyView(getLayoutInflater().inflate(R.layout.empty_servers, null));
        ((ViewGroup)getListView().getParent()).addView(getListView().getEmptyView());
        getListView().setDivider(getResources().getDrawable(R.drawable.divider));
        getListView().setDividerHeight(3);

        longClickMenu = new LongClickMenu(this);
    }

    // Update the list of the user renamed a server
    @Override
    public void onResume() {
        super.onResume();
        setListAdapter(new SimpleCursorAdapter(this, R.layout.server_item,
                SavedServers.getAll(),
                new String[] {"name"}, new int[] {android.R.id.text1}));
    }


    // Show context menu for a Server
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        String name = ((TextView)view.findViewById(android.R.id.text1)).getText().toString();
        longClickMenu.show(name);
        return true;
    }

    // Connect to a Server
    @Override
    public void onListItemClick(ListView l, View v, int position, long i)
    {
        String name = ((TextView) v.findViewById(android.R.id.text1)).getText().toString();

        Intent intent = new Intent(ServerList.this, MainActivity.class);
        intent.putExtras(SavedServers.get(name));
        startActivity(intent);
    }

    // Creates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu u) {
        u.add(0, 0, 0, R.string.connect);
        u.add(0, 1, 0, R.string.preferences);
        u.add(0, 2, 0, R.string.about);
        return super.onCreateOptionsMenu(u);
    }


    // Called when an options menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i = null;
        switch(item.getItemId())
        {
            case 0: //connect
                i = new Intent(ServerList.this, CreateServer.class);
                break;
            case 1: //preferences
                i = new Intent(ServerList.this, UserConfig.class);
                break;
            case 2: //about
                i = new Intent(ServerList.this, AboutActivity.class);
                break;

        }
        startActivity(i);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", resultCode + "!" + requestCode);
        if(requestCode==UnlockApp.class.hashCode() && resultCode!=RESULT_OK)
            finish();
    }


    // Dialog to show when a user long presses a Server.
    class LongClickMenu implements AdapterView.OnItemClickListener{
        Dialog d;
        String name;

        public void show(final String name) {
            this.name = name;
            d.show();
        }

        public LongClickMenu(Context that) {
            AlertDialog.Builder builder = new AlertDialog.Builder(that);

            ListView list = new ListView(that);
            String[] stringArray = new String[] { getResources().getString(R.string.edit),
                    getResources().getString(R.string.delete) };
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(that, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
            list.setAdapter(modeAdapter);
            list.setOnItemClickListener(this);
            builder.setView(list);

            d = builder.create();
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            d.dismiss();
            switch(i) {
                case 0: //edit
                    Intent intent = new Intent(ServerList.this, CreateServer.class);
                    Bundle b = new Bundle();
                    b.putString("name", name);
                    intent.putExtras(b);
                    startActivity(intent);
                    break;
                case 1: //delete
                    new AlertDialog.Builder(ServerList.this)
                            .setTitle(name)
                            .setMessage(R.string.confirm_delete)
                            .setNegativeButton(R.string.no, null)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SavedServers.remove(name);
                                    onResume();
                                }
                            })
                            .show();
                    break;
            }
        }
    }

}
