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

import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.twitterapime.rest.Credential;
import com.twitterapime.rest.UserAccountManager;
import com.twitterapime.search.LimitExceededException;

public class TwLogin extends Activity {
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private static final String TAG = "TwLogin";
	private Debug debug = new Debug();
	String username = null;
	String password = null;
	private boolean logged = false;
	String response = "";
		
	GoogleAnalyticsTracker tracker;
	
	static final int CONNECTING_DIALOG_ID = 0;
	
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
        
        setContentView(R.layout.twitter);
         
        final EditText tw_username_et = (EditText) findViewById(R.id.tw_username_et);
        final EditText tw_password_et = (EditText) findViewById(R.id.tw_password_et);
        final Button tw_login_but = (Button) findViewById(R.id.tw_login_but);
        
        tw_password_et.setOnKeyListener( new OnKeyListener() {
        	public boolean onKey(View v, int keyCode, KeyEvent event) {
        		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
        			username = tw_username_et.getText().toString();
        			password = tw_password_et.getText().toString();
        			if ( username.length() > 0 && password.length() > 0 ) {
        				new DoTwLogin().execute();
        			}
        			return true;
        		}
        		return false;
        	}
        });
        tw_login_but.setOnClickListener( new OnClickListener() {
        	public void onClick(View v) {
        		username = tw_username_et.getText().toString();
    			password = tw_password_et.getText().toString();
    			debug.logV(TAG, "onClick username " + username + " pass " + password);
    			if ( username.length() == 0 || password.length() == 0 ) {
    				return;
    			}
        		new DoTwLogin().execute();
        	}
        });   
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        /* track pageview */
        tracker.trackPageView("/" + this.getLocalClassName());
    }
    
    @Override
    public void onBackPressed()
    {
    	super.onBackPressed();    	
    	
    	backToInitial();
    }
    
    private class DoTwLogin extends AsyncTask<Void, String, Void> {
    	
    	protected void onPreExecute() {
    		showDialog(CONNECTING_DIALOG_ID);    		
    	}
    	
    	protected Void doInBackground(Void... unused ) {
    		debug.logD(TAG, "DoTwLogin: starting");
    		
    		Credential c = new Credential(username, password, "XXX", "XXX");
    		UserAccountManager m = UserAccountManager.getInstance(c);
    		
    		User u = new User();

    		try {
				if ( m.verifyCredential()) {
					String access_token = "oauth_token_secret=" + m.getAccessToken().getSecret() + "&oauth_token=" + m.getAccessToken().getToken();
            		FlipInterface fi = new FlipInterface();
            		debug.logV(TAG, "DoTwLogin, credentials ok, got token " + access_token);
            		try {
    					u = fi.requestTokenWithToken("tw_access_token=1&" + access_token );
    					if ( u != null ) {
        					logged = true;
        					debug.logV(TAG, "TwLoginTask: login successful");
    					} else {
    						response = "Can't get FlipZu token, please retry";    						
    					}
    				} catch (InvalidToken e) {
    				    debug.logE(TAG, "TwLoginTask: requestTokenWithToken ERROR ", e.getCause());
    				    response = "Can't get FlipZu token, please retry";
    				}
				} else {
					response = "Invalid Username or Password";
				}
			} catch (IOException e1) {
				response = "Can't connect with Twitter, please retry";
				debug.logE(TAG, "DoTwLogin", e1.getCause());
			} catch (LimitExceededException e1) {
				response = "API Limit Exceeded, please retry";
				debug.logE(TAG, "DoTwLogin", e1.getCause());
			}
            	        	        
    		//debug.logV(TAG, "TwLogin " + resp.getCode());
        	
            if ( logged ) {
            	try {
            		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            		SharedPreferences.Editor editor = settings.edit();
            		editor.putString("username", u.getUsername());
            		editor.putString("token", u.getToken());
            		if ( u.hasFacebook() ) {
            			editor.putString("has_facebook", "1");
            		} else {
            			editor.putString("has_facebook", "0");
            		}
            		if ( u.isPremium() ) {
            			editor.putString("is_premium", "1");
            		} else {
            			editor.putString("is_premium", "0");
            		}
            		editor.putInt("account_type", 1); // 1 is Twitter
            		editor.putString("has_twitter", "1");
            		editor.commit();
                   	Intent recorderIntent = new Intent();
                	recorderIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Listings");
                	startActivity(recorderIntent);
                	TwLogin.this.finish();
            	} catch (Exception e ) {
            		debug.logE(TAG, "doInbackground ERROR", e.getCause());
            		e.printStackTrace();
            	}
            } else {
            	publishProgress(response);
            }    
            
            debug.logD(TAG,"DoTwLogin: ending");
            
            return null;
    	}
    	
    	protected void onPostExecute(Void unused) {
    		try {
    			dismissDialog(CONNECTING_DIALOG_ID);	
    		} catch ( IllegalArgumentException e ) {
    			//
    		}
    	    
    	}
    	
		protected void onProgressUpdate(String... values) {
			
			for ( String response : values ) {
	        	if ( response != null ) {            		
	        		Toast.makeText(TwLogin.this, response, Toast.LENGTH_SHORT).show();
	        	} else {
	        		Toast.makeText(TwLogin.this, "Failed to login. Please try again or check your Internet connection.", Toast.LENGTH_SHORT).show();
	        	}				
			}
		}

    }
    
    private void backToInitial() {
		debug.logD(TAG,"TwLogin backToInitial");
		Intent LoginIntent = new Intent();
    	LoginIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Flipzu");
    	startActivity(LoginIntent);
    	this.finish();
	}
        
    @Override
    protected void onDestroy() 
    {
    	super.onDestroy();
    	/* stop tracker */
    	tracker.stopSession();
    }
    
	protected Dialog onCreateDialog( int id ) {
		Dialog dialog = null;
		ProgressDialog pDialog;
		
		switch(id) { 
		case CONNECTING_DIALOG_ID:
			pDialog = new ProgressDialog(TwLogin.this);
			pDialog.setTitle(null);
			pDialog.setMessage("Connecting with Twitter...");
			dialog = pDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
}
