package com.qweex.eyebrows;

import android.app.Activity;
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
        setContentView(R.layout.create_activity);
        findViewById(R.id.save).setOnClickListener(this);


        //Test Data
        ((TextView)findViewById(R.id.host)).setText("192.168.0.112");
        ((TextView)findViewById(R.id.port)).setText("9002");
        ((CheckBox)findViewById(R.id.ssl)).setChecked(false);
        ((TextView)findViewById(R.id.username)).setText("notbryant");
        ((TextView)findViewById(R.id.password)).setText("butts");
    }

    @Override
    public void onClick(View view) {
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
        Intent intent = new Intent(CreateServer.this, MainActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
