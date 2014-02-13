package com.qweex.eyebrows;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class AboutActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        findViewById(R.id.donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("market://details?id=com.qweex.donation");
                Intent intent = new Intent (Intent.ACTION_VIEW, uri);
                try {
                    startActivity(intent);
                } catch(ActivityNotFoundException e)
                {
                    Toast.makeText(AboutActivity.this, "Unable to open Android Play Store", Toast.LENGTH_SHORT).show(); //Locale
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        setResult(RESULT_CANCELED);
    }
}
