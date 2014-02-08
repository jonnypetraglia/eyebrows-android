package com.qweex.eyebrows;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import com.blundell.tut.LoaderTouchImageView;

import java.util.ArrayList;

public class PictureViewer extends FragmentActivity {
    ImageViewPager pager;

    ArrayList<LoaderTouchImageView> images = new ArrayList<LoaderTouchImageView>();
    ArrayList<String> filenames;
    String baseUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filenames = getIntent().getExtras().getStringArrayList("images");
        String startFile = getIntent().getExtras().getString("filename");
        baseUrl = getIntent().getExtras().getString("path");
        boolean ssl = getIntent().getExtras().getBoolean("ssl");
        String authString = getIntent().getExtras().getString("authString");

        int startIndex = 0;
        for(String filename : filenames) {
            images.add(new LoaderTouchImageView(this, null, ssl, authString));
            if(filename.equals(startFile))
                startIndex = images.size()-1;
        }


        pager = new ImageViewPager(this);
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                Log.d("asdf", "Page Selected: " + images.get(i));
                if (!images.get(i).hasBeenSet)
                    images.get(i).setImageDrawable(baseUrl + Uri.encode(filenames.get(i)));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        pager.setAdapter(new Adapter());
        setContentView(pager);

        Log.d("asdf", "asdf " + startIndex);
        if(startIndex == 0)
            images.get(startIndex).setImageDrawable(baseUrl + Uri.encode(filenames.get(startIndex)));
        pager.setCurrentItem(startIndex);
    }

    class Adapter extends PagerAdapter {

        public LoaderTouchImageView getItemAt(int i) {
            return images.get(i);
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.gravity = Gravity.CENTER;

            ((ViewPager) collection).addView(images.get(position), lp);
            return images.get(position);
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            ((ViewPager) collection).removeView((LoaderTouchImageView) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==((LoaderTouchImageView)object);
        }

        @Override
        public int getCount() {
            return images.size();
        }
    }

}
