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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.view.Window;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Listings extends FragmentActivity {
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    
	/* debugging tag */
	private static final String TAG = "Listings";
	private Debug debug = new Debug();
	
	/* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private SharedPreferences settings;
    
    /* menus */
    private static final int MENU_ITEM_LOGOUT = 0;
    private static final int MENU_ITEM_ABOUT = 1;
    private static final int MENU_ITEM_REFRESH = 2;
    private static final int MENU_ITEM_BROADCAST = 3;
    private static final int MENU_ITEM_SHARE_FLIPZU = 4;
    
    private final static String APP_PNAME = "com.flipzu.flipzu";
    
    /* tabs position */
    private static final int TAB_ALL = 0;
    private static final int TAB_FRIENDS = 1;
    private static final int TAB_PROFILE = 2;
    private static final int TAB_HOT = 3;
    private static final int TAB_SEARCH = 4;
    
	/* google analytics */
	GoogleAnalyticsTracker tracker;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		/* restore settings object */
		settings = getSharedPreferences(PREFS_NAME, 0);
        
		/* init Google Analytics tracker */
		initGATracker();
		
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.actionbar_tabs_pager);
        
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_background));
        
        ActionBar.Tab tab1 = getSupportActionBar().newTab().setIcon(R.drawable.all);
        ActionBar.Tab tab2 = getSupportActionBar().newTab().setIcon(R.drawable.friends);
        ActionBar.Tab tab3 = getSupportActionBar().newTab().setIcon(R.drawable.me);
        ActionBar.Tab tab4 = getSupportActionBar().newTab().setIcon(R.drawable.hot);
        ActionBar.Tab tab5 = getSupportActionBar().newTab().setIcon(R.drawable.search);       

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
        mTabsAdapter.addTab(tab1, ListingsFragmentAll.class);
    	mTabsAdapter.addTab(tab2, ListingsFragmentFriends.class);
    	mTabsAdapter.addTab(tab3, ListingsFragmentProfile.class);
    	mTabsAdapter.addTab(tab4, ListingsFragmentHottest.class);
    	mTabsAdapter.addTab(tab5, SearchFragment.class);

        if (savedInstanceState != null) {
        	getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("index"));
        }
        AppRater.app_launched(this);
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
            
            menu.add(0, MENU_ITEM_REFRESH, 3, R.string.refresh)
            .setIcon(R.drawable.refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            
            menu.add(0, MENU_ITEM_BROADCAST, 4, R.string.golive)
            .setIcon(R.drawable.golive)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);

		return super.onCreateOptionsMenu(menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_REFRESH:
        case android.R.id.home:
        	int position = getSupportActionBar().getSelectedNavigationIndex();
        	switch ( position ) {
        	case TAB_ALL:
            	EventNotifier.getInstance().signalRefreshAll();
            	break;
        	case TAB_FRIENDS:
            	EventNotifier.getInstance().signalRefreshFriends();
            	break;
        	case TAB_PROFILE:
            	EventNotifier.getInstance().signalRefreshProfile();
            	break;
        	case TAB_HOT:
            	EventNotifier.getInstance().signalRefreshHottest();
            	break;
        	case TAB_SEARCH:
        		EventNotifier.getInstance().signalRefreshSearch();
            	break;
        	}        	
        	return true;
        case MENU_ITEM_LOGOUT:
        	logoutListings();
        	return true;
        case MENU_ITEM_ABOUT:
        	showAbout();
        	return true;
        case MENU_ITEM_BROADCAST:
        	goToRecorder();
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
	
	/* Opens "about" page on flipzu website */
	public void showAbout() {
		/* track "About" */
		tracker.trackEvent("Listings", "Click", "About", 0);

		Uri uri = Uri.parse("http://static.flipzu.com/about-android.html");
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	private void goToRecorder() {
		debug.logD(TAG,"Listings goToRecorder");
		Intent recIntent = new Intent();
    	recIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Recorder");
    	startActivity(recIntent);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		/* stop tracker */
		tracker.stopSession();

		debug.logV(TAG, "onDestroy()");
	}
	
	/* Logout */
	private void logoutListings() {
		/* track "Logout" */
		tracker.trackEvent("Listings", "Click", "Logout", 0);
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", null);
		editor.putString("token", null);
		editor.commit();
		Intent loginIntent = new Intent();
		loginIntent.setClassName("com.flipzu.flipzu",
				"com.flipzu.flipzu.Flipzu");
		startActivity(loginIntent);

		finish();		
	}
	
	
	/* initialize Google Analytics tracking object */
	private void initGATracker() {
		/* get analytics singleton */
		tracker = GoogleAnalyticsTracker.getInstance();

		/* start tracker. Dispatch every 30 seconds. */
		tracker.startNewSession("UA-20341887-1", 30, getApplicationContext());
		
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
	

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
    }
    
    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<String> mTabs = new ArrayList<String>();

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss) {
            mTabs.add(clss.getName());
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), null);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

    	@Override
    	public void onTabSelected(Tab tab, FragmentTransaction ft) {
    		mViewPager.setCurrentItem(tab.getPosition());
    		int pos = tab.getPosition();
    		switch ( pos ) {
    		case TAB_ALL:
    			tab.setIcon(R.drawable.all_active);
    			break;
    		case TAB_FRIENDS:
    			tab.setIcon(R.drawable.friends_active);
    			break;
    		case TAB_PROFILE:
    			tab.setIcon(R.drawable.me_active);
    			break;
    		case TAB_HOT:
    			tab.setIcon(R.drawable.hot_active);
    			break;
    		case TAB_SEARCH:
    			tab.setIcon(R.drawable.search_active);
    			break;
    		}
    	}

    	@Override
    	public void onTabReselected(Tab tab, FragmentTransaction ft) {
    		EventNotifier.getInstance().goToTop();
    	}

    	@Override
    	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    		int pos = tab.getPosition();
    		switch ( pos ) {
    		case TAB_ALL:
    			tab.setIcon(R.drawable.all);
    			break;
    		case TAB_FRIENDS:
    			tab.setIcon(R.drawable.friends);
    			break;
    		case TAB_PROFILE:
    			tab.setIcon(R.drawable.me);
    			break;
    		case TAB_HOT:
    			tab.setIcon(R.drawable.hot);
    			break;
    		case TAB_SEARCH:
    			tab.setIcon(R.drawable.search);
    			break;
    		}
    	}
    }
}

//public class Listings extends FragmentActivity {
//	/* google analytics */
//	GoogleAnalyticsTracker tracker;
//
//	/* application settings */
//	public static final String PREFS_NAME = "FlipzuPrefsFile";
//	private SharedPreferences settings;
//
//	/* debugging tag */
//	private static final String TAG = "Listings";
//	private Debug debug = new Debug();
//	
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		debug.logV(TAG, "Listings, onCreate");
//		
//	    super.onCreate(savedInstanceState);	 
//	    setContentView(R.layout.listings_fragment);	   
//	}
//
////	@Override
////	protected void onResume() {
////		super.onResume();
////
////		/* track pageview */
////		tracker.trackPageView("/" + this.getLocalClassName());
////	}
////
////	@Override
////	protected void onDestroy() {
////		super.onDestroy();
////		/* stop tracker */
////		tracker.stopSession();
////
////		debug.logD(TAG, "Listings, onDestroy() called");
////
////		saveCurrentState();
////	}
////
////	private void saveCurrentState() {
////		SharedPreferences.Editor ed = settings.edit();
////		ed.putInt("selected_list", mListSelection);
////		ed.putString("search_keyword", mSearch);
////		ed.commit();
////	}
////
////	@Override
////	public void onBackPressed() {
////		debug.logD(TAG, "onBackPressed()");
////
////		super.onBackPressed();
////	}
//
//
//
//	/* initialize Google Analytics tracking object */
//	private void initGATracker() {
//		/* get analytics singleton */
//		tracker = GoogleAnalyticsTracker.getInstance();
//
//		/* start tracker. Dispatch every 30 seconds. */
//		tracker.startNewSession("UA-20341887-1", 30, this);
//
//		/* debug GA */
//		tracker.setDebug(false);
//		tracker.setDryRun(false);
//
//		// Determine the screen orientation and set it in a custom variable.
//		String orientation = "unknown";
//		switch (this.getResources().getConfiguration().orientation) {
//		case Configuration.ORIENTATION_LANDSCAPE:
//			orientation = "landscape";
//			break;
//		case Configuration.ORIENTATION_PORTRAIT:
//			orientation = "portrait";
//			break;
//		}
//		tracker.setCustomVar(3, "Screen Orientation", orientation, 2);
//	}
//
//
//
//
//}
