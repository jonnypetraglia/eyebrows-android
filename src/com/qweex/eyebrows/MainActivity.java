package com.qweex.eyebrows;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import com.qweex.eyebrows.did_not_write.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    Bundle extras;
    List<Fragment> fragments = new ArrayList<Fragment>();
    FragmentStatePagerAdapter adapter;
    CustomViewPager pager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        extras = getIntent().getExtras();
        Log.e("Eyebrows:Starting", "Setting Bundle is " + extras);
        extras.getString("herp");

        // Setup the fragments, defining the number of fragments, the screens and titles.
        adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()){
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
                return ((MainFragment)fragments.get(position)).getPathUrl();
            }
            @Override
            public int getItemPosition(Object object){
                return PagerAdapter.POSITION_NONE;
            }
        };

        pager = (CustomViewPager)findViewById(R.id.pager);
        //pager.setPagingEnabled(false);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(pageChangeListener);
        addFragment(new ArrayList<String>());
    }

    public Bundle getExtras() {
        Log.e("Eyebrows:Starting", "Getting Bundle is " + extras);
        return extras;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu u) {
        MenuItem mu = u.add(0,0,0,"Download as ZIP");
        mu = u.add(0,1,0, "Refresh");
        mu = u.add(0,2,0, "Exit to Server List");
        return super.onCreateOptionsMenu(u);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case 0: //download as zip
                return true;
            case 1: //Refresh
                getCurrent().getData();
                return true;
            case 2: //Exit to server list
                popFragmentsOrFinish(fragments.size());
                return true;

        }
        return super.onOptionsItemSelected(item);

    }

    private MainFragment getCurrent() {
        return (MainFragment) fragments.get(fragments.size()-1);
    }

    public void showErrorDialog(String msg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error);
        builder.setMessage(msg);
        builder.setNeutralButton(android.R.string.ok, null);
        builder.show().setOnDismissListener(closeErrorDialog);
    }

    private DialogInterface.OnDismissListener closeErrorDialog = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(getCurrent());
            ft.commit();
        }
    };

    public void addFragment(ArrayList<String> uri_path)
    {
        MainFragment mf = new MainFragment();
        Bundle arg = new Bundle();
        arg.putStringArrayList("uri_path", uri_path);
        mf.setArguments(arg);
        fragments.add(mf);
        pager.getAdapter().notifyDataSetChanged();
        pager.setCurrentItem(fragments.size()-1, true);
    }

    public void popFragmentsOrFinish(int howmany)
    {
        Log.d("Eyebrows:pop", "Size: " + fragments.size() + ", popping " + howmany);
        if(fragments.size()<=1)
            finish();
        else {
            pager.setCurrentItem(fragments.size() - 1 - howmany, true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            popFragmentsOrFinish(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            Log.d("Eyebrows:POP", "Selecting Item: " + position + " < " + (fragments.size()-1));
            if(position < fragments.size()-1) {
                while(fragments.size()-1>position) {
                    Log.d("Eyebrows", "Popping " + fragments.size());
                    fragments.remove(fragments.size()-1);
                }
                pager.getAdapter().notifyDataSetChanged();
            }
        }
    };
}
