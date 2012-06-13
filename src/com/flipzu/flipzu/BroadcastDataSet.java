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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BroadcastDataSet {
	private String started_ts;
	private String id;
	private String text;
	private String username;
	private String img_url;
	private String audio_url;
	private String audio_url_fallback;
	private String liveaudio_url;
	private boolean is_live;
	private String listens;
	private String time_str;
	private String full_url;
	
	private boolean authorized = true;
	
	public String getStarted_ts() {
		return started_ts;
	}
	public void setStarted_ts(String startedTs) {
		started_ts = startedTs;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getImgUrl() {
		return img_url;
	}
	public void setImgUrl(String imgUrl) {
		img_url = imgUrl;
	}
	public String getAudioUrl() {
		return audio_url;
	}
	public void setAudioUrl(String audioUrl) {
		audio_url = audioUrl;
	}
	public String getAudioUrlFallback() {
		return audio_url_fallback;
	}
	public void setAudioUrlFallback(String audioUrl) {
		audio_url_fallback = audioUrl;
	}
	public String getLiveaudioUrl() {
		return liveaudio_url;
	}
	public void setLiveaudioUrl(String liveaudioUrl) {
		liveaudio_url = liveaudioUrl;
	}
	public boolean isLive() {
		return is_live;
	}
	public void setLive(boolean isLive) {
		is_live = isLive;
	}
	public String getListens() {
		return listens;
	}
	public void setListens(String listens) {
		this.listens = listens;
	}
	public String getTimeStr() {
		
		if ( started_ts == null ) 
			return null;
		
		try {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date time_ts = formatter.parse(started_ts);
			PrettyDate pd = new PrettyDate(time_ts);
			return pd.toString();
		} catch ( ParseException e) {
			return time_str;
		}		
	}
	public void setTimeStr(String timeStr) {
		time_str = timeStr;
	}
	public String getFullUrl() {
		return full_url;
	}
	public void setFullUrl( String url ) {
		full_url = url;
	}
	public boolean isAuthorized() {
		return authorized;
	}
	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}	
}
