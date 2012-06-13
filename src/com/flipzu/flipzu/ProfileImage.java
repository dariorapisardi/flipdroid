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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class ProfileImage extends Activity {

	/* debugging tag */
	private static final String TAG = "Profile";
	private Debug debug = new Debug();

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.profile_url);
		ImageView iv = (ImageView) findViewById(R.id.profile_image);
		UrlImageViewHelper.setUrlDrawable(iv, getIntent().getStringExtra("imageUrl").replace("_reasonably_small", ""), R.drawable.icon_sq);
		debug.logV(TAG, "URL --> " + getIntent().getStringExtra("imageUrl").replace("_reasonably_small", ""));
		findViewById(R.id.profile_image).setOnClickListener(new OnClickListener() {
			@Override public void onClick(View arg0) {ProfileImage.this.finish();}
		});
	}
}
