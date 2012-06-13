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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Flipzu extends Activity {
	
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	GoogleAnalyticsTracker tracker;
	private static final String TAG = "Flipzu";
	private Debug debug = new Debug();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* get analytics singleton */
        tracker = GoogleAnalyticsTracker.getInstance();
        
        /* start tracker. Dispatch every 30 seconds. */
        tracker.startNewSession("UA-20341887-1", 30, this);
        
        /* debug GA */
        tracker.setDebug(false);
        tracker.setDryRun(false);
        
        //Count launch
        AppRater.incrementLaunches(this);
        
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

        
        setContentView(R.layout.main);
        
        // check if we're already logged
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String username = settings.getString("username", null);
        String has_twitter = settings.getString("has_twitter", null);
        String has_facebook = settings.getString("has_facebook", null);
        if ( username != null && (has_twitter != null || has_facebook != null)) {
        	/* returning user, track account type in session scope */
        	int account_type = settings.getInt("account_type", 0);
        	if ( account_type == 1 ) {
        		tracker.setCustomVar(2, "Account Type", "Twitter", 2);
        	} else if ( account_type == 2 ) {
        		tracker.setCustomVar(2, "Account Type", "Facebook", 2);
        	}
        	
        	Intent recorderIntent = new Intent();
        	recorderIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Listings");
        	startActivity(recorderIntent);
        	this.finish();
        } else {
        	/* new user, track app version in visitor scope */
        	PackageInfo pinfo;
			try {
				pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				String versionName = pinfo.versionName;
				tracker.setCustomVar(1, "Version", versionName, 1);
			} catch (NameNotFoundException e) {
				debug.logW(TAG, "onCreate(): couldn't find ourselves in PackageManager: " + e.getMessage());
			}
        }
         
        final Button tw_login_but = (Button) findViewById(R.id.tw_login_but);
        final Button fb_login_but = (Button) findViewById(R.id.fb_login_but);

        tw_login_but.setOnClickListener( new OnClickListener() {
        	public void onClick(View v) {
        		/* track twitter login click */
        		tracker.trackEvent("Login Page", "Click", "Twitter", 0);
        		
               	Intent TwLoginIntent = new Intent();
               	TwLoginIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.TwLogin");
            	startActivity(TwLoginIntent);
            	Flipzu.this.finish();
            	}
        }); 
        fb_login_but.setOnClickListener( new OnClickListener() {
        	public void onClick(View v) {
        		/* track facebook login click */
        		tracker.trackEvent("Login Page", "Click", "Facebook", 0);
        		
               	Intent FbLoginIntent = new Intent();
               	FbLoginIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.FbLogin");
            	startActivity(FbLoginIntent);
            	Flipzu.this.finish();        	}
        });
        fb_login_but.requestFocus();
        
    }
    
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        /* track back without login */
		tracker.trackEvent("Login Page", "Click", "Nothing", 0);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        /* track "/" pageview */
        tracker.trackPageView("/");
    }

    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	/* stop tracker */
    	tracker.stopSession();
    }
    

}
