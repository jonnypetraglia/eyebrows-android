package com.blundell.tut;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.example.touch.TouchImageView;

import javax.net.ssl.HttpsURLConnection;

// I modified this file slightly but it is mostly the same
//    -notbryant

/**
 * Free for anyone to use, just say thanks and share :-)
 * @author Blundell
 *
 */
public class LoaderTouchImageView extends LinearLayout{

	private static final int COMPLETE = 0;
	private static final int FAILED = 1;

	private Context mContext;
	private Drawable mDrawable;
	private ProgressBar mSpinner;
	private TouchImageView mImage;

    private boolean ssl = false;
    private String authstring = null;

    public boolean hasBeenSet = false;

    public boolean isBeingZoomed() {
        return mImage.isBeingZoomed();
    }


	/**
	 * This is used when creating the view in XML
	 * To have an image load in XML use the tag 'image="http://developer.android.com/images/dialog_buttons.png"'
	 * Replacing the url with your desired image
	 * Once you have instantiated the XML view you can call
	 * setImageDrawable(url) to change the image
	 * @param context
	 * @param attrSet
	 */
	public LoaderTouchImageView(final Context context, final AttributeSet attrSet) {
		super(context, attrSet);
		final String url = attrSet.getAttributeValue(null, "image");
		if(url != null){
			instantiate(context, url);
		} else {
			instantiate(context, null);
		}
	}
	
	/**
	 * This is used when creating the view programatically
	 * Once you have instantiated the view you can call
	 * setImageDrawable(url) to change the image
	 * @param context the Activity context
	 * @param imageUrl the Image URL you wish to load
	 */
	public LoaderTouchImageView(final Context context, final String imageUrl, boolean ssl, String authstring) {
		super(context);
        this.ssl = ssl;
        this.authstring = authstring;
		instantiate(context, imageUrl);
	}

	/**
	 *  First time loading of the LoaderTouchImageView
	 *  Sets up the LayoutParams of the view, you can change these to
	 *  get the required effects you want
	 */
	private void instantiate(final Context context, final String imageUrl) {
		mContext = context;

		mImage = new TouchImageView(mContext);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
		mImage.setLayoutParams(lp);
		
		mSpinner = new ProgressBar(mContext);
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        mSpinner.setLayoutParams(lp);
			
		mSpinner.setIndeterminate(true);
		
		addView(mSpinner);
		addView(mImage);
		
		if(imageUrl != null){
			setImageDrawable(imageUrl);
		}
	}

	/**
	 * Set's the view's drawable, this uses the internet to retrieve the image
	 * don't forget to add the correct permissions to your manifest
	 * @param imageUrl the url of the image you wish to load
	 */
	public void setImageDrawable(final String imageUrl) {
        hasBeenSet = true;
		mDrawable = null;
		mSpinner.setVisibility(View.VISIBLE);
		mImage.setVisibility(View.GONE);
		new Thread(){
			public void run() {
				try {
                    Log.e("asdf", imageUrl);
					mDrawable = getDrawableFromUrl(imageUrl);
					imageLoadedHandler.sendEmptyMessage(COMPLETE);
				} catch (MalformedURLException e) {
                    Log.e("asdf", "Malformed: ");
					imageLoadedHandler.sendEmptyMessage(FAILED);
				} catch (IOException e) {
                    Log.e("asdf", "IO: " + e);
					imageLoadedHandler.sendEmptyMessage(FAILED);
				}
			};
		}.start();
	}
	
	/**
	 * Callback that is received once the image has been downloaded
	 */
	private final Handler imageLoadedHandler = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case COMPLETE:
				mImage.setImageDrawable(mDrawable);
				mImage.setVisibility(View.VISIBLE);
				mSpinner.setVisibility(View.GONE);
				break;
			case FAILED:
			default:
				// Could change image here to a 'failed' image
				// otherwise will just keep on spinning
                //Bundle b = null; b.putAll(null);
                mImage.setVisibility(View.INVISIBLE);
				break;
			}
			return true;
		}		
	});

	/**
	 * Pass in an image url to get a drawable object
	 * @return a drawable object
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private Drawable getDrawableFromUrl(final String url) throws IOException, MalformedURLException {
        InputStream stream;
        URL u = new java.net.URL(url);
        if(ssl)
        {
            HttpsURLConnection h = (HttpsURLConnection) u.openConnection();
            if(authstring!=null)
                h.setRequestProperty("Authorization", "Basic " + authstring);
            stream = (InputStream) h.getContent();
        } else {
            HttpURLConnection h = (HttpURLConnection) u.openConnection();
            if(authstring!=null)
                h.setRequestProperty("Authorization", "Basic " + authstring);
            stream = (InputStream) h.getContent();
        }

		return Drawable.createFromStream(((java.io.InputStream) stream), "name");
	}
	
}
