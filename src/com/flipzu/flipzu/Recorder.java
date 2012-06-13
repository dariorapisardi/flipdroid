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

import java.util.Hashtable;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Recorder extends FragmentActivity implements ResponseListener {	
	
    private final static String APP_PNAME = "com.flipzu.flipzu";

	/* debugging tag */
	private static final String TAG = "Recorder";
	private Debug debug = new Debug();
	
	/* service notification ID */
	private int NOTIF_ID = R.string.fz_recorder_alert;
	
	/* google analytics */
	GoogleAnalyticsTracker tracker;
	
	/* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private SharedPreferences settings;
	
	/* user settings */
	public User mUser = null;
	private String mKey = null;
	private boolean hasTwitter = false;
	private static boolean shareTwitter = true;
	private boolean hasFacebook = false;
	private static boolean shareFacebook = true;
	
	/* broadcast title */
	private String mTitle = "";
	private Integer mBcastId = 0;

	/* recorder states */
	enum recorderState {
		STOPPED, RECORDING, CONNECTING, STOPPING
	};
	static recorderState mState = recorderState.STOPPED;
	/* Recorder service intent */
	private Intent intent = null;
	
	/* dialogs */
	static final int CONNECTING_DIALOG_ID = 0;
	static final int DISCONNECTING_DIALOG_ID = 1;
	static final int AUTH_FAILED_DIALOG_ID = 2;
	static final int CONNECTION_LOST_DIALOG_ID = 3;
	static final int NO_INTERNET_DIALOG_ID = 4;
	static final int MIC_FAILED_DIALOG_ID = 5;
	static final int SLOW_NETWORK_DIALOG_ID = 6;
	static final int SLOW_CPU_DIALOG_ID = 7;
	static final int BIND_FACEBOOK_DIALOG_ID = 8;
	static final int BIND_TWITTER_DIALOG_ID = 9;
	
	/* dialog states */
	static Integer dState = null;
	
	/* VUMeter */
	VUMeter vumeter = null;
	
	/* Handler for broadcast duration */
	private Handler mHandler = new Handler();
	private Handler clockHandler = new Handler();
	
	/* menus */
	private static final int MENU_ITEM_LOGOUT = 0;
	private static final int MENU_ITEM_ABOUT = 1;
	private static final int MENU_ITEM_SHARE_FLIPZU = 2;
	private static final int MENU_ITEM_SHARE = 3;
	
	/* broadcast duration runnable */
	private long mStartTime = 0;
	private Runnable mUpdateDurationTask = new Runnable() {
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			long elapsed;
			if (mState != recorderState.RECORDING) {
				elapsed = 0;
			} else {
				long start = mStartTime;
				long current = System.currentTimeMillis();
				elapsed = current - start;
			}

			int seconds = (int) elapsed / 1000;
			int minutes = seconds / 60;
			seconds = seconds % 60;

			 debug.logD(TAG, "mUpdateDurationTask() called, duration " +
			 minutes + ":" + seconds + " , mStartTime is " + mStartTime);

			final TextView duration_tv = (TextView) findViewById(R.id.duration_tv);

			String minutes_pad;
			if (minutes < 10) {
				minutes_pad = "0" + minutes;
			} else {
				minutes_pad = Integer.toString(minutes);
			}

			String seconds_pad;
			if (seconds < 10) {
				seconds_pad = "0" + seconds;
			} else {
				seconds_pad = Integer.toString(seconds);
			}

			duration_tv.setText(minutes_pad + ":" + seconds_pad);

			// run in 1 second
			clockHandler.postDelayed(this, 1000);
		}
	};
	
	/* listeners/comments runnable */
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {			
			debug.logD(TAG, "mUpdateTimeTask() called for bcastId " + mBcastId);
			
			if ( mBcastId != 0 ) {
				AsyncFlipInterface.getComments(mBcastId, Recorder.this);
				AsyncFlipInterface.getListeners(mBcastId, Recorder.this);
			} else {
				final LinearLayout cc = (LinearLayout) findViewById(R.id.comments_container);

				final LayoutParams params = new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
				if ( mState != recorderState.RECORDING ) {
					/* cleanup comments first */
					cc.removeAllViews();
					String msg = "Pick a good and descriptive broadcast title. A great title will attract more listeners!";
					showFlipzuTipsOffline(cc, params, msg);
					msg = "You can disable sharing in Twitter and Facebook by clicking the logo buttons. This is great for testing.";
					showFlipzuTipsOffline(cc, params, msg);
					msg = "Press the \"Start Broadcast\" button and let them hear you!";
					showFlipzuTipsOffline(cc, params, msg);
				}				
			}
			
			// run in 10 seconds
			mHandler.postDelayed(this, 10000);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		debug.logV(TAG, "onCreate()");
		
		/* set content view from recorder.xml */
		setContentView(R.layout.recorder);
		
		/* actionbar */ 
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_background));
        
		/* init Google tracker */
		initGATracker();
		
		/* init recording intent if needed */
		if ( intent == null ) {
			intent = new Intent(this, FlipzuRecorderService.class);
		}
		
		/* vumeter */
		if ( vumeter == null ) {
			vumeter = new VUMeter(this);
		}
		
		/* button listeners */
		final Button rec_but = (Button) findViewById(R.id.rec_but);
		rec_but.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// if we're playing, stop
				if (mState == recorderState.STOPPED) {
					startRec();
					return;
				}
	
				// otherwise, start recording and broadcast tasks
				if ( mState == recorderState.RECORDING ) {
					stopRec();
					return;
				}
				
			}		
		});
		
		
		final EditText bcast_title_et = (EditText) findViewById(R.id.bcast_title);		
		bcast_title_et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					startRec();
					return true;
				}
				return false;
			}
		});		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		debug.logV(TAG, "onDestroy()");
		
		/* clean vumeter */
		vumeter = null;

		/* stop tracker */
		tracker.stopSession();

		if ( mState == recorderState.STOPPED ) {
			/* stop recording service */
//			stopService(intent);			
		}			
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		debug.logV(TAG, "onPause()");
		
		/* unregister status updater */
		try {
			unregisterReceiver(broadcastReceiver);	
		} catch ( IllegalArgumentException e ) {
			debug.logE(TAG, "onPause ERROR", e.getCause());
		}
		
		/* stop timer */
		clockHandler.removeCallbacks(mUpdateDurationTask);
		
		/* stop comments/listeners */
		mHandler.removeCallbacks(mUpdateTimeTask);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		debug.logV(TAG, "onResume()");
		
        /* track pageview */
        tracker.trackPageView("/" + this.getLocalClassName());
		
		/* clear service notifications, if any */
		clearNotifications();
		
		/* restore user settings */
		restoreSettings();
		
		/* update Recorder Status */
		sendRequestStatus();
		registerReceiver(broadcastReceiver, new IntentFilter(
				FlipzuRecorderService.INTENT_FILTER));
		
		/* Clock timer for broadcast duration */
		clockHandler.removeCallbacks(mUpdateDurationTask);
		clockHandler.postDelayed(mUpdateDurationTask, 100);
		
		/* comments/listeners */
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 100);
		
		/* twitter and facebook buttons */
		final Button tw_but = (Button) findViewById(R.id.share_tw);
		final Button fb_but = (Button) findViewById(R.id.share_fb);
		
		if ( hasTwitter && shareTwitter ) {
			shareTwitter = true;
			tw_but.setBackgroundResource(R.drawable.twitter_button_push);
			tw_but.setContentDescription(getText(R.string.share_tw_pushed));
		}
		if ( hasFacebook && shareFacebook ) {
			shareFacebook = true;
			fb_but.setBackgroundResource(R.drawable.facebook_button_push);
			fb_but.setContentDescription(getText(R.string.share_fb_pushed));
		}
		
		tw_but.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( hasTwitter ) {
					if ( shareTwitter ) {
						shareTwitter = false;
						tw_but.setBackgroundResource(R.drawable.twitter_button);
						tw_but.setContentDescription(getText(R.string.share_tw));
					} else {
						shareTwitter = true;
						tw_but.setBackgroundResource(R.drawable.twitter_button_push);
						tw_but.setContentDescription(getText(R.string.share_tw_pushed));
					}					
				} else {
					showDialog(BIND_TWITTER_DIALOG_ID);
				}
			}
		});
	
		fb_but.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( hasFacebook ) {
					if ( shareFacebook ) {
						shareFacebook = false;
						fb_but.setBackgroundResource(R.drawable.facebook_button);
						fb_but.setContentDescription(getText(R.string.share_fb));
					} else {
						shareFacebook = true;						
						fb_but.setBackgroundResource(R.drawable.facebook_button_push);
						fb_but.setContentDescription(getText(R.string.share_fb_pushed));
					}					
				} else {
					showDialog(BIND_FACEBOOK_DIALOG_ID);
				}
			}
		});
		
	}
	
	private void sendRequestStatus() {
		debug.logV(TAG, "requestStatus()");

		if ( intent == null ) 
			intent = new Intent(this, FlipzuRecorderService.class);		

		intent.setAction(FlipzuRecorderService.REQUEST_STATUS);
		startService(intent);			
	}
	
	/* initialize Google Analytics tracking object */
	private void initGATracker() {
		/* get analytics singleton */
		tracker = GoogleAnalyticsTracker.getInstance();

		/* start tracker. Dispatch every 60 seconds. */
		tracker.startNewSession("UA-20341887-1", 60, this);
		
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
	}
	
	/* start recording process */
	private void startRec() {
		debug.logV(TAG, "startRec()");
		
		/* set title */
		final EditText bcast_title_et = (EditText) findViewById(R.id.bcast_title);
		bcast_title_et.setFocusable(false);		
		mTitle = bcast_title_et.getText().toString();
		
		if ( mTitle == null ) {
			mTitle = "";
		}
		/* hide soft keyboard */
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(bcast_title_et.getWindowToken(), 0);

		
		/* key management */
		if ( mKey == null ) {
			requestKey();
		} else {
			beginRecording();
		}
		
		/* Clock timer for broadcast duration */
		clockHandler.removeCallbacks(mUpdateDurationTask);
		clockHandler.postDelayed(mUpdateDurationTask, 100);
		
		/* comments/listeners */
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 100);
	}	
	
	/* start actual recording, we need a key for this */
	private void beginRecording() {
		/* track startRec event */
		tracker.trackEvent("Recorder", "Action", "startRec", 0);
		String share = "Nothing";
		if ( shareTwitter && shareFacebook ) {
			share = "TW & FB";
		} else if ( shareTwitter ) {
			share = "TW";
		} else if ( shareFacebook ) {
			share = "FB";
		}
		tracker.setCustomVar(4, "Share", share, 3);

		String title = (String) getText(R.string.live_at) + " http://flipzu.com/" + mUser.getUsername();		
		intent.setAction(FlipzuRecorderService.ACTION_REC);
		intent.putExtra("title", title);
		intent.putExtra("key", mKey);
		startService(intent);
	}
	
	/* request key */
	private void requestKey() {
		debug.logV(TAG, "requestKey()");
		
		/* set state */
		mState = recorderState.CONNECTING;
		
		/* request key */
		AsyncFlipInterface.requestKey(mUser.getToken(), mTitle, shareTwitter, shareFacebook, Recorder.this);
		
		/* refresh UI */
		refreshScreen();
	}
	
	/* stop recording */
	private void stopRec() {
		debug.logV(TAG, "stopRec()");
		
		/* track stopRec event */
		tracker.trackEvent("Recorder", "Action", "stopRec", 0);
		
		/* stop service */
		intent.setAction(FlipzuRecorderService.ACTION_STOP);
		startService(intent);
		
		/* clean our key */
		mKey = null;
	}
	
	/* receiver from the service. We update the UI afterwards */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {			
			updateState(intent);
		}
	};

	/* update local state from service message */
	private void updateState(Intent intent) {		
		String state = intent.getStringExtra("state");
		dState = intent.getIntExtra("err_code", 0);
		mStartTime = intent.getLongExtra("start_time", 0);
		mBcastId = intent.getIntExtra("bcast_id", 0);
		debug.logV(TAG, "updateState() with state " + state + " and error " + dState.toString());

		if (state.equals(FlipzuRecorderService.STATUS_RECORDING)) {
			debug.logV(TAG, "updateState to RECORDING");
			mState = recorderState.RECORDING;
		}		
		
		if (state.equals(FlipzuRecorderService.STATUS_STOPPED)) {
			debug.logV(TAG, "updateState to STOPPED");
			mState = recorderState.STOPPED;
		}
		
		if (state.equals(FlipzuRecorderService.STATUS_CONNECTING)) {
			debug.logV(TAG, "updateState to CONNECTING");
			mState = recorderState.CONNECTING;
		}

		/* refresh UI */
		refreshScreen();
	}
	
	private void refreshScreen() {
		debug.logV(TAG, "refreshScreen()");
		
		try {
			dismissDialog(CONNECTING_DIALOG_ID);
			dismissDialog(DISCONNECTING_DIALOG_ID);
			dismissDialog(AUTH_FAILED_DIALOG_ID);
			dismissDialog(CONNECTION_LOST_DIALOG_ID);
			dismissDialog(NO_INTERNET_DIALOG_ID);
			dismissDialog(MIC_FAILED_DIALOG_ID);
			dismissDialog(SLOW_NETWORK_DIALOG_ID);
			dismissDialog(SLOW_CPU_DIALOG_ID);
			clearNotifications();
		} catch (IllegalArgumentException e) {
			//
		}
		
		final Button rec_but = (Button) findViewById(R.id.rec_but);
		final EditText bcast_title_et = (EditText) findViewById(R.id.bcast_title);
		switch (mState) {
		case RECORDING:
			rec_but.setBackgroundResource(R.drawable.rec_on);
			rec_but.setContentDescription(getText(R.string.stoprecording));
			bcast_title_et.setFocusable(false);
			break;
		case STOPPED:
			rec_but.setBackgroundResource(R.drawable.rec_button);
			rec_but.setContentDescription(getText(R.string.startrecording));
			bcast_title_et.setFocusableInTouchMode(true);
			break;
		case CONNECTING:
			showDialog(CONNECTING_DIALOG_ID);
			break;	
		case STOPPING:
			showDialog(DISCONNECTING_DIALOG_ID);
			bcast_title_et.setFocusableInTouchMode(true);
			break;
		}
		
		if ( dState != null ) {
			switch ( dState ) {
			case FlipzuRecorderService.AUTH_FAILED:
				showDialog(AUTH_FAILED_DIALOG_ID);
				break;
			case FlipzuRecorderService.MIC_FAILED:
				showDialog(MIC_FAILED_DIALOG_ID);
				stopService(intent);
				break;
			case FlipzuRecorderService.SLOW_CPU:
				showDialog(SLOW_CPU_DIALOG_ID);
				stopService(intent);
				break;
			case FlipzuRecorderService.SLOW_NETWORK:
				showDialog(SLOW_NETWORK_DIALOG_ID);
				stopService(intent);
				break;
			case FlipzuRecorderService.NO_CONNECTION:
				showDialog(NO_INTERNET_DIALOG_ID);
				stopService(intent);
				break;
			case FlipzuRecorderService.CONNECTION_LOST:
				showDialog(CONNECTION_LOST_DIALOG_ID);				
				stopService(intent);
				break;
			}
			dState = null;
		}

	}
	
	/* restore user settings */
	private void restoreSettings() {
		/* restore recorder status and user preferences */
		settings = getSharedPreferences(PREFS_NAME, 0);
		String username = settings.getString("username", null);
		String token = settings.getString("token", null);
		Integer state = settings.getInt("recorder_status", recorderState.STOPPED.ordinal());
		hasTwitter = settings.getString("has_twitter", "0").equals("1");
		hasFacebook = settings.getString("has_facebook", "0").equals("1");
				
		mUser = new User();
		
		mUser.setUsername(username);
		mUser.setToken(token);
		mUser.setFacebook(hasFacebook);
		mUser.setTwitter(hasTwitter);
		mState = recorderState.values()[state];		
	}

	@Override
	public void onCommentsReceived(Hashtable<String, String>[] comments) {
		debug.logV(TAG, "onCommentsReceived");
		
		final LinearLayout cc = (LinearLayout) findViewById(R.id.comments_container);

		/* cleanup comments first */
		cc.removeAllViews();
		
		/* get pixel values for various DIPs */
		final float scale = getResources().getDisplayMetrics().density;
//		final int pixel_10 = 10 / (int) (scale + 0.5f);
		final int pixel_5 = 5 / (int) (scale + 0.5f);
//		final int pixel_30 = 30 / (int) (scale + 0.5f);
		final LayoutParams params = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		if (comments != null && comments.length > 0) {
		
			for (int i = 0; i < comments.length; i++) {
				
				LinearLayout cl = new LinearLayout(Recorder.this);					
				cl.setOrientation(LinearLayout.HORIZONTAL);
				cl.setPadding(0, 0, 0, pixel_5);
				
				debug.logD(TAG, "Refresher comment " + comments[i]);
				
				/* comment */
				TextView comment_tv = new TextView(Recorder.this);
				comment_tv.setLayoutParams(params);
				comment_tv.setText(comments[i].get("username") + ": " + comments[i].get("comment"), TextView.BufferType.SPANNABLE);
				comment_tv.setTextColor(Color.parseColor("#656565"));
				Spannable comment_span = (Spannable) comment_tv.getText();
				comment_span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, comment_tv.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				comment_span.setSpan(new ForegroundColorSpan(Color.parseColor("#182e5b")), 0, comments[i].get("username").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				comment_tv.setText(comment_span);
				cl.addView(comment_tv);
				
				cc.addView(cl);				
				
			}

		} else {
			if ( mState == recorderState.RECORDING ) {
				String msg = "You're LIVE! Your broadcast can be heard at http://flipzu.com/" + mUser.getUsername();
				showFlipzuTipsOffline(cc, params, msg);
			} else {
				String msg = "Pick a good and descriptive broadcast title. A great title will attract more listeners!";
				showFlipzuTipsOffline(cc, params, msg);
				msg = "You can disable sharing in Twitter and Facebook by clicking the logo buttons. This is great for testing.";
				showFlipzuTipsOffline(cc, params, msg);
				msg = "Press the \"Start Broadcast\" button and let them hear you!";
				showFlipzuTipsOffline(cc, params, msg);
			}
		}	
	}

	@Override
	public void onIsLiveReceived(boolean isLive) {
		// do nothing
		
	}

	@Override
	public void onRequestKeyReceived(String key) {
		this.mKey = key;
		
		if ( mKey == null ) {
			debug.logV(TAG, "onRequestKeyReceived, got NULL key");
			mState = recorderState.STOPPED;
			dState = FlipzuRecorderService.NO_CONNECTION; 
			refreshScreen();
			return;
		} else {
			if ( mKey.equals("0")) {
				debug.logV(TAG, "onRequestKeyReceived, got 0 key");
				mState = recorderState.STOPPED;
				dState = FlipzuRecorderService.AUTH_FAILED; 
				refreshScreen();
				return;
			} else {
				debug.logV(TAG, "onRequestKeyReceived, got key " + key);	
			}
		}
		
		
		/* start actual recording */
		beginRecording();
	}

	@Override
	public void onResponseReceived(BroadcastDataSet bcast) {
		// Tdo nothing
		
	}
	
	protected Dialog onCreateDialog( int id ) {
		Dialog dialog = null;
		ProgressDialog pDialog;
		AlertDialog aDialog;
		AlertDialog.Builder builder;
		
		switch(id) { 
		case CONNECTING_DIALOG_ID:
			pDialog = new ProgressDialog(Recorder.this);
			pDialog.setTitle(null);
			pDialog.setMessage("Connecting...");
			dialog = pDialog;
			break;
		case DISCONNECTING_DIALOG_ID:
			pDialog = new ProgressDialog(Recorder.this);
			pDialog.setTitle(null);
			pDialog.setMessage("Closing broadcast, please wait...");
			dialog = pDialog;
			break;
		case AUTH_FAILED_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Auth failed. Please logout and try again.")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(AUTH_FAILED_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case CONNECTION_LOST_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Connection lost :-(")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(CONNECTION_LOST_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case NO_INTERNET_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("No Internet connection")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(NO_INTERNET_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case MIC_FAILED_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Microphone initialization failed! Please try restarting the App.")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(MIC_FAILED_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case SLOW_NETWORK_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Network speed is too slow! Disconnected.")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(SLOW_NETWORK_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case SLOW_CPU_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("We can't process audio fast enough, so we're disconnecting. Sorry :(")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(SLOW_CPU_DIALOG_ID);
		        	   clearNotifications();
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case BIND_FACEBOOK_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Please visit http://flipzu.com ('Settings' section) to bind your Facebook account. Then Re-Login on this app.")
			.setCancelable(false)
			.setTitle("Facebook Share")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(BIND_FACEBOOK_DIALOG_ID);
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		case BIND_TWITTER_DIALOG_ID:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Please visit http://flipzu.com ('Settings' section) to bind your Twitter account. Then Re-Login on this app.")
			.setCancelable(false)
			.setTitle("Twitter Share")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dismissDialog(BIND_TWITTER_DIALOG_ID);
		           }
		       });
			aDialog = builder.create();
			dialog = aDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
	
	public void showFlipzuTipsOffline(LinearLayout cc, LayoutParams params, String msg) {	
		final float scale = getResources().getDisplayMetrics().density;
		final int pixel_5 = 5 / (int) (scale + 0.5f);
		
		LinearLayout cl = new LinearLayout(Recorder.this);					
		cl.setOrientation(LinearLayout.HORIZONTAL);
		cl.setPadding(0, 0, 0, pixel_5);				
		
		/* comment */
		String tips_username = "Flipzu Tips";
		
		TextView comment_tv = new TextView(Recorder.this);
		comment_tv.setLayoutParams(params);
		comment_tv.setText(tips_username + ": " + msg, TextView.BufferType.SPANNABLE);
		comment_tv.setTextColor(Color.parseColor("#656565"));
		Spannable comment_span = (Spannable) comment_tv.getText();
		comment_span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, comment_tv.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		comment_span.setSpan(new ForegroundColorSpan(Color.parseColor("#182e5b")), 0, tips_username.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		comment_tv.setText(comment_span);
		cl.addView(comment_tv);				
		
		cc.addView(cl);		
	}

	@Override
	public void onListenersReceived(Integer listeners) {
		debug.logV(TAG, "onListenersReceived got " + listeners);
		
		final TextView listeners_tv = (TextView) findViewById(R.id.listeners_tv);

		listeners_tv.setText(listeners.toString());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ITEM_LOGOUT, 0, R.string.logout)
			.setIcon(R.drawable.ic_menu_revert)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		menu.add(0, MENU_ITEM_SHARE_FLIPZU, 1, R.string.share_flipzu)
		.setIcon(R.drawable.ic_menu_share_flipzu)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);		
		
		menu.add(0, MENU_ITEM_ABOUT, 2, R.string.about)
		.setIcon(R.drawable.ic_menu_info_details)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

		menu.add(0, MENU_ITEM_SHARE, 3, R.string.share)
        .setIcon(R.drawable.ic_action_share)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Recorder.this.finish();
			return true;
		case MENU_ITEM_LOGOUT:
			logoutRecorder();
			return true;
		case MENU_ITEM_ABOUT:
			showAbout();
			return true;
		case MENU_ITEM_SHARE_FLIPZU:
			shareFlipzu();
			return true;
		case MENU_ITEM_SHARE:
			shareBroadcast();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/* shares Flipzu App */
	private void shareFlipzu() {
		
		//create the intent  
		Intent shareIntent =   
		 new Intent(android.content.Intent.ACTION_SEND);  
		  
		//set the type  
		shareIntent.setType("text/plain");  
		  
		//add a subject  
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,   
		 "Check out this cool App");		
		  
		//build the body of the message to be shared  
		String shareMessage = "Flipzu, live audio broadcast from your Android phone: https://market.android.com/details?id=" + APP_PNAME;  
		  
		//add the message  
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,   
		 shareMessage);  
		  
		//start the chooser for sharing  
		startActivity(Intent.createChooser(shareIntent,   
		 getText(R.string.share_app)));  

	}	
		
	/* shares this broadcast */
	private void shareBroadcast() {
		
		//create the intent  
		Intent shareIntent =   
		 new Intent(android.content.Intent.ACTION_SEND);  
		  
		//set the type  
		shareIntent.setType("text/plain");  
		  
		//add a subject  
		/* set title */
		final EditText bcast_title_et = (EditText) findViewById(R.id.bcast_title);
		mTitle = bcast_title_et.getText().toString();
		if ( mTitle == null ) {
			mTitle = "";
		}
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTitle);		
		  
		//build the body of the message to be shared  
		String shareMessage = "I'm broadcasting live on: http://flipzu.com/" + mUser.getUsername();  
		  
		//add the message  
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,   
		 shareMessage);  
		  
		//start the chooser for sharing  
		startActivity(Intent.createChooser(shareIntent,   
		 getText(R.string.share_bcast_with)));  

	}	
		
	/* Logout and go back to Flipzu activity */
	private Void logoutRecorder() {
		/* track "Logout" */
		tracker.trackEvent("Recorder", "Click", "Logout", 0);

		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", null);
		editor.putString("token", null);
		editor.commit();
		Intent loginIntent = new Intent();
		loginIntent.setClassName("com.flipzu.flipzu",
				"com.flipzu.flipzu.Flipzu");
		startActivity(loginIntent);
		Recorder.this.finish();
		return null;
	}

	/* Opens "about" page on flipzu website */
	public void showAbout() {
		/* track "About" */
		tracker.trackEvent("Recorder", "Click", "About", 0);

		Uri uri = Uri.parse("http://static.flipzu.com/about-android.html");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	@Override
	public void onListingReceived(List<BroadcastDataSet> bcastList, Integer list_type) {
		// do nothing		
	}
	
	private void clearNotifications() {
		debug.logV(TAG, "clearNotifications()");
		String ns = Context.NOTIFICATION_SERVICE;
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancel(NOTIF_ID);
	}

	@Override
	public void onUserReceived(FlipUser user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFollowReceived(boolean ret) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnfollowReceived(boolean ret) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBroadcastDeletedReceived(boolean ret) {
		// TODO Auto-generated method stub
		
	}
	
}	

////	Timer vumeter_timer;
////	private static final int VUMETER_REFRESH = 100; // ms, 10 times a second
//	GLSurfaceView mGLSurfaceView = null;

//	/* OpenGL Refresher Timer */
//	class UpdateVUMeterTask extends TimerTask {
//		public void run() {
//			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
//			// debug.logD(TAG, "UpdateVUMeter() called");
//			if (mGLSurfaceView == null) {
//				debug.logW(TAG, "UpdateVUMeter(): surface is null!");
//				return;
//			}
//			mGLSurfaceView.requestRender();
//		}
//	}

////		mGLSurfaceView = (GLSurfaceView) findViewById(R.id.vuMeter);
////		mGLSurfaceView.setRenderer(vumeter);
////		mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

//		/* Timer for VUMeter OpenGL manual refresh */
////		vumeter_timer = new Timer();
////		vumeter_timer.schedule(new UpdateVUMeterTask(), VUMETER_REFRESH,
////				VUMETER_REFRESH);
