package com.qweex.eyebrows;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import com.blundell.tut.LoaderTouchImageView;

public class ImageViewPager extends ViewPager {

    public ImageViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewPager(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("onTouch", getCurrentItem() + "A");
        Log.d("onTouch", getChildCount() + "B");
        LoaderTouchImageView img = ((PictureViewer.Adapter)this.getAdapter()).getItemAt(getCurrentItem());
        if(img.isBeingZoomed()) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.d("onTouch", getCurrentItem() + "A");
        Log.d("onTouch", getChildCount() + "B");
        LoaderTouchImageView img = ((PictureViewer.Adapter)this.getAdapter()).getItemAt(getCurrentItem());
        if(img.isBeingZoomed()) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    }
}
