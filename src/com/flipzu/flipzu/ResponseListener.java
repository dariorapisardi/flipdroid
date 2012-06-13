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

public interface ResponseListener {
	public void onResponseReceived( BroadcastDataSet bcast );
	public void onIsLiveReceived( boolean isLive );
	public void onCommentsReceived( Hashtable<String,String>[] comments );
	public void onRequestKeyReceived ( String key );
	public void onListenersReceived ( Integer listeners );
	public void onListingReceived ( List<BroadcastDataSet> bcastList, Integer list_type );
	public void onUserReceived ( FlipUser user );
	public void onFollowReceived ( boolean ret );
	public void onUnfollowReceived ( boolean ret );
	public void onBroadcastDeletedReceived ( boolean ret );
}
