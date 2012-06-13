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

public class User {
    private String username;
    private String token;
    private boolean has_twitter = false;
    private boolean has_facebook = false;
    private boolean is_premium = false;

    public User(String username, String token) {
            this.username = username;
            this.token = token;
    }
    public User() {
            // TODO Auto-generated constructor stub
    }
    public String getUsername() {
            return username;
    }
    public void setUsername(String username) {
            this.username = username;
    }
    public String getToken() {
            return token;
    }
    public void setToken(String token) {
            this.token = token;
    }
    public void setTwitter(boolean has_twitter) {
    	this.has_twitter = has_twitter;
    }
    public void setFacebook(boolean has_facebook) {
    	this.has_facebook = has_facebook;
    }
    public boolean hasTwitter() {
    	return this.has_twitter;
    }
    public boolean hasFacebook() {
    	return this.has_facebook;
    }
    public boolean isPremium() {
    	return is_premium;
    }
    public void setPremium( boolean premium ) {
    	this.is_premium = premium;
    }
}
