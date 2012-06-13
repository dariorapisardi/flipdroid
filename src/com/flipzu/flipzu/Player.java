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
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Player extends FragmentActivity implements ResponseListener, SeekBar.OnSeekBarChangeListener {
	
    private final static String APP_PNAME = "com.flipzu.flipzu";
    
	/* google analytics */
	GoogleAnalyticsTracker tracker;

	/* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private SharedPreferences settings;

	/* debugging tag */
	private static final String TAG = "Player";
	private Debug debug = new Debug();

	/* User object */
	private User user = null;

	/* comments handler */
	private Handler comHandler = new Handler();

	/* android bug, check if live stream is still live */
	private Handler liveHandler = new Handler();

	/* timer handler */
	private Handler timerHandler = new Handler();

	/* intent for FlipzuPlayerService */
	private Intent intent = null;

	/* avoid duplicate stats */
	private boolean mStatsSent = false;

	/* flag to check if we're getting bcast from settings or live */
	private boolean mSavedBcast = false;

	/* accessibility */
	private final static String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
	private final static String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";

	/* menus */
	private static final int MENU_ITEM_LOGOUT = 0;
	private static final int MENU_ITEM_ABOUT = 1;
	private static final int MENU_ITEM_FOLLOW = 2;
	private static final int MENU_ITEM_SHARE = 3;
	private static final int MENU_ITEM_SHARE_FLIPZU = 4;
	
	/* referece to Menu */
	Menu mMenu = null;
	
	/* reference to broadcaster user */
	FlipUser mUser;
	
	String mUrl;
	String mTitle;
	BroadcastDataSet bcast = null;

	enum playerState {
		STOPPED, PLAYING, LOADING, ERROR, PAUSED, FINISHED
	};

	static playerState mState = playerState.STOPPED;

	private Runnable mTimerTask = new Runnable() {
		@Override
		public void run() {
			debug.logV(TAG, "mTimerTask called");

			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			if (mState == playerState.PLAYING) {
				sendRequestTimer();
			}
			// run again in 1 second
			timerHandler.postDelayed(this, 1000);
		}
	};

	private Runnable mCheckLiveTask = new Runnable() {
		@Override
		public void run() {
			debug.logV(TAG, "mCheckLiveTask called");

			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			if (bcast != null && mState == playerState.PLAYING) {
				AsyncFlipInterface.isLive(bcast.getUsername(), Player.this);
				// run again in 5 seconds
				liveHandler.postDelayed(this, 5000);
			}
		}
	};

	private Runnable mUpdateCommentsTask = new Runnable() {
		@Override
		public void run() {
			debug.logV(TAG, "mUpdateCommentsTask: started");

			if (bcast != null) {
				AsyncFlipInterface.getComments(Integer.parseInt(bcast.getId()),
						Player.this);
			}

			// run in 10 seconds...
			comHandler.postDelayed(this, 10000);
			
			
			/* also update timestamp */
			TextView time_tv = (TextView) findViewById(R.id.time_str);
			if ( bcast != null && bcast.getTimeStr() != null ) {
				time_tv.setText(bcast.getTimeStr());	
			}

			debug.logV(TAG, "mUpdateCommentsTask: finished");
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		/* stop tracker */
		tracker.stopSession();

		/* stop service if we quit recorder and we're paused */
		if (mState == playerState.PAUSED) {
			stopService(intent);
		}

		debug.logV(TAG, "onDestroy()");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// restore User object
		initUser();
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		/* set content view from player.xml */
		setContentView(R.layout.player);
		
		/* set loading progress */
		setProgressBarIndeterminateVisibility(Boolean.TRUE);

		String data = this.getIntent().getDataString();
		
		Integer bcastId;
		if ( data != null ) {
			bcastId = parseUrl(data);
			debug.logV(TAG, "Player, onCreate() with data " + data);
		} else {
			bcastId = getBcastFromSettings();
			debug.logV(TAG, "Player, onCreate() with saved bcast " + bcastId);
		}
		
		/* start async call in order to fetch bcast data */
		AsyncFlipInterface.getBroadcast(bcastId, Player.this);
		
		/* init Google Analytics tracker */
		initGATracker();
		
		/* actionbar */ 
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_background));

		/* play/pause button */
		final Button toggle_btn = (Button) findViewById(R.id.play_toggle_btn);
		toggle_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				debug.logV(TAG, "onClick with state " + mState.toString());
				switch (mState) {
				case LOADING:
				case PLAYING:
					if (Player.this.bcast != null && Player.this.bcast.isLive()) {
						mState = playerState.STOPPED;
						updateButton();
						setLiveVisible(false);
						stopService(intent);
					} else {
						sendPause();
					}
					break;
				case STOPPED:
					sendPlay();
					break;
				case ERROR:
					sendStop();
					sendPlay();
					break;
				case PAUSED:
					sendResume();
					break;
				}

			}
		});

		/* admob */
		if (user != null && !user.isPremium()) {
			debug.logV(TAG, "onCreate, normal user, showing ads");
			showAdmob();
		} else {
			debug.logV(TAG, "onCreate, premium user, no ads");
		}
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) { 
			debug.logV(TAG, "entered onReceive()");
			if(intent.getBooleanExtra("seekBarUpdate", false)){
				SeekBar seekBar = (SeekBar)findViewById(R.id.seekbar);
				seekBar.setMax(intent.getIntExtra("total", 0));
				seekBar.setProgress(intent.getIntExtra("currentPosition", seekBar.getProgress()));
				Log.e("PLAYER", intent.getIntExtra("total", 0) + " - " + intent.getIntExtra("bufferPercent", 0) + " - " + intent.getIntExtra("currentPosition", seekBar.getProgress()));

				return;
			}
			if(intent.getBooleanExtra("bufferUpdate", false)){
				SeekBar seekBar = (SeekBar)findViewById(R.id.seekbar);
				seekBar.setVisibility(View.VISIBLE);
				seekBar.setMax(intent.getIntExtra("total", 0));
				seekBar.setSecondaryProgress(intent.getIntExtra("bufferPercent", 0));
				return;
			}
			updateState(intent);
		}
	};

	@Override 
	public void onResume() {
		super.onResume();

		debug.logV(TAG, "onResume()");
		
        /* track pageview */
        tracker.trackPageView("/" + this.getLocalClassName());

		if (!isOnline()) {
			setLiveVisible(false);
			if (intent == null)
				intent = new Intent(this, FlipzuPlayerService.class);
			stopService(intent);
		}

		// request player update.
		sendRequestStatus();
		registerReceiver(broadcastReceiver, new IntentFilter(
				FlipzuPlayerService.INTENT_FILTER));

		if (bcast != null) {
			AsyncFlipInterface.getComments(Integer.parseInt(bcast.getId()),
					Player.this);
			timerHandler.postDelayed(mTimerTask, 1000);
			if(!bcast.isLive()){
				SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
				seekBar.setVisibility(View.VISIBLE);
				seekBar.setOnSeekBarChangeListener(this);
			}
		}
		comHandler.postDelayed(mUpdateCommentsTask, 10000);
	}

	@Override
	public void onPause() {
		super.onPause();
		debug.logV(TAG, "onPause()");
		try {
			unregisterReceiver(broadcastReceiver);
		} catch (IllegalArgumentException e) {
			debug.logE(TAG, "onPause ERROR", e.getCause());
		}

		comHandler.removeCallbacks(mUpdateCommentsTask);
		liveHandler.removeCallbacks(mCheckLiveTask);
		timerHandler.removeCallbacks(mTimerTask);
	}

	private void updateState(Intent intent) {
		String state = intent.getStringExtra("state");
		debug.logV(TAG, "entered updateState()");
		if (state.equals(FlipzuPlayerService.STATUS_PLAYING)) {
			debug.logV(TAG, "updateState to PLAYING");
			mState = playerState.PLAYING;
			sendPlayStat();
			if (bcast != null && bcast.isLive()) {
				setLiveVisible(true);
				liveHandler.removeCallbacks(mCheckLiveTask);
				liveHandler.postDelayed(mCheckLiveTask, 5000);
			} else {
				timerHandler.removeCallbacks(mTimerTask);
				timerHandler.postDelayed(mTimerTask, 1000);
			}
		} else if (state.equals(FlipzuPlayerService.STATUS_STOPPED)) {
			debug.logV(TAG, "updateState to STOPPED");
			mState = playerState.STOPPED;
			stopService(this.intent);
		} else if (state.equals(FlipzuPlayerService.STATUS_LOADING)) {
			debug.logV(TAG, "updateState to LOADING");
			mState = playerState.LOADING;
		} else if (state.equals(FlipzuPlayerService.STATUS_ERROR)) {
			debug.logV(TAG, "updateState to ERROR");
			mState = playerState.ERROR;
		} else if (state.equals(FlipzuPlayerService.STATUS_PAUSED)) {
			debug.logV(TAG, "updateState to PAUSED");
			mState = playerState.PAUSED;
		} else if (state.equals(FlipzuPlayerService.STATUS_FINISHED)) {
			debug.logV(TAG, "updateState to PAUSED");
			mState = playerState.FINISHED;
		} else if (state.equals(FlipzuPlayerService.STATUS_UPDATE)) {
			debug.logV(TAG, "updateState to UPDATE");
			String position = intent.getStringExtra("position");
			String duration = intent.getStringExtra("duration");
			updateTimer(position, duration);
		}

		updateButton();
	}

	/* initialize Google Analytics tracking object */
	private void initGATracker() {
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
	}

	private void sendPause() {
		intent.setAction(FlipzuPlayerService.ACTION_PAUSE);
		startService(intent);
	}

	private void sendStop() {
		intent.setAction(FlipzuPlayerService.ACTION_STOP);
		startService(intent);
	}

	private void sendResume() {
		intent.setAction(FlipzuPlayerService.ACTION_RESUME);
		startService(intent);
	}

	private void sendPlay() {
		if (mUrl == null)
			return;

		if (intent == null)
			intent = new Intent(this, FlipzuPlayerService.class);

		intent.setAction(FlipzuPlayerService.ACTION_PLAY);
		Uri data = Uri.parse(mUrl);
		debug.logV(TAG, "sendPlay, got data " + data.toString());
		intent.setData(data);
		intent.putExtra("title", mTitle);
		if (bcast.isLive()) {
			intent.putExtra("live", true);
		} else {
			intent.putExtra("live", false);
			if(!bcast.isLive()){
				SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
				seekBar.setVisibility(View.VISIBLE);
				seekBar.setOnSeekBarChangeListener(this);
			}
		}
		startService(intent);

	}

	private void sendRequestStatus() {
		debug.logV(TAG, "requestStatus()");

		if (intent == null)
			intent = new Intent(this, FlipzuPlayerService.class);

		intent.setAction(FlipzuPlayerService.REQUEST_STATUS);
		startService(intent);
	}

	private void sendRequestTimer() {
		debug.logV(TAG, "requestTimer()");

		if (intent == null)
			intent = new Intent(this, FlipzuPlayerService.class);

		intent.setAction(FlipzuPlayerService.REQUEST_TIMER);
		startService(intent);

	}

	private void sendMediaPlayerSeek(int position) {
		debug.logV(TAG, "requestTimer()");

		if (intent == null)
			intent = new Intent(this, FlipzuPlayerService.class);

		intent.setAction(FlipzuPlayerService.MEDIAPLAYER_SEEK);
		intent.putExtra("position", position);
		startService(intent);

	}

	private Integer parseUrl(String url) {
		Integer ret = 0;

		if (url == null)
			return ret;

		// remove url and username
		String aux = url.substring("http://android.flipzu.com/".length());
		if (aux != null) {
			String[] aux2 = aux.split("/");
			if (aux2.length == 2) {
				String id_str;
				if (aux2[1].contains("?")) {
					id_str = aux2[1].split("\\?")[0];
				} else {
					id_str = aux2[1];
				}

				ret = Integer.parseInt(id_str);
			}
		}
		return ret;
	}

	private String getCurrentUrl(BroadcastDataSet bcast) {
		if (bcast == null)
			return null;

		if (bcast.isLive()) {
			return bcast.getLiveaudioUrl();
		}

		return bcast.getAudioUrl();
	}

	private void saveBcast(BroadcastDataSet bcast) {
		if ( bcast == null || bcast.getId() == null ) 
			return;
		
		settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("last_bcast_id", Integer.parseInt(bcast.getId()));
		editor.commit();
	}

	private Integer getBcastFromSettings() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		Integer bcastId = settings.getInt("last_bcast_id", 0);

		debug.logV(TAG,
				"getBcastFromSettings got last bcast id from settings: "
						+ bcastId);

		return bcastId;
	}

	private void updateButton() {
		Button but = (Button) findViewById(R.id.play_toggle_btn);
		switch (mState) {
		case PLAYING:
			but.clearAnimation();
			but.setBackgroundResource(R.drawable.pause_button);
			break;
		case FINISHED:
			setFinishedBanner(true);
			break;
		case STOPPED:
		case PAUSED:
			but.clearAnimation();
			but.setBackgroundResource(R.drawable.play_button);
			break;
		case LOADING:
			if (bcast != null && !bcast.isLive()) {
				setFinishedBanner(false);
			}
			but.setBackgroundResource(R.drawable.loading_button);
			but.startAnimation(AnimationUtils.loadAnimation(Player.this,
					R.anim.rotate));
			break;
		case ERROR:
			but.clearAnimation();
			but.setBackgroundResource(R.drawable.play_button);
			break;
		}
	}

	private boolean isNewBcast(BroadcastDataSet bcast) {
		Integer saved_bcast = getBcastFromSettings();

		if (bcast == null || saved_bcast == null)
			return true;

		if (bcast.getId() == null) {
			return true;
		}

		if (saved_bcast == Integer.parseInt(bcast.getId())) {
			debug.logV(TAG, "isNewBcast, returning false");
			return false;
		}

		debug.logV(TAG, "isNewBcast, returning true");
		return true;
	}

	private Bitmap drawable_from_url(String url, int width, int height) {
		Bitmap x;

		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse resp;
		InputStream input = null;
		try {
			resp = client.execute(get);
			HttpEntity entity = resp.getEntity();
			debug.logV(TAG, "drawable_from_url " + url
					+ " Image Content Length: " + entity.getContentLength());
			input = entity.getContent();
		} catch (ClientProtocolException e1) {
			return null;
		} catch (IOException e1) {
			return null;
		}

		x = BitmapFactory.decodeStream(input);

		return x;

	}

	private Drawable scaleBitmap(Bitmap x, int width, int height) {

		Drawable d = null;
		debug.logV(TAG, "drawable_from_url layout" + width + "*" + height);
		if (x == null)
			debug.logV(TAG, "X IS NULL");

		if (x == null)
			return null;

		d = new BitmapDrawable(x);

		if (width > 0 && height > 0 && x != null) {
			if (width > x.getWidth())
				width = x.getWidth();
			if (height > x.getHeight())
				height = x.getHeight();
			Bitmap cropped = Bitmap.createBitmap(x, 0, 0, width, height);
			d = new BitmapDrawable(cropped);
		} else {
			d = new BitmapDrawable(x);
		}

		// opacity
		d.setAlpha(122);

		return d;
	}

	AsyncTask<Void, Void, Bitmap> downloader = new AsyncTask<Void, Void, Bitmap>() {
		@Override
		protected Bitmap doInBackground(Void... params) {
			LinearLayout bg = (LinearLayout) findViewById(R.id.comments_layout);
			if (bcast == null)
				return null;

			return drawable_from_url(bcast.getImgUrl(), bg.getWidth(), bg
					.getHeight());
		}

		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				ScrollView bg = (ScrollView) findViewById(R.id.comments_scroll);
				debug.logV(TAG, "onPostExecute, " + bg.getWidth() + " "
						+ bg.getHeight());
				bg.setBackgroundDrawable(scaleBitmap(result, bg.getWidth(), bg
						.getHeight()));
			}

		}
	};

	private void cleanComments() {
		final LinearLayout com_layout = (LinearLayout) findViewById(R.id.comments_layout);
		com_layout.removeAllViews();
	}

	private void displayComment(Hashtable<String, String> comment) {
		if (comment == null)
			return;

		final LinearLayout com_layout = (LinearLayout) findViewById(R.id.comments_layout);
		TextView comment_tv = new TextView(Player.this);
		comment_tv.setText(comment.get("username") + ": "
				+ comment.get("comment"), TextView.BufferType.SPANNABLE);
		comment_tv.setTextColor(Color.parseColor("#656565"));
		Spannable comment_span = (Spannable) comment_tv.getText();
		comment_span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
				comment_tv.getText().length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		comment_span.setSpan(new ForegroundColorSpan(Color
				.parseColor("#597490")), 0, comment.get("username").length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		comment_tv.setText(comment_span);
		com_layout.addView(comment_tv);	
	}

	private void postComment() {
		/* track postComment event */
		tracker.trackEvent("Player", "Action", "postComment", 0);
		
		final EditText comment_et = (EditText) findViewById(R.id.post_comment_et);

		String comment = comment_et.getText().toString();
		comment_et.setText("");

		debug.logV(TAG, "postComment() " + comment);

		if (bcast != null) {
			/* hide soft keyboard */
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(comment_et.getWindowToken(), 0);

			AsyncFlipInterface.postComment(user, comment, Integer
					.parseInt(bcast.getId()));
			AsyncFlipInterface.getComments(Integer.parseInt(bcast.getId()), Player.this);
		}
	}

	private void initUser() {
		/* restore recorder status and user preferences */
		settings = getSharedPreferences(PREFS_NAME, 0);
		String username = settings.getString("username", null);
		String token = settings.getString("token", null);
		String is_premium = settings.getString("is_premium", "0");

		/* create User object */
		user = new User(username, token);
		if (is_premium.equals("1")) {
			user.setPremium(true);
			debug.logV(TAG, "initUser, got premium user");
		}
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
        
        menu.add(0, MENU_ITEM_FOLLOW, 4, R.string.follow)
        .setIcon(R.drawable.friends)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        mMenu = menu;
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Player.this.finish();
			return true;
		case MENU_ITEM_LOGOUT:
			logoutPlayer();
			return true;
		case MENU_ITEM_ABOUT:
			showAbout();
			return true;
		case MENU_ITEM_FOLLOW:
			followUnfollow();
			return true;
		case MENU_ITEM_SHARE:
			share();
			return true;
		case MENU_ITEM_SHARE_FLIPZU:
			shareFlipzu();
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
	
	/* shares the broadcast */
	private void share() {
		
		if ( mUser == null || bcast == null )
			return;
		
		
		//create the intent  
		Intent shareIntent =   
		 new Intent(android.content.Intent.ACTION_SEND);  
		  
		//set the type  
		shareIntent.setType("text/plain");  
		
		//add a subject  
	    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,   
		mUser.getFullname() + " live on FlipZu");
		
		String text = bcast.getText();
		if ( text == null ) {
			text = mUser.getUsername() + " live on FlipZu";
		}
		  
		//build the body of the message to be shared  
		String shareMessage = text + " - http://fzu.fm/" + bcast.getId();  
		  
		//add the message  
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,   
		 shareMessage);  
		  
		//start the chooser for sharing  
		startActivity(Intent.createChooser(shareIntent,   
		 getText(R.string.share_bcast_with)));  

	}

	/* Opens "about" page on flipzu website */
	public void showAbout() {
		/* track "About" */
		tracker.trackEvent("Player", "Click", "About", 0);

		Uri uri = Uri.parse("http://static.flipzu.com/about-android.html");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	public void followUnfollow() {
		/* track "Follow" */
		tracker.trackEvent("Player", "Click", "Follow", 0);
		
		if ( mUser != null && user != null ) {
			if ( !mUser.isFollowing())
				AsyncFlipInterface.setFollow(mUser.getUsername(), user.getToken(), this);
			else
				AsyncFlipInterface.setUnfollow(mUser.getUsername(), user.getToken(), this);
		}
	}

	/* Logout */
	private Void logoutPlayer() {
		/* track "Logout" */
		tracker.trackEvent("Player", "Click", "Logout", 0);


		/* stop player service */
		stopService(intent);

		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", null);
		editor.putString("token", null);
		editor.commit();
		Intent loginIntent = new Intent();
		loginIntent.setClassName("com.flipzu.flipzu",
				"com.flipzu.flipzu.Flipzu");
		startActivity(loginIntent);
		Player.this.finish();
		return null;
	}

	private void showAdmob() {
		AdView adView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest();
		// adRequest.addTestDevice("9E2EB45B6E9B69B99CA84FC580A42654"); //
		// Flipout
		// adRequest.addTestDevice("A2306398D79D0E65DA63F366B0F53899"); //
		// Milestone
		// adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
		adView.loadAd(adRequest);
	}

	private void setLiveVisible(boolean visible) {
		
		ImageView live_v = (ImageView) findViewById(R.id.livetag);
		ImageView live_vb = (ImageView) findViewById(R.id.livetag_bottom);
		TextView time_str = (TextView) findViewById(R.id.time_str);

		if (visible) {
			int color = Color.parseColor("#FFFFFF");
			updateBanner("",color);
			live_v.setVisibility(View.VISIBLE);
			live_vb.setVisibility(View.VISIBLE);
			time_str.setVisibility(View.INVISIBLE);
			findViewById(R.id.seekbar).setVisibility(View.INVISIBLE);
		} else {
			live_v.setVisibility(View.INVISIBLE);
			live_vb.setVisibility(View.INVISIBLE);
//			time_str.setVisibility(View.VISIBLE);
		}
	}

	private void setFinishedBanner(boolean finished) {

		int color = Color.parseColor("#FFFFFF");
		if (finished) {
			setLiveVisible(false);
//			if (isScreenReaderActive()) {
//				XXX
//				showFinishedDialog();
//			}
			updateBanner(getText(R.string.flipzu_finished).toString(), color);
		} else {
			updateBanner("", color);
		}
	}

	private void updateTimer(String position, String duration) {
		debug.logV(TAG, "updateTimer called with " + position + "/" + duration);
		int color = Color.parseColor("#FFFFFF");
		updateBanner(position + "/" + duration, color);
	}

	private void updateBanner(String msg, int color) {
		final TextView timer_tv = (TextView) findViewById(R.id.timer);
		timer_tv.setTextColor(color);
		timer_tv.setText(msg);
	}

	private void sendPlayStat() {
		if (bcast != null && !mStatsSent) {
			AsyncFlipInterface.playAircast(Integer.parseInt(bcast.getId()));
			mStatsSent = true;
		}
	}

	@Override
	public void onResponseReceived(BroadcastDataSet bcast) {
		
		/* unset loading progress */
		setProgressBarIndeterminateVisibility(Boolean.FALSE);
		
		if (bcast != null) {
			debug.logV(TAG, "onResponseReceived called with bcast "
					+ bcast.getId());
		} else {
			debug.logV(TAG, "onResponseReceived called with bcast NULL");
		}

		boolean newBcast = false;
		if (bcast != null) {
			/* start async call to fetch user data */
			AsyncFlipInterface.getUser(bcast.getUsername(), user.getToken(), this);

			if (isNewBcast(bcast)) {
				debug.logV(TAG, "onResponseReceived, is a new broadcast");
				saveBcast(bcast);
				newBcast = true;
			} else {
				debug.logV(TAG, "onResponseReceived, is not a new broadcast");
			}
		} else {
			// try again with bcast from settings...
			if (!mSavedBcast) {
				mSavedBcast = true;
				AsyncFlipInterface.getBroadcast(getBcastFromSettings(), this);
			}
			return;
		}

		this.bcast = bcast;

		mUrl = getCurrentUrl(bcast);
		if (bcast != null) {
			mTitle = bcast.getUsername();
			if (bcast.getText() != null)
				mTitle += " - " + bcast.getText();
		}

		debug.logV(TAG, "onResponseReceived, got URL " + mUrl);

		/* layout */
		ImageView bg = (ImageView) findViewById(R.id.user_avatar);

		final TextView title_tv = (TextView) findViewById(R.id.title);
		final TextView time_tv = (TextView) findViewById(R.id.time_str);
		final TextView listeners_tv = (TextView) findViewById(R.id.listeners);
		final TextView username_tv = (TextView) findViewById(R.id.username);
		username_tv.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Player.this, Profile.class);
				i.putExtra("user", ((TextView)findViewById(R.id.username)).getText());
				startActivity(i);
			}
		});

		bg.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Player.this, Profile.class);
				i.putExtra("user", ((TextView)findViewById(R.id.username)).getText());
				startActivity(i);
			}
		});


		final TextView bottom_title_tv = (TextView) findViewById(R.id.player_title_bottom);

		// fix padding for player
//		ScrollingTextView sc_tv = (ScrollingTextView) findViewById(R.id.actionbar_title);
//		sc_tv.setPadding(35, 0, 0, 0);

		/* action bar */
//		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
//		actionBar.setHomeLogo(R.drawable.logo2);

		if (bcast != null) {
			UrlImageViewHelper.setUrlDrawable(bg, bcast.getImgUrl(),
					R.drawable.icon_sq);
			getSupportActionBar().setTitle(bcast.getUsername());
			username_tv.setText(bcast.getUsername());
			CharSequence title = bcast.getText();
			if (title == null)
				title = getText(R.string.empty_title);
			title_tv.setText(title);
			bottom_title_tv.setText(mTitle);
			bottom_title_tv.setSelected(true); // hack to make text scroll
			if ( bcast.getTimeStr() != null ) {
				time_tv.setText(bcast.getTimeStr());	
			}
			if ( bcast.getListens() == null ) {
				// maybe erased? Go back...
				Player.this.finish();
			}
			try {
				Integer listens = Integer.parseInt(bcast.getListens()) + 1;
				listeners_tv.setText(listens.toString());
			} catch ( NumberFormatException e ) {
				Player.this.finish();
			}
		} 
		
		// EditText comment listener
		final EditText comment_et = (EditText) findViewById(R.id.post_comment_et);
		comment_et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					postComment();
					return true;
				}
				return false;
			}
		});

		/* start bcast if it's a new one */
		if (newBcast) {
			debug.logV(TAG, "onResponseReceived, newBcast, sending PLAY");
			sendPlay();
		} else {
			if (mState == playerState.STOPPED)
				sendPlay();
		}

		/* stats thread */
		if (bcast != null) {
			if ( bcast.getId() != null ) {
				AsyncFlipInterface.getComments(Integer.parseInt(bcast.getId()),
						Player.this);				
			}
			if (bcast.isLive()) {
				if (mState == playerState.PLAYING)
					setLiveVisible(true);
				liveHandler.removeCallbacks(mCheckLiveTask);
				liveHandler.postDelayed(mCheckLiveTask, 5000);
			} else {
				timerHandler.removeCallbacks(mTimerTask);
				timerHandler.postDelayed(mTimerTask, 1000);
			}
		}
	}

	/* check if we have internet connectivity */
	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			debug.logV(TAG, "isOnline, returning true");
			return true;
		}
		debug.logV(TAG, "isOnline, returning false");
		return false;
	}

	@Override
	public void onIsLiveReceived(boolean isLive) {
		if (!isLive) {
			debug.logV(TAG, "onIsLiveReceived, got OFFLINE");
			setFinishedBanner(true);
			mState = playerState.STOPPED;
			updateButton();
			sendStop();
		} else {
			debug.logV(TAG, "onIsLiveReceived, got LIVE");
			setLiveVisible(true);
		}
	}

	@Override
	public void onCommentsReceived(Hashtable<String, String>[] comments) {
		debug.logV(TAG, "onCommentsReceived called");
		if (comments != null) {
			cleanComments();

			for (int i = 0; i < comments.length; i++) {
				displayComment(comments[i]);
			}
		}
	}

	private boolean isScreenReaderActive() {
		// Restrict the set of intents to only accessibility services that have
		// the category FEEDBACK_SPOKEN (aka, screen readers).
		Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
		screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
		List<ResolveInfo> screenReaders = getPackageManager()
				.queryIntentServices(screenReaderIntent, 0);
		ContentResolver cr = getContentResolver();
		Cursor cursor = null;
		int status = 0;
		for (ResolveInfo screenReader : screenReaders) {
			// All screen readers are expected to implement a content provider
			// that responds to
			// content://<nameofpackage>.providers.StatusProvider
			cursor = cr.query(Uri.parse("content://"
					+ screenReader.serviceInfo.packageName
					+ ".providers.StatusProvider"), null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
				// These content providers use a special cursor that only has
				// one element,
				// an integer that is 1 if the screen reader is running.
				status = cursor.getInt(0);
				cursor.close();
				if (status == 1) {
					return true;
				}
			}
		}
		return false;
	}

	private void showFinishedDialog() {
		AlertDialog aDialog;
		AlertDialog.Builder builder;
		builder = new AlertDialog.Builder(this);
		builder.setMessage("FlipZu Finished").setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		aDialog = builder.create();
		Dialog dialog = aDialog;
		dialog.show();
	}

	@Override
	public void onRequestKeyReceived(String key) {
		// do nothing
	}

	@Override
	public void onListenersReceived(Integer listeners) {
		// do nothing
	}

	@Override
	public void onListingReceived(List<BroadcastDataSet> bcastList, Integer list_type) {
		// do nothing
	}

	@Override
	public void onUserReceived(FlipUser user) {		
		debug.logV(TAG, "onUserReceived, got " + user.getUsername());
		
		mUser = user;
		
		if(mMenu != null){
			MenuItem item = mMenu.findItem(MENU_ITEM_FOLLOW);
			
			if ( user.isFollowing() ) {
				item.setIcon(R.drawable.friends_active);
				item.setTitle(R.string.unfollow);
			} else {
				item.setIcon(R.drawable.friends);
				item.setTitle(R.string.follow);
			}
		}
	}

	@Override
	public void onFollowReceived(boolean ret) {
		debug.logV(TAG, "onFollowReceived " + ret);
		if ( ret ) {
			MenuItem item = mMenu.findItem(MENU_ITEM_FOLLOW);
			item.setIcon(R.drawable.friends_active);
			item.setTitle(R.string.unfollow);
			mUser.setFollowing(true);
		}
	}

	@Override
	public void onUnfollowReceived(boolean ret) {
		debug.logV(TAG, "onUnfollowReceived " + ret);
		if ( ret ) {
			MenuItem item = mMenu.findItem(MENU_ITEM_FOLLOW);
			item.setIcon(R.drawable.friends);
			item.setTitle(R.string.follow);
			mUser.setFollowing(false);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
        if (fromUser) {
        	Log.e("PLAYER", "MEDIAPLAYER PROGRESS " + progress);
            sendMediaPlayerSeek(progress);
        }	
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onBroadcastDeletedReceived(boolean ret) {
		// TODO Auto-generated method stub
		
	}

}
