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
import java.util.Timer;
import java.util.TimerTask;


public class Stats {
	private static final String TAG = "Stats";
	private Debug debug = new Debug();
	private static Integer listeners = 0;
	private static Hashtable<String,String>[] comments = null;
	private int bcast_id = -1;
	User user = null;

	Timer timer = null;
	
	class mStatsTask extends TimerTask 
	{
		public void run() {
			debug.logD(TAG, "mStatsTask() called");
			
			final FlipInterface fi = new FlipInterface();
			
			listeners = fi.getListeners(bcast_id);
			
			debug.logD(TAG, "mStatsTask() called. Got listeners " + listeners.toString());
			
			try {
				comments = fi.getComments(user);	
			} catch (IOException e ) {
				e.printStackTrace();
			}
			
			if ( comments != null ) {
				debug.logD(TAG,"mStatsTask comments len " + comments.length);				
			} else {
				debug.logD(TAG,"mStatsTask no comments");
			}

			
			/*for(int i=0; i<comments.length; i++) {
				debug.logD(TAG,"mStatsTask comment " + comments[i]);				
			}*/
		}
	}
	
	public Integer getListeners()
	{
		return listeners;
	}
	
	public Hashtable<String,String>[] getComments()
	{
		return comments;
	}
	
	public void startStats(int broadcast_id, User flipzuser)
	{
		bcast_id = broadcast_id;
		user = flipzuser;
		
		if ( timer == null ) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new mStatsTask(), 5 * 1000, 5 * 1000);
		}
	}
	
	public void stopStats()
	{
		timer.cancel();
		timer.purge();
		timer = null;
	}
	
	public void setBcastID(int broadcast_id)
	{
		bcast_id = broadcast_id;
	}
	
	public void setUser(User flipzuser) 
	{
		user = flipzuser;
	}
}
