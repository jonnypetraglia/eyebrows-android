package com.qweex.eyebrows;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.qweex.utils.Crypt;

public class UnlockApp extends Activity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_dialog);

        findViewById(R.id.current_password).setVisibility(View.GONE);
        findViewById(R.id.password_confirm).setVisibility(View.GONE);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10,10,10,10);

        Button unlock = new Button(this);
        unlock.setText(R.string.unlock);
        unlock.setOnClickListener(this);

        LinearLayout l = (LinearLayout) findViewById(R.id.layout);
        l.setBackgroundColor(getResources().getColor(android.R.color.white));
        l.addView(unlock, lp);

        setResult(RESULT_CANCELED);
    }

    @Override
    public void onClick(View view) {
        String password = ((EditText)findViewById(R.id.password)).getText().toString();
        UserConfig.masterKey = Crypt.getKeyFromPassword(password);

        String keyCheck = PreferenceManager.getDefaultSharedPreferences(this).getString("key_check", "fuckshit");
        try {
            keyCheck = Crypt.decrypt(keyCheck, UserConfig.masterKey);
        } catch(Exception e) {
            //the hash of "herpaderp" should never be equal to "herpaderp", so we can leave it as the hash.
        }
        if(keyCheck.equals(UserConfig.KEY_CHECK)) {
            setResult(RESULT_OK);
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.incorrect_password)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }
}
