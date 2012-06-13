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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Profile extends FragmentActivity implements ResponseListener{

	private final static String APP_PNAME = "com.flipzu.flipzu";
	
	/* google analytics */
	GoogleAnalyticsTracker tracker;

	/* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private SharedPreferences settings;

	/* debugging tag */
	private static final String TAG = "Profile";
	private Debug debug = new Debug();

	/* User object */
	private User user = null;

	/* menus */
	private static final int MENU_ITEM_LOGOUT = 0;
	private static final int MENU_ITEM_ABOUT = 1;
	private static final int MENU_ITEM_FOLLOW = 2;
	private static final int MENU_ITEM_SHARE = 3;
	private static final int MENU_ITEM_SHARE_FLIPZU = 4;
	
	/* referece to Menu */
	Menu mMenu = null;
	
	/* reference to profile user */
	FlipUser mUser;
	private ListingsFragment frag;
	
	String mUrl;
	String mTitle;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initUser();
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		//String data = this.getIntent().getDataString();
		String username = this.getIntent().getStringExtra("user");
		
		/* start async call in order to fetch user data */
		AsyncFlipInterface.getUser(username, user.getToken(), this);
		
		/* init Google Analytics tracker */
		initGATracker();

		/* set content view from player.xml */
		setContentView(R.layout.profile);

		/* actionbar */
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_background));

		FragmentManager fragmentManager = this.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		frag = new ListingsFragmentUser();
		fragmentTransaction.add(R.id.broadcasts, frag);
		fragmentTransaction.commit();
	}
	
	@Override
	public void onResponseReceived(BroadcastDataSet bcast) {
		// do nothing
	}

	@Override
	public void onIsLiveReceived(boolean isLive) {
		// do nothing
	}

	@Override
	public void onCommentsReceived(Hashtable<String, String>[] comments) {
		// do nothing
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
	public void onListingReceived(List<BroadcastDataSet> bcastList,
			Integer list_type) {
		// do nothing
	}

	@Override
	public void onUserReceived(FlipUser user) {
		this.mUser = user;
		((TextView)findViewById(R.id.username)).setText(user.getUsername());
		((TextView)findViewById(R.id.full_name)).setText(user.getFullname());
		ImageView bg = (ImageView) findViewById(R.id.user_avatar);
		bg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Profile.this, ProfileImage.class);
				i.putExtra("imageUrl", mUser.getAvatarUrl());
				startActivity(i);
			}
		});
		UrlImageViewHelper.setUrlDrawable(bg, user.getAvatarUrl(),
				R.drawable.icon_sq);
		frag.setUser(user);
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
	public void onBroadcastDeletedReceived(boolean ret) {
		// do nothing
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
        .setTitle(R.string.follow)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        mMenu = menu;
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Profile.this.finish();
			return true;
		case MENU_ITEM_LOGOUT:
			logoutProfile();
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

	/* shares the profile */
	private void share() {
		
		if ( mUser == null )
			return;
		
		
		//create the intent  
		Intent shareIntent =   
		 new Intent(android.content.Intent.ACTION_SEND);  
		  
		//set the type  
		shareIntent.setType("text/plain");  
		  
		//add a subject  
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,   
		 mUser.getFullname() + " live on FlipZu");		
		  
		//build the body of the message to be shared  
		String shareMessage = mUser.getUsername() + " profile on Flipzu: http://flipzu.com/" + mUser.getUsername();  
		  
		//add the message  
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,   
		 shareMessage);  
		  
		//start the chooser for sharing  
		startActivity(Intent.createChooser(shareIntent,   
		 getText(R.string.share_profile_with)));  

	}
	
	/* Opens "about" page on flipzu website */
	public void showAbout() {
		/* track "About" */
		tracker.trackEvent("Profile", "Click", "About", 0);

		Uri uri = Uri.parse("http://static.flipzu.com/about-android.html");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	public void followUnfollow() {
		/* track "Follow" */
		tracker.trackEvent("Profile", "Click", "Follow", 0);
		
		if ( mUser != null && user != null ) {
			if ( !mUser.isFollowing())
				AsyncFlipInterface.setFollow(mUser.getUsername(), user.getToken(), this);
			else
				AsyncFlipInterface.setUnfollow(mUser.getUsername(), user.getToken(), this);
		}
	}

	/* Logout */
	private Void logoutProfile() {
		/* track "Logout" */
		tracker.trackEvent("Profile", "Click", "Logout", 0);


		/* stop Profile service */

		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", null);
		editor.putString("token", null);
		editor.commit();
		Intent loginIntent = new Intent();
		loginIntent.setClassName("com.flipzu.flipzu",
				"com.flipzu.flipzu.Flipzu");
		startActivity(loginIntent);
		Profile.this.finish();
		return null;
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

}
