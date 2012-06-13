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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TimelineHandler extends DefaultHandler {
	
	private Debug debug = new Debug();
	private String TAG = "TimelineHandler";
    // ===========================================================
    // Fields
    // ===========================================================

	private String status_tagline = "status";
    private String outer_tagline = "broadcast";
    private boolean in_status = false;
    private boolean in_started_ts = false;
    private boolean in_id = false;
    private boolean in_text = false;
    private boolean in_username = false;
    private boolean in_img_url = false;
    private boolean in_audio_url = false;
    private boolean in_audio_url_fallback = false;
    private boolean in_liveaudio_url = false;
    private boolean in_is_live = false;
    private boolean in_listens = false;
    private boolean in_time_str = false;
    private boolean in_full_url = false;
    
    private String text = "";
    private String username = "";
    private String img_url = "";
    private String audio_url = "";
    private String audio_url_fallback = "";
    private String liveaudio_url = "";
    private String full_url = "";

    private BroadcastDataSet bcastDataSet = new BroadcastDataSet();    
    private List<BroadcastDataSet> bcastList = new ArrayList<BroadcastDataSet>();

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public List<BroadcastDataSet> getParsedData() {
        return this.bcastList;
    }

    // ===========================================================
    // Methods
    // ===========================================================
    @Override
    public void startDocument() throws SAXException {
    	debug.logV(TAG,"startDocument()");
    }

    @Override
    public void endDocument() throws SAXException {
    	debug.logV(TAG, "endDocument()");
    }

    /** Gets be called on opening tags like: 
     * <tag> 
     * Can provide attribute(s), when xml was like:
     * <tag attribute="attributeValue">*/
    @Override
    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);
      
        if (localName.equals(this.status_tagline)) {
        	this.in_status = true;
        }
        else if (localName.equals(this.outer_tagline)) {
        	bcastDataSet = new BroadcastDataSet();
        }
        else if (localName.equals("started_ts")) {
            this.in_started_ts = true;
        }
        else if (localName.equals("id")) {
            this.in_id = true;
        }
        else if (localName.equals("text")) {
        	debug.logV(TAG, "startElement text");
            this.in_text = true;
        }
        else if (localName.equals("username")) {
            this.in_username= true;
        }
        else if (localName.equals("img_url")) {
            this.in_img_url = true;
        }
        else if (localName.equals("audio_url")) {
            this.in_audio_url = true;
        }
        else if (localName.equals("audio_url_fallback")) {
            this.in_audio_url_fallback = true;
        }
        else if (localName.equals("liveaudio_url")) {
            this.in_liveaudio_url = true;
        }
        else if (localName.equals("is_live")) {
            this.in_is_live = true;
        }
        else if (localName.equals("listens")) {
            this.in_listens = true;
        }
        else if (localName.equals("time_str")) {
            this.in_time_str = true;
        }
        else if (localName.equals("full_url")) {
            this.in_full_url = true;
        }
    }


    /** Gets be called on closing tags like: 
     * </tag> */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
    	
    	if (localName.equals(this.status_tagline)) {
    		if ( !bcastDataSet.isAuthorized() ) {
    			bcastList.add(bcastDataSet);
    		}
    		this.in_status = false;
        }
    	else if (localName.equals(this.outer_tagline)) {
    		if ( bcastList != null )
    			bcastList.add(bcastDataSet);
        	bcastDataSet = null;
        }
    	else if (localName.equals("started_ts")) {
            this.in_started_ts = false;
        }
        else if (localName.equals("id")) {
            this.in_id = false;
        }
        else if (localName.equals("text")) {
        	debug.logV(TAG, "endElement text");
        	text = "";
            this.in_text = false;
        }
        else if (localName.equals("username")) {
        	username = "";
            this.in_username = false;
        }
        else if (localName.equals("img_url")) {
        	img_url = "";
            this.in_img_url = false;
        }
        else if (localName.equals("audio_url")) {
        	audio_url = "";
            this.in_audio_url = false;
        }
        else if (localName.equals("audio_url_fallback")) {
        	audio_url_fallback = "";
            this.in_audio_url_fallback = false;
        }
        else if (localName.equals("liveaudio_url")) {
        	liveaudio_url = "";
            this.in_liveaudio_url = false;
        }
        else if (localName.equals("is_live")) {
            this.in_is_live = false;
        }
        else if (localName.equals("listens")) {
            this.in_listens = false;
        }
        else if (localName.equals("time_str")) {
            this.in_time_str = false;
        }
        else if (localName.equals("full_url")) {
        	full_url = "";
            this.in_full_url = false;
        }
    }       


    /** Gets be called on the following structure: 
     * <tag>characters</tag> */
    @Override
    public void characters(char ch[], int start, int length) {
    	if(this.in_status) {
    		String status = new String(ch, start, length);
    		if ( status.equals("NOK") ) {
    			bcastDataSet.setAuthorized(false);
    		} else {
    			bcastDataSet.setAuthorized(true);
    		}
    	}
        if(this.in_started_ts){
        	bcastDataSet.setStarted_ts(new String(ch, start, length));
        }
        if(this.in_id) {
        	bcastDataSet.setId(new String(ch, start, length));
        }
        if(this.in_text) {
        	String aux_text = new String(ch, start, length);
        	text += aux_text;
        	bcastDataSet.setText(text);
        	debug.logV(TAG,"characters, setText " + text);
        }
        if(this.in_username) {
        	String aux_username = new String(ch, start, length);
        	username += aux_username;
        	bcastDataSet.setUsername(username);
        }
        if(this.in_img_url) {
        	String aux_img_url = new String(ch, start, length);
        	img_url += aux_img_url;
        	bcastDataSet.setImgUrl(img_url);
        }
        if(this.in_audio_url) {
        	String aux_audio_url = new String(ch, start, length);
        	audio_url += aux_audio_url;
        	bcastDataSet.setAudioUrl(audio_url);
        }
        if(this.in_audio_url_fallback) {
        	String aux_audio_url_fallback = new String(ch, start, length);
        	audio_url_fallback += aux_audio_url_fallback;
        	bcastDataSet.setAudioUrlFallback(audio_url_fallback);
        }
        if(this.in_liveaudio_url) {
        	String aux_liveaudio_url = new String(ch, start, length);
        	liveaudio_url = aux_liveaudio_url;
        	bcastDataSet.setLiveaudioUrl(liveaudio_url);
        }
        if(this.in_is_live) {
        	String is_live = new String(ch, start, length);
        	if ( is_live.equalsIgnoreCase("true") ) {
        		bcastDataSet.setLive(true);
        	} else {
        		bcastDataSet.setLive(false);
        	}	
        }
        if(this.in_listens) {
        	bcastDataSet.setListens(new String(ch, start, length));
        }
        if(this.in_time_str) {
        	bcastDataSet.setTimeStr(new String(ch, start, length));
        }
        if(this.in_full_url) {
        	String aux_full_url = new String(ch, start, length);
        	full_url += aux_full_url;
        	bcastDataSet.setFullUrl(full_url);
        }
    }
}
