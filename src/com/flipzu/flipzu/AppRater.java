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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class AppRater {
    private final static String APP_TITLE = "Flipzu";
    private final static String APP_PNAME = "com.flipzu.flipzu";
    
    private final static int LAUNCHES_UNTIL_PROMPT = 3;

    /* application settings */
	public static final String PREFS_NAME = "FlipzuPrefsFile";

    public static void app_launched(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        if (prefs.getBoolean("dontshowagain", false)) { return ; }
        
        SharedPreferences.Editor editor = prefs.edit();
        
        long launch_count = prefs.getLong("launch_count", 0);

        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
                showRateDialog(mContext, editor);
        }
    }
    
    public static void incrementLaunches(Context mContext){
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        
        SharedPreferences.Editor editor = prefs.edit();
        
        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);
        editor.commit();
    }
    
    public static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	builder.setTitle(mContext.getString(R.string.rate_title, APP_TITLE));
    	builder.setIcon(R.drawable.icon);
    	builder.setMessage(mContext.getString(R.string.rate_message, APP_TITLE))
    	       .setCancelable(false)
    	       .setPositiveButton(mContext.getString(R.string.rate_title, APP_TITLE), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
	                    editor.putBoolean("dontshowagain", true);
	                    editor.commit();
    	                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
    	                dialog.dismiss();
    	           }
    	       })
    	       .setNeutralButton(R.string.remind, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
		                dialog.dismiss();
		                editor.putLong("launch_count", 0);
		                editor.commit();
					}
				})
    	       .setNegativeButton(R.string.nothanks, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                if (editor != null) {
    	                    editor.putBoolean("dontshowagain", true);
    	                    editor.commit();
    	                }
    	                dialog.dismiss();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();        
    }
}
