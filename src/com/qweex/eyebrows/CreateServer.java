package com.qweex.eyebrows;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.qweex.utils.Crypt;


public class CreateServer extends Activity implements View.OnClickListener{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
        setTitle(R.string.create_server);
        setContentView(R.layout.create_activity);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);


        String name = null;
        Bundle extras = getIntent().getExtras();
        if(extras!=null)
            name = extras.getString("name");
        if(name==null || name.length()==0)
        {
            findViewById(R.id.name).setTag("-1");
            return;
        }
        extras = SavedServers.get(this, name);
        Log.d("ASDSADS", extras + "!");

        ((TextView)findViewById(R.id.host)).setText(extras.getString("host"));
        ((TextView)findViewById(R.id.port)).setText(Integer.toString(extras.getInt("port")));
        ((CheckBox)findViewById(R.id.ssl)).setChecked(extras.getBoolean("ssl"));
        ((TextView)findViewById(R.id.name)).setText(name);
        findViewById(R.id.name).setTag(Long.toString(extras.getLong("id")));

        try {
            String auth64;
            if(UserConfig.hasMasterPass(this))
                    auth64 = Crypt.decrypt(extras.getString("auth"), UserConfig.masterKey);
            else
                auth64 = extras.getString("auth");
            auth64 = new String(Base64.decode(auth64, Base64.DEFAULT));
            String[] auth = auth64.split(":");

            ((TextView)findViewById(R.id.username)).setText(auth[0]);
            ((TextView)findViewById(R.id.password)).setText(auth[1]);
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setMessage("There was a problem decrypting your auth info, which is a very bad thing.")
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show();
        }
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
        Long id = Long.parseLong((String) findViewById(R.id.name).getTag());

        String name = ((TextView)findViewById(R.id.name)).getText().toString();
        if(id==-1 && view.getId()==R.id.save && SavedServers.get(this, name)!=null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage("A saved server with this name already exists")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();
            return;
        }

        try {
            String host = ((TextView)findViewById(R.id.host)).getText().toString();
            if(host.isEmpty())
                throw new Exception();
            int port = Integer.parseInt(((TextView)findViewById(R.id.port)).getText().toString());
            boolean ssl = ((CheckBox)findViewById(R.id.ssl)).isChecked();
            String username = ((TextView)findViewById(R.id.username)).getText().toString();
            String password = ((TextView)findViewById(R.id.password)).getText().toString();

            //Check if username contains colon
            if(username.contains(":")) {
                new AlertDialog.Builder(this)
                        .setMessage("The username you've entered contains a colon, which is forbidden. (Are you sure it is correct?)")
                        .setNegativeButton(android.R.string.ok, null)
                        .show();
                return;
            }

            bundle.putString("host", host);
            bundle.putInt("port", port);
            bundle.putBoolean("ssl", ssl);

            bundle.putString("auth", Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT));
            bundle.putString("name", name);
        } catch(Exception nfe) {
            Log.e("Eyebrows:createServer", "Insufficient: " + nfe.toString());
            Toast.makeText(view.getContext(), R.string.insufficient_info, Toast.LENGTH_SHORT);
            return;
        }

        Log.e("Eyebrows:Starting", "Creating Bundle is " + bundle);
        if(view.getId() == R.id.save)
            if(id<0)
                SavedServers.add(this, bundle);
            else
                SavedServers.update(this, id, bundle);
        Intent intent = new Intent(CreateServer.this, MainActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, MainActivity.class.hashCode() % 0xffff);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==MainActivity.class.hashCode() % 0xffff)
        {
            setResult(resultCode);
            finish();
        }
    }
}
