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
import java.util.Hashtable;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.ListFragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ListingsFragment extends ListFragment implements
		ResponseListener, ListView.OnScrollListener, ListingsEvents {

	/* debugging tag */
	private static final String TAG = "ListingsFragment";
	private Debug debug = new Debug();

	/* google analytics */
	GoogleAnalyticsTracker tracker;

	/* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";
	private SharedPreferences settings;

	/* broadcast lists */
	private ArrayList<BroadcastDataSet> mBcasts = null;
	private BcastAdapter mAdapter;

	/* default listing length and positions */
	private Integer mFrom = null;
	// private Integer mTo = null;
	private final Integer LIST_LEN = 25;

	/* EOL flag */
	private boolean mEndOfListing = false;

	public Integer mListType;
	public String mSearch;
	public FlipUser mUser = null;

	/* auxiliary */
	private BroadcastDataSet lastBcast = null;

	/* Flipzu Constants */
	private FlipInterface fi = new FlipInterface();

	/* refreshes timestamps */
	private Handler timerHandler = new Handler();
	private Runnable mTimerTask = new Runnable() {
		@Override
		public void run() {
			debug.logV(TAG, "mTimerTask called");

			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			if ( mAdapter != null ) {
				mAdapter.notifyDataSetChanged();
			}
			
			// run again in 1 minute
			timerHandler.postDelayed(this, 60000);
		}
	};

	private Runnable returnRes = new Runnable() {
		@Override
		public void run() {
			debug.logV(TAG, "returnRes called for list " + mListType);

			getSupportActivity().setProgressBarIndeterminateVisibility(
					Boolean.FALSE);
			mAdapter.notifyDataSetChanged();
			if (mFrom == null) {
				try {
					getListView().setSelection(0);
					getListView().requestFocus();
				} catch (Exception e) {
					// pass
				}
			}
			// getListView().setSelection(0);

		}
	};
	
		
	@Override
	public void onResume() {
		super.onResume();
		
		timerHandler.removeCallbacks(mTimerTask);
		timerHandler.postDelayed(mTimerTask, 0);
	}
		

	@Override
	public void onPause() {
		super.onPause();
		
		timerHandler.removeCallbacks(mTimerTask);
	}



	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mFrom = null;
		
		/* init Google Analytics tracker */
		initGATracker();
		
		/* event notifier */
		if (mListType == fi.FRIENDS) {
			this.setEmptyText(getText(R.string.loading));
			EventNotifier.getInstance().setEventFriends(this);
		} else if (mListType == fi.ALL) {
			this.setEmptyText(getText(R.string.loading));
			EventNotifier.getInstance().setEventAll(this);
		} else if (mListType == fi.PROFILE) {
			this.setEmptyText(getText(R.string.loading));
			EventNotifier.getInstance().setEventProfile(this);
		} else if (mListType == fi.USER) {
			this.setEmptyText(getText(R.string.loading));
			EventNotifier.getInstance().setEventUser(this);
		} else if (mListType == fi.HOTTEST) {
			this.setEmptyText(getText(R.string.loading));
			EventNotifier.getInstance().setEventHot(this);
		} else if (mListType == fi.SEARCH) {
			this.setEmptyText("");
			EventNotifier.getInstance().setEventSearch(this);
		}

		/* restore settings object */
		settings = getSupportActivity().getApplicationContext()
				.getSharedPreferences(PREFS_NAME, 0);

        if (savedInstanceState != null && mListType == fi.USER) {
        	String username = savedInstanceState.getString("mUser");
        	String token = settings.getString("token", null);
            AsyncFlipInterface.getUser(username, token, this);
        }

		mBcasts = new ArrayList<BroadcastDataSet>();

		if(mListType == fi.USER){
			this.mAdapter = new BcastAdapter(getSupportActivity()
					.getApplicationContext(), R.layout.bcast_row_user, mBcasts);
		}else{
			this.mAdapter = new BcastAdapter(getSupportActivity()
					.getApplicationContext(), R.layout.bcast_row, mBcasts);
		}
		setListAdapter(this.mAdapter);
		
		refreshListing();
		
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if(mListType == fi.PROFILE){
					final CharSequence[] items = {getString(R.string.delete_broadcast)};
					final int pos = position;
					AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
					
					builder.setItems(items, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int item) {
					    	String id = mBcasts.get(pos).getId();
					    	deleteBroadcast(id);
					    }
					});
					AlertDialog alert = builder.create();
					alert.setCanceledOnTouchOutside(true);
					alert.show();
					return true;
				}
				return true;
			}
		});

		getListView().setOnScrollListener(this);
	}
	
	private void deleteBroadcast(String bcastId) {
		String token = settings.getString("token", null);
		Integer id;
		try {
			id = Integer.parseInt(bcastId);
		} catch (Exception e ) {
			return;
		}
		AsyncFlipInterface.doDeleteBroadcast(id, token, this);
	}

	/* table row onclick */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		BroadcastDataSet bcast = mBcasts.get(position);
		goToPlayer(bcast);
	}

	private void goToPlayer(BroadcastDataSet bcast) {
		debug.logD(TAG, "Listings goToPlayer for " + bcast.getFullUrl());
		Intent recIntent = new Intent();
		Uri data = Uri.parse(bcast.getFullUrl());
		recIntent.setData(data);
		recIntent.setClassName("com.flipzu.flipzu", "com.flipzu.flipzu.Player");
		startActivity(recIntent);
	}

	@Override
	public void goToTopEvent() {
		// TODO
	}

	@Override
	public void refreshEvent() {
		debug.logV(TAG, "refreshEvent for list_type " + mListType + " class "
				+ this.getClass().getName());
		
		mFrom = null;

		clearListing();

		getBroadcasts();
	}

	@Override
	public void refreshSearch(String search) {
		debug.logV(TAG, "refreshSearch, got " + search);
		if (mListType == fi.SEARCH) {
			mSearch = search;
			clearListing();
			getBroadcasts();
		}
	}

	private void refreshListing() {

		clearListing();

		/* fetch broadcasts for listings */
		if(this.mListType == fi.USER ){
			debug.logV(TAG, "refreshing!!!!!!!");
			if(mUser != null){
				debug.logV(TAG, "refreshing not null!!!!!!!");
				getBroadcasts(mUser);
			}
		}
		else{
			getBroadcasts();
		}
	}

	private void getBroadcasts() {
		getBroadcasts(null, null, LIST_LEN);
	}

	private void getBroadcasts(FlipUser user) {
		getBroadcasts(null, null, LIST_LEN, mListType, user);
	}

	private void getBroadcasts(Integer from, Integer to, Integer limit) {
		getBroadcasts(from, to, limit, mListType);
	}

	private void getBroadcasts(Integer from, Integer to, Integer limit, FlipUser user) {
		getBroadcasts(from, to, limit, mListType, user);
	}

	private void getBroadcasts(Integer from, Integer to, Integer limit,
			int list_type) {
		getBroadcasts(from, to, limit, list_type, null);
	}
	
	
	private void getBroadcasts(Integer from, Integer to, Integer limit,
			int list_type, FlipUser profileUser) {
		// mBcasts = new ArrayList<BroadcastDataSet>();

		String username = settings.getString("username", null);
		String token = settings.getString("token", null);
		User user = new User(username, token);

		if (list_type == fi.FRIENDS) {
			debug.logV(TAG, "getting timeline for friends");
			AsyncFlipInterface.getTimelineFriends(user.getToken(), from, to,
					limit, this);
		} else if (list_type == fi.ALL) {
			debug.logV(TAG, "getting timeline for all");
			AsyncFlipInterface.getTimelineAll(user.getToken(), from, to, limit,
					this);
		} else if (list_type == fi.PROFILE) {
			debug.logV(TAG, "getting timeline for profile");
			AsyncFlipInterface.getTimelineProfile(user.getToken(), from, to,
					limit, this);
		} else if (list_type == fi.HOTTEST) {
			debug.logV(TAG, "getting timeline for hottest, from " + from
					+ " to " + to);
			AsyncFlipInterface.getTimelineHottest(user.getToken(), from, to,
					limit, this);
		} else if (list_type == fi.USER) {
			debug.logV(TAG, "getting timeline for user" );
			AsyncFlipInterface.getTimelineUser(user.getToken(), profileUser,from, to,
					limit, this);
		} else if (list_type == fi.SEARCH) {
			if (!mSearch.equals("")) {
				debug.logV(TAG, "getting timeline for search of " + mSearch);
				AsyncFlipInterface.doSearch(user.getToken(), from, to, limit,
						mSearch, this);
			}
		} else {
			debug.logV(TAG, "getting timeline for all");
			AsyncFlipInterface.getTimelineAll(user.getToken(), from, to, limit,
					this);
		}
	}

	private void getMore(Integer from, Integer to, Integer limit) {
		tracker.trackEvent("ListingsFragment", "Action", "getMore", 0);

		if (from != null) {
			debug.logV(TAG, "getMore from " + from.toString());
		}
		if (to != null) {
			debug.logV(TAG, "getMore to " + to.toString());
			mFrom = to;
		}
		if (limit != null) {
			debug.logV(TAG, "getMore limit " + limit.toString());
		}

		/* change title */
		getSupportActivity()
				.setProgressBarIndeterminateVisibility(Boolean.TRUE);

		if(mListType == fi.USER)
			getBroadcasts(from, to, limit, mUser);
		else
			getBroadcasts(from, to, limit);
	}

	private void clearListing() {
		SupportActivity a = getSupportActivity();
		if (a != null)
			a.setProgressBarIndeterminateVisibility(Boolean.TRUE);

		/* clear current view */
		mBcasts.clear();
	}

	/* initialize Google Analytics tracking object */
	private void initGATracker() {
		/* get analytics singleton */
		tracker = GoogleAnalyticsTracker.getInstance();

		/* start tracker. Dispatch every 30 seconds. */
		tracker.startNewSession("UA-20341887-1", 30, getSupportActivity()
				.getApplicationContext());

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
	

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_IDLE:
			// int first = view.getFirstVisiblePosition();
			// int count = view.getChildCount();
			//            
			// for (int i=0; i<count; i++) {
			// LinearLayout ll = (LinearLayout) view.getChildAt(i);
			// ImageView iv = (ImageView) ll.getChildAt(0);
			// iv.setAdjustViewBounds(true);
			// iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
			// if ( iv.getTag() != null ) {
			// String url = mBcasts.get(first+i).getImgUrl();
			// UrlImageViewHelper.setUrlDrawable(iv, url, R.drawable.icon_sq);
			// }
			// LinearLayout ll2 = (LinearLayout) ll.getChildAt(1);
			// TextView username_tv = (TextView) ll2.findViewById(R.id.toptext);
			// TextView title_tv = (TextView) ll2.findViewById(R.id.bottomtext);
			// TextView time_tv = (TextView) ll2.findViewById(R.id.time_str);
			// TextView listen_tv = (TextView)
			// ll2.findViewById(R.id.listeners_tv);
			// RelativeLayout live_lo = (RelativeLayout)
			// ll2.findViewById(R.id.livelayout);
			// TextView lt = (TextView) ll2.findViewById(R.id.livetext);
			// BroadcastDataSet bcast = mBcasts.get(first+i);
			// if ( username_tv != null )
			// username_tv.setText(bcast.getUsername());
			// if ( title_tv != null ) {
			// String title = bcast.getText();
			// if ( title != null )
			// title_tv.setText(title);
			// else
			// title_tv.setText(R.string.empty_title);
			// }
			//            		
			// if ( time_tv != null )
			// time_tv.setText(bcast.getTimeStr());
			// if ( listen_tv != null)
			// listen_tv.setText(bcast.getListens());
			// if ( live_lo != null ) {
			// if ( bcast.isLive() ) {
			// lt.setText(getText(R.string.live));
			// live_lo.setVisibility(RelativeLayout.VISIBLE);
			// } else {
			// live_lo.setVisibility(RelativeLayout.INVISIBLE);
			// lt.setText(getText(R.string.recorded));
			// }
			// }
			// }
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			break;
		case OnScrollListener.SCROLL_STATE_FLING:
			break;
		}
	}

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	class ViewHolder {
		public ImageView iv;
		public TextView tt;
		public TextView bt;
		public TextView time_tv;
		public TextView list_tv;
		public RelativeLayout ll;
	}

	@Override
	public void onCommentsReceived(Hashtable<String, String>[] comments) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onIsLiveReceived(boolean isLive) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onListenersReceived(Integer listeners) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onListingReceived(List<BroadcastDataSet> bcastList,
			Integer listType) {

		debug.logV(TAG, "onListingReceived for list_type " + listType);		
		
		if (bcastList == null && getSupportActivity() != null ) {			
			debug.logV(TAG, "onListingReceived got NULL bcastList");
			// actionBar.setProgressBarVisibility(View.INVISIBLE);
			getSupportActivity().setProgressBarIndeterminateVisibility(
					Boolean.FALSE);
			return;
		}

		if (bcastList.size() == 0) {
			mEndOfListing = true;
			/* show "empty" messages */
			if ( this.isAdded() ) {
				if (mListType == fi.FRIENDS) {
					this.setEmptyText(getText(R.string.empty_friends));
				} else if (mListType == fi.PROFILE) {
					this.setEmptyText(getText(R.string.empty_profile));
				} else if (mListType == fi.HOTTEST) {
					this.setEmptyText(getText(R.string.empty_hot));
				} else if (mListType == fi.SEARCH) {
					this.setEmptyText(getText(R.string.empty_search));
				}				
			}
		} else {
			mEndOfListing = false;
		}

		debug.logV(TAG, "onListingReceived got bcastList size "
				+ bcastList.size());

		for (BroadcastDataSet bcast : bcastList) {
			if (!bcast.isAuthorized()) {
				this.setEmptyText(getText(R.string.empty_auth_failed));
				debug.logV(TAG, "onListingReceived AUTH FAILED!");
			} else {				
				if ( mListType == fi.HOTTEST ) {
					mBcasts.add(bcast);
					lastBcast = null;
				} 
				/* filter repeated broadcasts from the getMore() web services */
				/*
				 * this is because LIVE broadcasts go first, and sometimes OLD
				 * live
				 */
				/* bcasts started beyond LIST_LEN */				
				else {
					/* start of listing */
					if ( mFrom == null ) {
						lastBcast = null; 
					}
					if (lastBcast == null) {
						debug.logV(TAG, "onListingReceived, setting lastBcast ");
						lastBcast = bcast;
						mBcasts.add(bcast);
					} else {
						if (!(!lastBcast.isLive() && bcast.isLive())) {
							debug.logV(TAG, "onListingReceived, adding " + bcast.getUsername());
							mBcasts.add(bcast);
							lastBcast = bcast;
						} // skip rest 						
					}					
				}
				// debug.logV(TAG, "onListingReceived, new mBcasts size " +
				// mBcasts.size());
			}
		}

		SupportActivity a = getSupportActivity();
		if (a != null) {
			debug.logV(TAG, "running returnRes for list_type " + listType);
			a.runOnUiThread(returnRes);
		}
	}

	@Override
	public void onRequestKeyReceived(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResponseReceived(BroadcastDataSet bcast) {
		// TODO Auto-generated method stub

	}

	public class BcastAdapter extends ArrayAdapter<BroadcastDataSet> {

		private ArrayList<BroadcastDataSet> items;
		private int layout;
		private LayoutInflater mInflater;

		public BcastAdapter(Context context, int textViewResourceId,
				ArrayList<BroadcastDataSet> items) {
			super(context, textViewResourceId, items);

			this.items = items;
			this.layout = textViewResourceId;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	// ViewHolder will buffer the assess to the individual fields of the row
    		// layout
        	ViewHolder holder;
        	
        	View v = convertView;
        	if (v == null) {
        		mInflater = (LayoutInflater)getSupportActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        		v = mInflater.inflate(this.layout, null);
        		holder = new ViewHolder();
        		if(this.layout == R.layout.bcast_row)
        			holder.iv = (ImageView) v.findViewById(R.id.user_avatar);
        		holder.tt = (TextView) v.findViewById(R.id.toptext);
        		holder.bt = (TextView) v.findViewById(R.id.bottomtext);
        		holder.time_tv = (TextView) v.findViewById(R.id.time_str);
        		holder.list_tv = (TextView) v.findViewById(R.id.listeners);
        		holder.ll = (RelativeLayout) v.findViewById(R.id.livelayout);
        		v.setTag(holder);
        	} else {
        		holder = (ViewHolder) v.getTag();
        	}
        	
        	if ( items.size() == 0 )
        		return v;
        	
        	BroadcastDataSet o = items.get(position);
        	if (o != null) {
        		/* user avatar */        		
        		if ( holder.iv != null && o.getImgUrl() != null ) {
                	holder.iv.setAdjustViewBounds(true);
                	holder.iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            		UrlImageViewHelper.setUrlDrawable(holder.iv, o.getImgUrl().replace("_reasonably_small", ""), R.drawable.icon_sq);
        		}
        		
        		if (holder.tt != null) {
        			holder.tt.setText(o.getUsername());
        		}
        		if(holder.bt != null){
        			String title = o.getText();
        			if ( title != null )
        				holder.bt.setText(title);
        			else
        				holder.bt.setText(R.string.empty_title);
        		}
        		if ( holder.time_tv != null ) { 
        			holder.time_tv.setText(o.getTimeStr());
        		}
        		if ( holder.list_tv != null ) {
        			holder.list_tv.setText(o.getListens());
        		}
        		if ( holder.ll != null ) {
            		if ( o.isLive() ) {
        				holder.ll.setVisibility(RelativeLayout.VISIBLE);        				
            		} else {
            			holder.ll.setVisibility(RelativeLayout.INVISIBLE);
            		}
        		}
            	if ( position == (items.size()-1)) {
            		if ( !mEndOfListing ) {

            			Integer to;
            			// sort by number of listeners
            			if ( mListType == fi.HOTTEST ) {
            				to = Integer.parseInt(o.getListens());
            			} else { // sort by date
            				to = Integer.parseInt(o.getId());	
            			}
                		
                		getMore(null, to-1 , LIST_LEN);
            		}
            	}        	        	
        	}
        	
        	return v;
        }

		public int getCount() {
			if (mBcasts == null)
				return 0;

			return mBcasts.size();
		}

		public long getItemId(int position) {
			return position;
		}
	}

	@Override
	public void onUserReceived(FlipUser user) {
		this.setUser(user);
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
		refreshListing();		
	}


	public void setUser(FlipUser user) {
		mUser = user;
		refreshListing();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(this.mListType == fi.USER && this.mUser != null)
        	outState.putString("mUser", this.mUser.getUsername());
    }
    

}
