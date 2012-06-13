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


public class CallbackWrapper implements Runnable  {
	private ResponseListener callbackActivity;
	private BroadcastDataSet bcast;
	private FlipUser flip_user;
	private Hashtable<String,String>[] comments;
	private boolean ret;
	
	private boolean isLive;
	private boolean runSetFollow = false;
	private boolean runSetUnfollow = false;
	private boolean runGetUser = false;
	private boolean runBcast = false;
	private boolean runIsLive = false;
	private boolean runGetComments = false;
	private boolean runRequestKey = false;
	private boolean runGetListeners = false;
	private boolean runGetListing = false;
	private boolean runDeleteBroadcast = false;
	private String key;
	private Integer listeners;
	private List<BroadcastDataSet> bcastList;
	private Integer list_type;
 
	public CallbackWrapper(ResponseListener callbackActivity) {
		this.callbackActivity = callbackActivity;
	}
 
	public void run() {
		if ( runSetFollow ) {
			callbackActivity.onFollowReceived(ret);
		}
		if ( runSetUnfollow ) {
			callbackActivity.onUnfollowReceived(ret);
		}
		if ( runGetUser ) {
			callbackActivity.onUserReceived(flip_user);
		}
		if ( runBcast ) {
			callbackActivity.onResponseReceived(bcast);
			runBcast = false;
		}
		if ( runIsLive ) {
			callbackActivity.onIsLiveReceived(isLive);	
			runIsLive = false;
		}
		if ( runGetComments ) {
			callbackActivity.onCommentsReceived(comments);
			runGetComments = false;
		}
		if ( runRequestKey ) {
			callbackActivity.onRequestKeyReceived(key);
			runRequestKey = false;
		}
		if ( runGetListeners ) {
			callbackActivity.onListenersReceived(listeners);
			runGetListeners = false;
		}
		if ( runGetListing ) {
			callbackActivity.onListingReceived(bcastList, list_type);
			runGetListing = false;
		}
		if ( runDeleteBroadcast ) {
			callbackActivity.onBroadcastDeletedReceived(ret);
			runDeleteBroadcast = false;
		}
	}
 
	public void setResponse(BroadcastDataSet bcast) {
		this.bcast = bcast;
		runBcast = true;
	}
	public void setOnLiveResponse(boolean isLive) {
		this.isLive = isLive;
		runIsLive = true;
	}
	public void setOnCommentsResponse( Hashtable<String,String>[] comments ) {
		this.comments = comments;
		runGetComments = true;
	}
	public void setOnRequestKeyResponse( String key ) {
		this.key = key;
		runRequestKey = true;
	}
	public void setOnListenersResponse( Integer listeners ) {
		this.listeners = listeners;
		runGetListeners = true;
	}
	public void setOnListingResponse ( List<BroadcastDataSet> bcastList, Integer list_type ) {
		this.bcastList = bcastList;
		this.list_type = list_type;
		runGetListing = true;
	}
	public void setOnUserResponse ( FlipUser user ) {
		this.flip_user = user;
		runGetUser = true;
	}
	public void setOnFollowResponse ( boolean ret ) {
		this.ret = ret;
		runSetFollow = true;
	}
	public void setOnUnfollowResponse ( boolean ret ) {
		this.ret = ret;
		runSetUnfollow = true;
	}
	public void setOnBroadcastDeletedResponse( boolean ret ) {
		this.ret = ret;
		runDeleteBroadcast = true;
	}
}
