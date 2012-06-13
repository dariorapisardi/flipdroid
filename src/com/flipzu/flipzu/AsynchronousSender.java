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
import java.util.Hashtable;
import java.util.List;

import android.os.Handler;

public class AsynchronousSender extends Thread {
	private static final String TAG = "AsynchronousSender";
	private Debug debug = new Debug();

	private FlipInterface fi = new FlipInterface();

	private String username;
	private Integer bcastId;
	private User user;
	private String comment;
	private String token;
	private String title;
	private boolean shareTW;
	private boolean shareFB;
	private String search;
	private String profile_user;

	private boolean setFollow = false;
	private Handler setFollowHandler;
	private CallbackWrapper setFollowWrapper;
	private boolean setUnfollow = false;
	private Handler setUnfollowHandler;
	private CallbackWrapper setUnfollowWrapper;
	private boolean getUser = false;
	private Handler getUserHandler;
	private CallbackWrapper getUserWrapper;
	private boolean getBcast = false;
	private Handler getBcastHandler;
	private CallbackWrapper getBcastWrapper;
	private boolean getIsLive = false;
	private Handler isLiveHandler;
	private CallbackWrapper isLiveWrapper;
	private boolean playAircast = false;
	private boolean postComment = false;
	private boolean getComments = false;
	private boolean isRequestKey = false;
	private boolean getListeners = false;
	private boolean getTimelineAll = false;
	private boolean getTimelineHottest = false;
	private boolean getTimelineFriends = false;
	private boolean getTimelineProfile = false;
	private boolean getTimelineUser = false;
	private boolean doSearch = false;
	private boolean doDeleteBroadcast = false;
	private Integer from;
	private Integer to;
	private Integer limit;
	private Handler getCommentsHandler;
	private CallbackWrapper getCommentsWrapper;
	private Handler requestKeyHandler;
	private CallbackWrapper requestKeyWrapper;
	private Handler getListenersHandler;
	private CallbackWrapper getListenersWrapper;
	private Handler getTimelineAllHandler;
	private CallbackWrapper getTimelineAllWrapper;
	private Handler getTimelineHottestHandler;
	private CallbackWrapper getTimelineHottestWrapper;
	private Handler getTimelineFriendsHandler;
	private CallbackWrapper getTimelineFriendsWrapper;
	private Handler getTimelineProfileHandler;
	private CallbackWrapper getTimelineProfileWrapper;
	private Handler getTimelineUserHandler;
	private CallbackWrapper getTimelineUserWrapper;
	private Handler doSearchHandler;
	private CallbackWrapper doSearchWrapper;
	private Handler doDeleteBroadcastHandler;
	private CallbackWrapper doDeleteBroadcastWrapper;

	protected AsynchronousSender() {
	}
	
	public void setFollow(String username, String token, Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setFollow called");
		this.setFollowHandler = handler;
		this.setFollowWrapper = wrapper;
		this.username = username;
		this.token = token;
		setFollow = true;
	}
	
	public void setUnfollow(String username, String token, Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setUnfollow called");
		this.setUnfollowHandler = handler;
		this.setUnfollowWrapper = wrapper;
		this.username = username;
		this.token = token;
		setUnfollow = true;
	}

	public void setUser(String username, String token, Handler handler,
			CallbackWrapper wrapper) {
		debug.logV(TAG, "setUser called");
		this.getUserHandler = handler;
		this.getUserWrapper = wrapper;
		this.username = username;
		this.token = token;
		getUser = true;
	}
	
	public void setBcast(Integer bcastId, Handler handler,
			CallbackWrapper wrapper) {
		debug.logV(TAG, "setBcast called");
		this.getBcastHandler = handler;
		this.getBcastWrapper = wrapper;
		this.bcastId = bcastId;
		getBcast = true;
	}

	public void setIsLive(String username, Handler handler,
			CallbackWrapper wrapper) {
		debug.logV(TAG, "setIsLive called");
		this.isLiveHandler = handler;
		this.isLiveWrapper = wrapper;
		this.username = username;
		getIsLive = true;
	}

	public void setPlayAircast(Integer bcastId) {
		debug.logV(TAG, "setPlayAircast called");
		this.bcastId = bcastId;
		playAircast = true;
	}

	public void setPostComment(User user, String comment, Integer bcastId) {
		debug.logV(TAG, "setPostComment called");
		this.user = user;
		this.comment = comment;
		this.bcastId = bcastId;
		postComment = true;
	}

	public void setGetComments(Integer bcastId, Handler handler,
			CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetComments called");
		this.getCommentsHandler = handler;
		this.getCommentsWrapper = wrapper;
		this.bcastId = bcastId;
		getComments = true;
	}

	public void setGetListeners(Integer bcastId, Handler handler,
			CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetListeners called");
		this.getListenersHandler = handler;
		this.getListenersWrapper = wrapper;
		this.bcastId = bcastId;
		getListeners = true;
	}

	public void setRequestKey(String token, String title, boolean shareTW,
			boolean shareFB, Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setRequestKey called");
		this.requestKeyHandler = handler;
		this.requestKeyWrapper = wrapper;
		this.token = token;
		this.title = title;
		this.shareFB = shareFB;
		this.shareTW = shareTW;
		isRequestKey = true;
	}

	public void setGetTimelineAll(String token, Integer from, Integer to, Integer limit, 
			Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetTimelineAll called");
		this.getTimelineAllHandler = handler;
		this.getTimelineAllWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		getTimelineAll = true;
	}

	public void setGetTimelineProfile(String token, Integer from, Integer to, Integer limit, 
			Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetTimelineProfile called");
		this.getTimelineProfileHandler = handler;
		this.getTimelineProfileWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		getTimelineProfile = true;
	}

	public void setGetTimelineUser(String token, String profile_user, Integer from, Integer to, Integer limit, 
			Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetTimelineProfile called");
		this.getTimelineUserHandler = handler;
		this.getTimelineUserWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		this.profile_user = profile_user;
		getTimelineUser = true;
	}

	public void setGetTimelineHottest(String token, Integer from, Integer to, Integer limit, 
			Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetTimelineHottest called, from " + from + " to " + to);
		this.getTimelineHottestHandler = handler;
		this.getTimelineHottestWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		getTimelineHottest = true;
	}

	public void setGetTimelineFriends(String token,Integer from, Integer to, Integer limit,
			 Handler handler, CallbackWrapper wrapper) {
		debug.logV(TAG, "setGetTimelineFriends called");
		this.getTimelineFriendsHandler = handler;
		this.getTimelineFriendsWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		getTimelineFriends = true;
	}
	
	public void doSearch( String token, Integer from, Integer to, Integer limit, String search, Handler handler, CallbackWrapper wrapper ) {
		debug.logV(TAG, "doSearch called");
		this.doSearchHandler = handler;
		this.doSearchWrapper = wrapper;
		this.token = token;
		this.from = from;
		this.to = to;
		this.limit = limit;
		this.search = search;
		doSearch = true;		
	}

	public void doDeleteBroadcast(Integer bcast_id, String token, Handler handler, CallbackWrapper wrapper ) {
		debug.logV(TAG, "doDeleteBroadcast called");
		this.doDeleteBroadcastHandler = handler;
		this.doDeleteBroadcastWrapper = wrapper;
		this.bcastId = bcast_id;
		this.token = token;
		doDeleteBroadcast = true;
	}
	public void run() {
		debug.logV(TAG, "run()");
		try {
			// synchronized (fi) {
			if (setFollow) {
				debug.logV(TAG, "run, setFollow");
				boolean ret = getClient().setFollow(username, token);
				setFollow = false;
				setFollowWrapper.setOnFollowResponse(ret);
				setFollowHandler.post(setFollowWrapper);
			}
			if ( setUnfollow ) {
				debug.logV(TAG, "run, setUnfollow");
				boolean ret = getClient().setUnfollow(username, token);
				setUnfollow = false;
				setUnfollowWrapper.setOnUnfollowResponse(ret);
				setUnfollowHandler.post(setUnfollowWrapper);
			}
			if (getUser) {
				debug.logV(TAG, "run, getUser");
				FlipUser user = getClient().getUser(username, token);
				getUser = false;
				getUserWrapper.setOnUserResponse(user);
				getUserHandler.post(getUserWrapper);
			}
			if (getBcast) {
				debug.logV(TAG, "run, getBcast");
				BroadcastDataSet bcast = getClient().getBroadcast(bcastId);
				getBcast = false;
				getBcastWrapper.setResponse(bcast);
				getBcastHandler.post(getBcastWrapper);
			}
			if (getIsLive) {
				debug.logV(TAG, "run, getIsLive");
				boolean isLive = getClient().isLive(username);
				getIsLive = false;
				isLiveWrapper.setOnLiveResponse(isLive);
				isLiveHandler.post(isLiveWrapper);
			}
			if (playAircast) {
				debug.logV(TAG, "run, playAircast");
				getClient().playAircast(bcastId.toString());
				playAircast = false;
			}
			if (postComment) {
				debug.logV(TAG, "run, postComments");
				getClient().postComment(user, comment, bcastId);
				postComment = false;
			}
			if (getComments) {
				debug.logV(TAG, "run, getComments");
				final Hashtable<String, String>[] comments = getClient()
						.getComments(bcastId);
				getCommentsWrapper.setOnCommentsResponse(comments);
				getCommentsHandler.post(getCommentsWrapper);
				getComments = false;
			}
			if (isRequestKey) {
				debug.logV(TAG, "run, requestKey");
				String key = getClient().requestKey(this.token, this.title,
						this.shareTW, this.shareFB);
				requestKeyWrapper.setOnRequestKeyResponse(key);
				requestKeyHandler.post(requestKeyWrapper);
				isRequestKey = false;
			}
			if (getListeners) {
				debug.logV(TAG, "run, getListeners");
				Integer listeners = getClient().getListeners(this.bcastId);
				getListenersWrapper.setOnListenersResponse(listeners);
				getListenersHandler.post(getListenersWrapper);
				getListeners = false;
			}
			if (getTimelineAll) {
				debug.logV(TAG, "run, getTimelineAll");
				List<BroadcastDataSet> bcastList = getClient().getTimelineAll(
						this.token, from, to, limit);
				getTimelineAllWrapper.setOnListingResponse(bcastList, getClient().ALL);
				getTimelineAllHandler.post(getTimelineAllWrapper);
				getTimelineAll = false;
			}
			if (getTimelineFriends) {
				debug.logV(TAG, "run, getTimelineFriends");
				List<BroadcastDataSet> bcastList = getClient()
						.getTimelineFriends(this.token, from, to, limit);
				getTimelineFriendsWrapper.setOnListingResponse(bcastList, getClient().FRIENDS);
				getTimelineFriendsHandler.post(getTimelineFriendsWrapper);
				getTimelineFriends = false;
			}
			if (getTimelineProfile) {
				debug.logV(TAG, "run, getTimelineProfile");
				List<BroadcastDataSet> bcastList = getClient()
						.getTimelineProfile(this.token, from, to, limit);
				getTimelineProfileWrapper.setOnListingResponse(bcastList, getClient().PROFILE);
				getTimelineProfileHandler.post(getTimelineProfileWrapper);
				getTimelineProfile = false;
			}
			if (getTimelineUser) {
				debug.logV(TAG, "run, getTimelineUser");
				List<BroadcastDataSet> bcastList = getClient()
						.getTimelineUser(this.token, profile_user,from, to, limit);
				getTimelineUserWrapper.setOnListingResponse(bcastList, getClient().PROFILE);
				getTimelineUserHandler.post(getTimelineUserWrapper);
				getTimelineUser = false;
			}
			if (getTimelineHottest) {
				debug.logV(TAG, "run, getTimelineHottest, from " + from + " to " + to);
				List<BroadcastDataSet> bcastList = getClient().getTimelineHottest(this.token, from, to, limit);
				getTimelineHottestWrapper.setOnListingResponse(bcastList, getClient().HOTTEST);
				getTimelineHottestHandler.post(getTimelineHottestWrapper);
				getTimelineHottest = false;
			}
			if ( doSearch ) {
				debug.logV(TAG, "run, doSearch");
				List<BroadcastDataSet> bcastList = getClient().getTimelineSearch(this.token, from, to, limit, search);
				doSearchWrapper.setOnListingResponse(bcastList, getClient().SEARCH);
				doSearchHandler.post(doSearchWrapper);
				doSearch = false;
			}
			if ( doDeleteBroadcast ) {
				debug.logV(TAG, "run, doDeleteBroadcast");
				boolean ret = getClient().deleteAircast(bcastId, token);
				doDeleteBroadcastWrapper.setOnBroadcastDeletedResponse(ret);
				doDeleteBroadcastHandler.post(doDeleteBroadcastWrapper);
				doDeleteBroadcast = false;
			}
			// }
		} catch (IOException e) {
			debug.logE(TAG, "run() ERROR", e.getCause());
		}
	}

	private FlipInterface getClient() {
		return fi;
	}

}
