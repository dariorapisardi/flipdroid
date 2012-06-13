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

import android.os.Handler;

public class AsyncFlipInterface {
	private static final String TAG = "AsyncFlipInterface";
	private static Debug debug = new Debug();
	
	public static void setFollow( String username, String token, ResponseListener callback ) {
		debug.logV(TAG, "setFollow called");
		AsynchronousSender as = new AsynchronousSender();
		as.setFollow(username, token, new Handler(),new CallbackWrapper(callback));
		as.start();		
	}
	
	public static void setUnfollow( String username, String token, ResponseListener callback ) {
		debug.logV(TAG, "setUnfollow called");
		AsynchronousSender as = new AsynchronousSender();
		as.setUnfollow(username, token, new Handler(),new CallbackWrapper(callback));
		as.start();		
	}

	public static void getUser( String username, String token, ResponseListener callback ) {
		debug.logV(TAG,"getUser called");
		AsynchronousSender as = new AsynchronousSender();
		as.setUser(username, token, new Handler(),new CallbackWrapper(callback));
		as.start();		
	}
	public static void getBroadcast( Integer bcastId, ResponseListener callback ) {
		debug.logV(TAG,"getBroadcast called");
		AsynchronousSender as = new AsynchronousSender();
		as.setBcast(bcastId, new Handler(),new CallbackWrapper(callback));
		as.start();		
	}
	public static void isLive( String username, ResponseListener callback ) {
		debug.logV(TAG, "isLive called");
		AsynchronousSender as = new AsynchronousSender();
		as.setIsLive(username, new Handler(),new CallbackWrapper(callback));
		as.start();
	}
	public static void postComment( User user, String comment, Integer bcastId) {
		debug.logV(TAG, "postComment called");
		AsynchronousSender as = new AsynchronousSender();
		as.setPostComment(user, comment, bcastId);
		as.start();
	}
	public static void playAircast( Integer bcastId ) {
		debug.logV(TAG, "playAircast called");
		AsynchronousSender as = new AsynchronousSender();
		as.setPlayAircast(bcastId);
		as.start();
	}
	public static void getComments( Integer bcastId , ResponseListener callback ) {
		debug.logV(TAG, "getComments called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetComments(bcastId, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void requestKey( String token, String title, boolean shareTW, boolean shareFB, ResponseListener callback ) {
		debug.logV(TAG, "requestKey called");
		AsynchronousSender as = new AsynchronousSender();
		as.setRequestKey(token, title, shareTW, shareFB, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getListeners( Integer bcastId , ResponseListener callback ) {
		debug.logV(TAG, "getListeners called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetListeners(bcastId, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getTimelineFriends( String token, Integer from, Integer to, Integer limit, ResponseListener callback ) {
		debug.logV(TAG, "getTimelineFriends called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetTimelineFriends(token, from, to, limit, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getTimelineAll( String token, Integer from, Integer to, Integer limit, ResponseListener callback ) {
		debug.logV(TAG, "getTimelineAll called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetTimelineAll(token, from, to, limit, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getTimelineHottest( String token, Integer from, Integer to, Integer limit, ResponseListener callback ) {
		debug.logV(TAG, "getTimelineHottest called, from " + from + " to " + to);
		AsynchronousSender as = new AsynchronousSender();
		as.setGetTimelineHottest(token, from, to, limit, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getTimelineProfile( String token, Integer from, Integer to, Integer limit, ResponseListener callback ) {
		debug.logV(TAG, "getTimelineProfile called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetTimelineProfile(token, from, to, limit, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void getTimelineUser( String token, FlipUser profileUser, Integer from, Integer to, Integer limit, ResponseListener callback ) {
		debug.logV(TAG, "getTimelineUser called");
		AsynchronousSender as = new AsynchronousSender();
		as.setGetTimelineUser(token, profileUser.getUsername(), from, to, limit, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void doSearch( String token, Integer from, Integer to, Integer limit, String search, ResponseListener callback ) {
		debug.logV(TAG, "doSearch called");
		AsynchronousSender as = new AsynchronousSender();
		as.doSearch(token, from, to, limit, search, new Handler(), new CallbackWrapper(callback));
		as.start();
	}
	public static void doDeleteBroadcast( Integer bcast_id, String token, ResponseListener callback ) {
		debug.logV(TAG, "doDeleteBroadcast called");
		AsynchronousSender as = new AsynchronousSender();
		as.doDeleteBroadcast(bcast_id, token, new Handler(), new CallbackWrapper(callback));
		as.start();
	}

}
