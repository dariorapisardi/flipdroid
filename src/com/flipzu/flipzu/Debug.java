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

import android.util.Log;

public class Debug {
	public static boolean DEBUG = false;
	
	public void logD ( String tag, String msg ) 
	{
		if ( DEBUG ) {
			Log.d(tag, msg);	
		}
	}
	public void logE ( String tag, String msg, Throwable e )
	{
		if ( DEBUG ) {
			if ( e != null ) {
				Log.e(tag, msg, e);
				e.printStackTrace();				
			} else {
				Log.e(tag, msg);
			}

		}
	}
	public void logV ( String tag, String msg )
	{
		if ( DEBUG ) {
			Log.v(tag, msg);
		}
	}
	public void logW ( String tag, String msg )
	{
		if ( DEBUG ) {
			Log.w(tag, msg);
		}
	}
}
