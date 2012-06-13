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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class SearchFragment extends Fragment {
	
	/* debugging tag */
	private static final String TAG = "SearchFragment";
	private Debug debug = new Debug();
	
	private String mSearch;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {    	    	
        super.onCreate(savedInstanceState);
        
        debug.logV(TAG, "onCreate()");
    }    
    
	@Override
	public void onStop() {
		super.onStop();
		
		debug.logV(TAG, "onStop()");		
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		debug.logV(TAG, "onCreateView()");
		
		getSupportFragmentManager().beginTransaction().replace(R.id.content, new ListingsFragmentSearch()).commit();				
		
	    return inflater.inflate(R.layout.search, container, false);
	}

	@Override
	public void onResume() {	
		super.onResume();
		
		debug.logV(TAG, "onResume()");
		
		final EditText search_et = (EditText) getActivity().findViewById(R.id.search);		
		search_et.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					mSearch = search_et.getText().toString().trim();
					debug.logV(TAG, "onKey, got " + mSearch);
					EventNotifier.getInstance().signalNewSearch(mSearch);
					return true;
				}
				return false;
			}
		});
		
		search_et.setText("");
//		mSearch = search_et.getText().toString();
//		
//		if ( mSearch != null ) {
//			debug.logV(TAG, "searching for " + mSearch);
//			EventNotifier.getInstance().signalNewSearch(mSearch);			
//		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		debug.logV(TAG, "onViewCreated()");
		
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		debug.logV(TAG, "onDestroyView()");
	}

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);
		
		debug.logV(TAG, "onAttach()");
	}
		
}
