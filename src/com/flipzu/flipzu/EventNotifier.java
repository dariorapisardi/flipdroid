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

public class EventNotifier {
	
	private static EventNotifier INSTANCE = new EventNotifier();
	
	private ListingsEvents leAll = null;
	private ListingsEvents leFriends = null;
	private ListingsEvents leHot = null;
	private ListingsEvents leProfile = null;
	private ListingsEvents leUser = null;
	private ListingsEvents leSearch = null;
	
	private EventNotifier() { }	
	
	public static EventNotifier getInstance() {
		return INSTANCE;
	}
	
	public void setEventAll(ListingsEvents event) {
		leAll = event;
	}
	
	public void setEventFriends(ListingsEvents event) {
		leFriends = event;
	}
	
	public void setEventHot(ListingsEvents event) {
		leHot = event;
	}
	
	public void setEventProfile(ListingsEvents event) {
		leProfile = event;
	}
	
	public void setEventUser(ListingsEvents event) {
		leUser = event;
	}
	
	public void setEventSearch(ListingsEvents event) {
		leSearch = event;
	}
	
	public void signalRefreshAll( ) {
		if ( leAll != null ) {
			leAll.refreshEvent();
		}
	}
	
	public void signalRefreshFriends() {
		if ( leFriends != null ) {
			leFriends.refreshEvent();
		}
	}
	
	public void signalRefreshProfile() {
		if ( leProfile != null ) {
			leProfile.refreshEvent();
		}
	}
	
	public void signalRefreshUser() {
		if ( leUser != null ) {
			leUser.refreshEvent();
		}
	}
	
	public void signalRefreshHottest() {
		if ( leHot != null ) {
			leHot.refreshEvent();
		}
	}
	
	public void signalRefreshSearch() {
		if ( leSearch != null ) {
			leSearch.refreshEvent();
		}
	}
	
	public void signalNewSearch(String search) {
		if ( leSearch != null ) {
			leSearch.refreshSearch(search);
		}
	}
	
	public void goToTop() {
//		TBI
	}
}
