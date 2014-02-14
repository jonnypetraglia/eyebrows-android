package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.qweex.eyebrows.did_not_write.*;
import com.qweex.utils.MultiFileChooserDialog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// This is the class that creates and manages the fragments for listing files.
// It also contains things like the navbar and the notification bar.

public class MainActivity extends FragmentActivity implements Spinner.OnItemSelectedListener, MultiFileChooserDialog.OnFilesChosen {

    Bundle extras;
    List<MainFragment> fragments = new ArrayList<MainFragment>();
    CustomViewPager pager;
    String HOME;
    Spinner path_spinner;
    boolean spinnerIsLoading = true;
    List<Pair<String, List<String>>> filesToUpload = new ArrayList<Pair<String, List<String>>>(); //<uploadServerUri, files...
    FileUploader uploader;
    DownloadManager downloader;
    ZipDownloader zipDownloader;
    List<Pair<String, List<String>>> zipsToDownload = new ArrayList<Pair<String, List<String>>>();
    boolean askForPassOnResume;

    // Called when activity is created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_OK);
        setContentView(R.layout.main);
        HOME = getResources().getString(R.string.home);

        extras = getIntent().getExtras();
        setTitle(extras.getString("name"));
        Log.d("MainActivity", "Creating with auth: " + extras.getString("auth"));

        pager = (CustomViewPager)findViewById(R.id.pager);
        pager.setPagingEnabled(false);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(pageChangeListener);

        // Setup Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_list, new ArrayList<String>());
        path_spinner = (Spinner)findViewById(R.id.path_spinner);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner);
        path_spinner.setAdapter(spinnerAdapter);
        path_spinner.setOnItemSelectedListener(this);

        findViewById(R.id.upload).setOnClickListener(clickUpload);

        addFragment(new ArrayList<String>());

        downloader = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);

        askForPassOnResume = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(askForPassOnResume && UserConfig.hasMasterPass(this))
            startActivityForResult(new Intent(this, UnlockApp.class), UnlockApp.class.hashCode() % 0xffff);

        askForPassOnResume = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ask_for_pass_on_resume", false);
        setResult(RESULT_OK);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(resultCode!=RESULT_OK && requestCode==UnlockApp.class.hashCode() % 0xffff) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            askForPassOnResume = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        setResult(RESULT_CANCELED);
    }


    // The adapter that will manage the fragments inside the ViewPager
    FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()){
        @Override
        public int getCount() {
            return fragments.size();
        }
        @Override
        public Fragment getItem(final int position) {
            return fragments.get(position);
        }
        @Override
        public CharSequence getPageTitle(final int position) {
            return fragments.get(position).getPathUrl();
        }
        @Override
        public int getItemPosition(Object object){
            return PagerAdapter.POSITION_NONE;
        }
    };

    // Sets the spinner data; for use for by the fragments
    public void setSpinner(List<String> spinnerContents)
    {
        Log.d("Eyebrows", "Setting Spinner: " + spinnerContents);
        ArrayAdapter<String> adap = ((ArrayAdapter<String>) path_spinner.getAdapter());
        adap.clear();
        adap.add(HOME);
        adap.addAll(spinnerContents);
        adap.notifyDataSetChanged();
        this.spinnerIsLoading = true;
        path_spinner.setSelection(path_spinner.getCount()-1);
    }

    // Used to start uploading files when the user has selected them from the FilePickerDialog
    @Override
    public void onFilesChosen(String path, List<String> files) {
            Log.d("Eyebrows", "OK UPLOADING FILES");
            String uploadPath = "http" +
                    (extras.getBoolean("ssl") ? "s" : "") + "://" + extras.getString("host") + ":" + extras.getInt("port") + "/" +
                    path;

            filesToUpload.add(Pair.create(uploadPath, files));

            if(uploader==null)
            {
                uploader = new FileUploader(MainActivity.this,
                        showUploadNotification,
                        updateUploadNotification,
                        hideUploadNotification);
                uploader.execute();
            }
    };

    static long download_id;

    public Handler downloadFilesHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.getData()==null || msg.getData().getStringArrayList("files")==null)
                return;

            ArrayList<String> newDownloads = msg.getData().getStringArrayList("files");
            Log.d("Eyebrows", "Downloading: " + msg.getData().getString("downloadPath") + "!");

            String downloadPath = msg.getData().getString("downloadPath");
            if(downloadPath.length()>0)
                downloadPath = downloadPath + "/";
            downloadPath = "http" +
                    (extras.getBoolean("ssl") ? "s" : "") + "://" + extras.getString("host") + ":" + extras.getInt("port")
                    + "/" + downloadPath
                    ;

            String downloadDir = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("download_dir", "Eyebrows");
            for(String file : newDownloads) {
                // Determine where it will be saved locally
                String dest = TextUtils.join("/", new String[] {downloadDir, msg.getData().getString("downloadPath")});
                String filename = file;


                Uri Download_Uri = Uri.parse(downloadPath + file);
                Log.d("Eyebrows", "Downloading: " + Download_Uri + "!");
                DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                request.addRequestHeader("Authorization", "Basic " + extras.getString("auth"));
                request.setDestinationInExternalPublicDir(dest, filename);
                //request.setDestinationInExternalFilesDir(MainActivity.this, dest, filename);
                Log.d("Eyebrows", "Downloading to : " + dest + " | " + filename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setVisibleInDownloadsUi(true);
                request.allowScanningByMediaScanner();
                request.setTitle(filename);
                download_id = downloader.enqueue(request);
            }
        }
    };

    // User clicks upload button
    View.OnClickListener clickUpload = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new MultiFileChooserDialog(MainActivity.this, MainActivity.this);
        }
    };

    /*
    // User clicks upload button when an upload is already going on
    View.OnClickListener clickCancel = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.confirm_cancel)
                    .setPositiveButton(R.string.yes, confirmCancelUpload)
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    };
    */

    // User confirms to cancel uploading
    DialogInterface.OnClickListener confirmCancelUpload = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            uploader.cancel(true);
            hideUploadNotification.run();
        }
    };

    // Getter; for use by fragments
    public Bundle getExtras() { return extras; }

    // Creates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu u) {
        MenuItem mu;
        mu = u.add(0,0,0, R.string.download_as_zip);
        mu = u.add(0,1,0, R.string.refresh);
        mu = u.add(0,2,0, R.string.exit);
        return super.onCreateOptionsMenu(u);
    }

    // Called when an options menu is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case 0: //download as zip
                fragments.get(fragments.size()-1).downloadZip();
                return true;
            case 1: //Refresh
                fragments.get(fragments.size()-1).getData();
                return true;
            case 2: //Exit to server list
                popFragmentsOrFinish(fragments.size());
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Shows an error dialog and quits the activity whenever the user acknowledges it
    public void showErrorDialog(String msg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error);
        builder.setMessage(msg);
        builder.setNeutralButton(android.R.string.ok, null);
        builder.show().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
    }


    // Adds a fragment & switches to it
    public void addFragment(ArrayList<String> uri_path)
    {
        this.setSpinner(uri_path);
        MainFragment mf = new MainFragment();
        Bundle arg = new Bundle();
        arg.putStringArrayList("uri_path", uri_path);
        mf.setArguments(arg);
        fragments.add(mf);
        pager.getAdapter().notifyDataSetChanged();
        pager.setCurrentItem(fragments.size()-1, true);
    }

    @Override
    public void finish()
    {
        if(findViewById(R.id.notification).getVisibility()==View.VISIBLE) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.finish_confirm_cancel)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            uploader.cancel(true);
                            MainActivity.super.finish();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            super.finish();
        }
    }

    // Pops a fragment off or if there is only 1 fragment, finishes
    public void popFragmentsOrFinish(int howmany)
    {
        Log.d("Eyebrows:pop", "Size: " + fragments.size() + ", popping " + howmany);
        if(fragments.size()<=1)
            finish();
        else {
            pager.setCurrentItem(fragments.size() - 1 - howmany, true);
        }
    }

    // Override back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            popFragmentsOrFinish(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // Pops off all the fragments after the one selected
    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            Log.d("Eyebrows:POP", "Selecting Item: " + position + " < " + (fragments.size()-1));
            if(position < fragments.size()-1) {
                while(fragments.size()-1>position) {
                    Log.d("Eyebrows", "Popping " + fragments.size());
                    fragments.remove(fragments.size()-1);
                }
                setSpinner(fragments.get(fragments.size() - 1).getPath());
                pager.getAdapter().notifyDataSetChanged();
            }
        }
    };

    // Shows the notification bar
    public Runnable showUploadNotification = new Runnable() {
        public void run() {
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_top);
            findViewById(R.id.notification_upload).setVisibility(View.VISIBLE);
            if(findViewById(R.id.notification).getVisibility()==View.GONE) {
                findViewById(R.id.notification).setVisibility(View.VISIBLE);
                findViewById(R.id.notification).startAnimation(anim);
            } else
                findViewById(R.id.notification_download).startAnimation(anim);

            ((ImageButton)findViewById(R.id.upload)).setImageResource(R.drawable.ic_action_cancel);
            //((ImageButton)findViewById(R.id.upload)).setOnClickListener(clickCancel);
        }
    };

    // Hides the notification bar
    public Runnable hideUploadNotification = new Runnable() {
        public void run() {
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_bottom);
            findViewById(R.id.notification).setVisibility(View.GONE);
            if(findViewById(R.id.notification_download).getVisibility()==View.GONE) {
                findViewById(R.id.notification).setVisibility(View.GONE);
                findViewById(R.id.notification).startAnimation(anim);
            } else
                findViewById(R.id.notification).startAnimation(anim);

            ((TextView)findViewById(R.id.notification_upload_text)).setText("");
            ((ProgressBar)findViewById(R.id.notification_upload_bar)).setProgress(0);


            ((ImageButton)findViewById(R.id.upload)).setImageResource(R.drawable.ic_action_upload);
            //((ImageButton)findViewById(R.id.upload)).setOnClickListener(clickUpload);
            uploader = null;
        }
    };

    // Updates the notification bar
    public Handler updateDownloadNotification = new Handler() {
        public void handleMessage(Message m) {
            Long part = m.getData().getLong("totalRead");
            Long whole = m.getData().getLong("fileSize");
            String filename = m.getData().getString("fileName");

            TextView text = (TextView) findViewById(R.id.notification_download_text);
            ProgressBar bar = (ProgressBar) findViewById(R.id.notification_download_bar);


            Log.d("Eyebrows", part + " of " + whole);
            bar.setProgress((int) (part / 100));
            bar.setMax((int)(whole/100));
            text.setText(filename);
        }
    };

    // Updates the notification bar
    public Handler updateUploadNotification = new Handler() {
        public void handleMessage(Message m) {
            Long part = m.getData().getLong("totalRead");
            Long whole = m.getData().getLong("fileSize");
            String filename = m.getData().getString("fileName");

            TextView text = (TextView) findViewById(R.id.notification_upload_text);
            ProgressBar bar = (ProgressBar) findViewById(R.id.notification_upload_bar);


            Log.d("Eyebrows", part + " of " + whole);
            bar.setProgress((int) (part / 100));
            bar.setMax((int)(whole/100));
            text.setText(filename);
        }
    };

    // Spinner
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        if(!spinnerIsLoading)
            popFragmentsOrFinish(path_spinner.getCount() - pos - 1);
        spinnerIsLoading = false;
    }

    // Spinner
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    public Pair<String, List<String>> getFileToUpload() {
        if(filesToUpload.size()>0)
            return filesToUpload.remove(0);
        else
            return null;
    }

    public Pair<String, List<String>>  getZipUrlToDownload() {
        if(zipsToDownload.size()>0)
            return zipsToDownload.remove(0);
        else
            return null;
    }

    public String getBaseUrl() {
        return "http" + (extras.getBoolean("ssl") ? "s" : "") + "://" +
                extras.getString("host") + ":" + extras.getInt("port") + "/";
    }

    // Utility
    public static final DecimalFormat twoDec = new DecimalFormat("0.00");
    private static final String[] SUFFIXES = new String[] {"", "K", "M", "G", "T"};
    public static String formatBytes(long input)
    {
        double temp = input;
        int i;
        for(i=0; temp>5000; i++)
            temp/=1024;
        return (twoDec.format(temp) + " " + SUFFIXES[i] + "B");
    }
}
