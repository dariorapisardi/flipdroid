/**
* Copyright 2011 Flipzu
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  
*  Contributors: 
*  		Dario Rapisardi <dario@rapisardi.org>
*  		Nicolás Gschwind <nicolas@gschwind.com.ar>
*/
package com.flipzu.flipzu;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class FbLogin extends Activity implements DialogListener{
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private static final String TAG = "FbLogin";
	private Debug debug = new Debug();
	
	private static Facebook fb = null;
	
	GoogleAnalyticsTracker tracker;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.facebook);
        
        /* get analytics singleton */
        tracker = GoogleAnalyticsTracker.getInstance();
        
        /* start tracker. Dispatch every 30 seconds. */
        tracker.startNewSession("UA-20341887-1", 30, this);
        
        /* debug GA */
        tracker.setDebug(false);
        tracker.setDryRun(false);
        
        // Determine the screen orientation and set it in a custom variable.
        String orientation = "unknown";
        switch (this.getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = "landscape";
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                orientation = "portrait";
                break;
        }
        tracker.setCustomVar(3, "Screen Orientation", orientation, 2);
        
        fb = new Facebook("XXX"); // flipzu
//        fb = new Facebook("XXX"); // flipzu-devel

        try {
			fb.logout(this);
		} catch (Exception e) {
			debug.logE(TAG, "FbLogin Logout Failed", e.getCause());
		}
		
        String [] perms = {"publish_stream","offline_access","read_stream","email"};

        debug.logD(TAG,"FbLogin starting authorize");
        fb.authorize(this, perms,this);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        /* track pageview */
        tracker.trackPageView("/" + this.getLocalClassName());
    }

    public void onComplete(Bundle values) {
		debug.logD(TAG,"FbLogin Completed");
		FlipInterface fi = new FlipInterface();
		try {
			User u = fi.requestTokenWithToken( "fb_access_token=" + values.getString("access_token") );
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    		SharedPreferences.Editor editor = settings.edit();
    		if ( editor == null || u == null) {
    			Toast.makeText(FbLogin.this, "Login with Facebook failed", Toast.LENGTH_SHORT).show();
    			backToInitial();
    			return;
    		}
    		editor.putString("username", u.getUsername());
    		editor.putString("token", u.getToken());
    		if ( u.hasTwitter() ) {
    			editor.putString("has_twitter", "1");
    		} else {
    			editor.putString("has_twitter", "0");
    		}
    		if ( u.isPremium() ) {
    			editor.putString("is_premium", "1");
    		} else {
    			editor.putString("is_premium", "0");
    		}
    		editor.putInt("account_type", 2); // 2 is Facebook
    		editor.putString("has_facebook", "1");
    		editor.commit();
    		
    		FbLogin.this.fwdToRecorder();
    		
		} catch (InvalidToken e) {
		    debug.logE(TAG, "FbLogin requestTokenWithToken ERROR ", e.getCause());
		}
    }

    @Override
	public void onCancel() {
		debug.logD(TAG,"FbLogin Cancelled");
		Toast.makeText(FbLogin.this, "Cancelled", Toast.LENGTH_SHORT).show();
       	backToInitial();
		
	}

    @Override
	public void onError(DialogError e) {
		debug.logD(TAG,"FbLogin Error");
		Toast.makeText(FbLogin.this, "Error: " + e.getMessage() , Toast.LENGTH_LONG).show();
       	backToInitial();
	}

    @Override
	public void onFacebookError(FacebookError e) {
		debug.logD(TAG,"FbLogin Facebook Error");
		Toast.makeText(FbLogin.this, "Error: " + e.getMessage() , Toast.LENGTH_LONG).show();
       	backToInitial();
	}
	
	private void backToInitial() {
		debug.logD(TAG,"FbLogin backToInitial");
		Intent LoginIntent = new Intent();
    	LoginIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Flipzu");
    	startActivity(LoginIntent);
    	this.finish();
	}
	
	private void fwdToRecorder() {
		debug.logD(TAG,"FbLogin fwdToRecorder");
		Intent RecIntent = new Intent();
    	RecIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Listings");
    	startActivity(RecIntent);
    	this.finish();
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      debug.logD(TAG,"FbLogin onActivityResult request Code " + requestCode + " resultCode " + resultCode);
      fb.authorizeCallback(requestCode, resultCode, data);
    }
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		/* stop tracker */
		tracker.stopSession();
	}
	
    @Override
    public void onBackPressed()
    {
    	super.onBackPressed();    	
    	
    	backToInitial();
    }

}
