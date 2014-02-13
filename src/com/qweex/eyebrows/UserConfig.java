package com.qweex.eyebrows;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import com.qweex.utils.Crypt;

import java.security.Key;


public class UserConfig extends PreferenceActivity
{
    Preference advanced;
    CheckBoxPreference use_master;
    Preference change_master;

    AlertDialog passwordDialog;
    EditText password, password_confirm, current_password;

    public static Key masterKey;
    public final static String KEY_CHECK = "herpaderp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
        addPreferencesFromResource(R.xml.preferences);
        use_master = (CheckBoxPreference) getPreferenceScreen().findPreference("use_master_password");
        change_master = getPreferenceScreen().findPreference("change_master_password");
        advanced = getPreferenceScreen().findPreference("advanced");

        if(hasMasterPass(getApplicationContext())) {
            use_master.setOnPreferenceClickListener(remove_pass);
            advanced.setEnabled(false);
        }
        else {
            use_master.setOnPreferenceClickListener(create_pass);
            advanced.setEnabled(true);
        }

        change_master.setOnPreferenceClickListener(change_pass);


        View v = getLayoutInflater().inflate(R.layout.password_dialog, null);
        current_password = (EditText) v.findViewById(R.id.current_password);
        password = (EditText) v.findViewById(R.id.password);
        password_confirm = (EditText) v.findViewById(R.id.password_confirm);
        passwordDialog = new AlertDialog.Builder(this)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
    }

    DialogInterface.OnClickListener performCreation = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            String passwordText = password.getText().toString();
            String password_confirmText = password_confirm.getText().toString();
            if(!passwordText.equals(password_confirmText)) {
                Log.e("ERROR", "asdsdsadsa");
                toast("Passwords do not match!");
                return;
            }

            new AsyncCrypt(UserConfig.this,
                    AsyncCrypt.Task.ENCRYPTING,
                    new AsyncHandler("Data was successfully encrypted. Remember your password!", true))
                    .execute(passwordText);
        }
    };

    // Press ok on decryptAll
    DialogInterface.OnClickListener performRemoval = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            String passwordText = password.getText().toString();
            new AsyncCrypt(UserConfig.this,
                    AsyncCrypt.Task.DECRYPTING,
                    new AsyncHandler("Data was successfully decrypted.", false))
                    .execute(passwordText);
        }
    };

    // Press ok on changeMaster
    DialogInterface.OnClickListener performChange = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            setUse(true);
            String oldPassword = current_password.getText().toString();
            String newPassword = password.getText().toString();
            String newPasswordConfirm = password_confirm.getText().toString();
            if(!newPassword.equals(newPasswordConfirm)) {
                toast("Passwords do not match!");
                return;
            }
            new AsyncCrypt(UserConfig.this,
                    AsyncCrypt.Task.CHANGING,
                    new AsyncHandler("Password was succcessfully changed. Remember your new one!", true))
                    .execute(newPassword);
        }
    };

    class AsyncHandler extends Handler {
        String msg;
        boolean setOnSuccess;
        public AsyncHandler(String msg, boolean b) {
            this.msg = msg;
            this.setOnSuccess = b;
        }
        @Override
        public void handleMessage(Message m) {
            Exception e = (Exception) m.getData().get("error");
            Log.d("ASYNCHANDLER RECEIVED", e + "?");
            if(e==null) {
                setUse(setOnSuccess);
                toast(msg);

                try {
                    String keyCheck = Crypt.encrypt(KEY_CHECK, UserConfig.masterKey);
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(UserConfig.this).edit();
                    edit.putString("key_check", keyCheck);
                    edit.putString("salt", Crypt.getSalt());
                    edit.commit();
                } catch(Exception e1) {
                    e1.printStackTrace();
                }
            }
            else
                toast("An error occurred: \n" + e.getMessage() + " [" + e.getClass() + "]");
        }
    }


    // Click preference to create pass
    Preference.OnPreferenceClickListener create_pass = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference _preference) {
            current_password.setVisibility(View.GONE);
            password_confirm.setVisibility(View.VISIBLE);
            passwordDialog.setTitle("Create Master Password");
            passwordDialog.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), performCreation);
            passwordDialog.show();
            setUse(false);
            return false;
        }
    };

    // Click preference to remove pass
    Preference.OnPreferenceClickListener remove_pass = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference _preference) {
            if(SavedServers.getAll().getCount()>0)
            {
                current_password.setVisibility(View.GONE);
                password_confirm.setVisibility(View.GONE);
                passwordDialog.setTitle("Remove Master Password");
                passwordDialog.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), performRemoval);
                passwordDialog.show();
                setUse(true);
                use_master.setOnPreferenceClickListener(create_pass);
            }
            return false;
        }
    };

    Preference.OnPreferenceClickListener change_pass = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            current_password.setVisibility(View.GONE);
            password_confirm.setVisibility(View.GONE);
            passwordDialog.setTitle("Change Master Password");
            passwordDialog.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), performChange);
            passwordDialog.show();
            return false;
        }
    };


    // Get status of "use_master_password" from preferences
    public static boolean hasMasterPass(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean("use_master_password", false);
    }

    // Set status of "use_master_password" in preferences and update the checkbox
    protected void setUse(boolean def) {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("use_master_password", def).commit();
        use_master.setChecked(def);
        advanced.setEnabled(!def);
        if(def)
            use_master.setOnPreferenceClickListener(remove_pass);
        else
            use_master.setOnPreferenceClickListener(create_pass);
    }


    @Override
    public void onPause() {
        super.onPause();
        setResult(RESULT_CANCELED);
    }

    public void toast(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .show();
    }
}