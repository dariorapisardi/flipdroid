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

import java.util.Date;
import java.util.TimeZone;

public class PrettyDate {
	private Date date;

	public PrettyDate() {
		this(new Date());
	}

	public PrettyDate(Date date) {
		this.date = date;
	}

	public String toString() {
		int tz_offset = TimeZone.getDefault().getRawOffset();
		// server is hardcoded in New York...
		int server_offset = TimeZone.getTimeZone("America/New_York").getRawOffset();
		int offset = server_offset - tz_offset;
		long	current = (new Date()).getTime(),
			timestamp = date.getTime(),
			diff = (current + offset - timestamp)/1000;
		
		int	amount = 0;
		String	what = "";

		/**
		 * Second counts
		 * 3600: hour
		 * 86400: day
		 * 604800: week
		 * 2592000: month
		 * 31536000: year
		 */

		if(diff > 31536000) {
			amount = (int)(diff/31536000);
			what = "year";
		}
		else if(diff > 31536000) {
			amount = (int)(diff/31536000);
			what = "month";
		}
		else if(diff > 604800) {
			amount = (int)(diff/604800);
			what = "week";
		}
		else if(diff > 86400) {
			amount = (int)(diff/86400);
			what = "day";
		}
		else if(diff > 3600) {
			amount = (int)(diff/3600);
			what = "hour";
		}
		else if(diff > 60) {
			amount = (int)(diff/60);
			what = "minute";
		}
		else {
			amount = (int)diff;
			what = "second";
			if(amount < 6) {
				return "Just now";
			}
		}

		if(amount == 1) {
			if(what.equals("day")) {
				return "Yesterday";
			}
			else if(what.equals("week") || what.equals("month") || what.equals("year")) {
				return "Last " + what;
			}
		}
		else {
			what += "s";
		}

		return amount + " " + what + " ago";
	}
}
