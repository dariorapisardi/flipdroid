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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UserHandler extends DefaultHandler {
	
	private Debug debug = new Debug();
	private String TAG = "UserHandler";
    // ===========================================================
    // Fields
    // ===========================================================

	private String status_tagline = "status";
    private String outer_tagline = "user";
    
    private boolean in_status = false;
    private boolean in_username = false;
    private boolean in_fullname = false;
    private boolean in_avatar = false;
    private boolean in_following = false;
    
    private FlipUser dataSet = new FlipUser();

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public FlipUser getParsedData() {
        return dataSet;
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
        
        debug.logV(TAG, "startElement, got " + localName);
      
        if (localName.equals(this.status_tagline)) {
        	this.in_status = true;
        }
        else if (localName.equals(this.outer_tagline)) {
        	dataSet = new FlipUser();
        }
        else if (localName.equals("username")) {
            this.in_username = true;
        }
        else if (localName.equals("fullname")) {
            this.in_fullname = true;
        }
        else if (localName.equals("avatar")) {
            this.in_avatar = true;
        }
        else if (localName.equals("following")) {
            this.in_following= true;
        }
    }


    /** Gets be called on closing tags like: 
     * </tag> */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
    	
    	debug.logV(TAG, "endElement, got " + localName);
    	
    	if (localName.equals(this.status_tagline)) {
    		this.in_status = false;
        }
    	else if (localName.equals("username")) {
            this.in_username = false;
        }
        else if (localName.equals("fullname")) {
            this.in_fullname = false;
        }
        else if (localName.equals("avatar")) {
            this.in_avatar = false;
        }
        else if (localName.equals("following")) {
            this.in_following = false;
        }
    }       


    /** Gets be called on the following structure: 
     * <tag>characters</tag> */
    @Override
    public void characters(char ch[], int start, int length) {
    	
    	String data = new String(ch, start, length);
    	
    	debug.logV(TAG, "characters, got " + data);
    	
    	if(this.in_status) {
    		String status = data;
    		if ( status.equals("NOK") ) {
    			dataSet.setAuthorized(false);
    		} else {
    			dataSet.setAuthorized(true);
    		}
    	}
        if(this.in_username){
        	dataSet.setUsername(data);
        }
        if(this.in_fullname) {
        	dataSet.setFullname(data);
        }
        if(this.in_avatar) { 
        	dataSet.setAvatarUrl(data);
        }
        if(this.in_following) {
        	String following = data;
        	if ( following.equals("1")) {
        		dataSet.setFollowing(true);
        	} else {
        		dataSet.setFollowing(false);
        	}
        }
    }
}
