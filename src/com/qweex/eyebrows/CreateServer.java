package com.qweex.eyebrows;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


public class CreateServer extends Activity implements View.OnClickListener{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.create_server);
        setContentView(R.layout.create_activity);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);


        Bundle extras = getIntent().getExtras();
        if(extras==null)
            return;

        //Test Data
        ((TextView)findViewById(R.id.host)).setText(extras.getString("host"));
        ((TextView)findViewById(R.id.port)).setText(Integer.toString(extras.getInt("port")));
        ((CheckBox)findViewById(R.id.ssl)).setChecked(extras.getBoolean("ssl"));
        ((TextView)findViewById(R.id.username)).setText(extras.getString("username"));
        ((TextView)findViewById(R.id.password)).setText(extras.getString("password"));
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.save && ((TextView)findViewById(R.id.name)).getText().length()==0)
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.a_name_is_required)
                    .setNeutralButton(android.R.string.ok, null)
                    .show();
            return;
        }

        Bundle bundle = new Bundle();
        try {
            String host = ((TextView)findViewById(R.id.host)).getText().toString();
            if(host.isEmpty())
                throw new Exception();
            int port = Integer.parseInt(((TextView)findViewById(R.id.port)).getText().toString());
            boolean ssl = ((CheckBox)findViewById(R.id.ssl)).isChecked();
            String username = ((TextView)findViewById(R.id.username)).getText().toString();
            String password = ((TextView)findViewById(R.id.password)).getText().toString();

            bundle.putString("host", host);
            bundle.putInt("port", port);
            bundle.putBoolean("ssl", ssl);
            bundle.putString("username", username);
            bundle.putString("password", password);
        } catch(Exception nfe) {
            Log.e("Eyebrows:createServer", "Insufficient: " + nfe.toString());
            Toast.makeText(view.getContext(), R.string.insufficient_info, Toast.LENGTH_SHORT);
            return;
        }

        Log.e("Eyebrows:Starting", "Creating Bundle is " + bundle);
        if(view.getId() == R.id.save)
            SavedServers.add(bundle);
        Intent intent = new Intent(CreateServer.this, MainActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
