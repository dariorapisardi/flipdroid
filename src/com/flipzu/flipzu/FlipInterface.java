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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import android.util.Log;

public class FlipInterface {

	private static final String TAG = "FlipInterface";
	private Debug debug = new Debug();

	private String WSServerSecure = "https://flipzu.com";
	private String WSListen = "http://stats.flipzu.com";
	private String WSServer = "http://flipzu.com";
	private long last_id = 0;

	public final Integer FRIENDS = 0;
	public final Integer ALL = 1;
	public final Integer HOTTEST = 2;
	public final Integer PROFILE = 3;
	public final Integer USER = 4;
	public final Integer SEARCH = 99;

	// always verify the host - dont check for certificate
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	public FlipInterface() {
		super();
	}

	public User requestTokenWithToken(String data) throws InvalidToken {

		String result = null;

		debug.logV(TAG, "requestTokenWithToken() called");

		try {
			result = this.postViaHttpsConnection(
					"/api/request_token_with_token.xml", data);
			Element rootElement = getRootElement(result);

			debug.logV(TAG, "requestTokenWithToken() result: " + result);

			if (getResponse(rootElement).equals("OK")) {
				User u = new User();
				u.setUsername(getNodeValue(rootElement, "username"));
				u.setToken(getNodeValue(rootElement, "token"));
				if (getNodeValue(rootElement, "has_twitter") != null
						&& getNodeValue(rootElement, "has_twitter").equals("1")) {
					u.setTwitter(true);
				}
				if (getNodeValue(rootElement, "has_facebook") != null
						&& getNodeValue(rootElement, "has_facebook")
								.equals("1")) {
					u.setFacebook(true);
				}
				if (getNodeValue(rootElement, "is_premium") != null
						&& getNodeValue(rootElement, "is_premium").equals("1")) {
					u.setPremium(true);
				}
				return u;
			} else {
				return null;
			}

		} catch (Exception e) {
			debug.logE(TAG, "Login Failed:", e.getCause());
		}

		return null;
	}

	public Element getRootElement(String resp)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		resp = this.replaceAll(resp, "\0", "");

		byte[] bytes = resp.getBytes(); // IMPORTANT FOR NULL CHARACTERS

		Document document = null;
		try {
			document = builder.parse(new ByteArrayInputStream(bytes));
		} catch (SAXParseException e) {
			debug.logW(TAG, "getRootElement: " + e.getMessage());
		}

		// Normalize the root element of the XML document. This ensures that all
		// Text
		// nodes under the root node are put into a "normal" form, which means
		// that
		// there are neither adjacent Text nodes nor empty Text nodes in the
		// document.
		// See Node.normalize().

		Element rootElement = null;
		if (document != null) {
			rootElement = document.getDocumentElement();
			rootElement.normalize();
		}

		return rootElement;

	}

	public String getResponse(Element rootElement) {

		if (rootElement == null) {
			debug.logD(TAG, "getResponse(): got NULL rootElement");
			return null;
		}
		debug.logD(TAG, "getResponse(): " + rootElement.getChildNodes());
		return rootElement.getChildNodes().item(1).getAttributes()
				.getNamedItem("status").getNodeValue();

	}

	public String getNodeValue(Element rootElement, String itemname) {
		NodeList i = rootElement.getElementsByTagName(itemname);

		Node r = null;
		if ((r = i.item(0)) != null) {
			debug.logD(TAG, "" + r.getChildNodes().item(0).getNodeValue());
			return r.getChildNodes().item(0).getNodeValue();
		} else {
			return null;
		}
	}

	private String getElementValue(Node node) {
		NodeList children = node.getChildNodes();
		if (children.getLength() > 0) {
			return children.item(0).getNodeValue();
		} else {
			return null;
		}

	}

	public String replaceAll(String text, String searchString,
			String replacementString) {
		StringBuffer sBuffer = new StringBuffer();
		int pos = 0;
		while ((pos = text.indexOf(searchString)) != -1) {
			sBuffer.append(text.substring(0, pos) + replacementString);
			text = text.substring(pos + searchString.length());
		}
		sBuffer.append(text);
		return sBuffer.toString();
	}

	public String requestKey(String token, String title, boolean shareTW,
			boolean shareFB) {
		String sh_tw = "0";
		if (shareTW) {
			sh_tw = "1";
		}
		String sh_fb = "0";
		if (shareFB) {
			sh_fb = "1";
		}
		String data = "access_token=" + token + "&text=" + title + "&tw_share="
				+ sh_tw + "&fb_share=" + sh_fb;

		String result = null;
		debug.logV(TAG, "requestKey(): " + data);

		try {
			result = this.postViaHttpsConnection("/api/request_key.xml", data);
			Element rootElement = getRootElement(result);
			if (getResponse(rootElement).equals("OK")) {
				return getNodeValue(rootElement, "key");
			} else {
				debug.logW(TAG, result);
				return "0";
			}

		} catch (Exception e) {
			debug.logE(TAG, "requestKey error", e.getCause());
			return null;
		}

	}

	/*
	 * POSTs the params, returns a String (response body). Should be
	 * postprocessed and get the XML
	 */
	String postViaHttpsConnection(String path, String params)
			throws IOException {
		HttpsURLConnection c = null;
		InputStream is = null;
		OutputStream os = null;
		String respString = null;
		int rc;

		// String url = WSServerSecure + path;
		URL url = new URL(WSServerSecure + path);

		try {
			trustAllHosts();
			c = (HttpsURLConnection) url.openConnection();
			c.setHostnameVerifier(DO_NOT_VERIFY);

			c.setDoOutput(true);
			// Set the request method and headers
			c.setRequestMethod("POST");
			c.setRequestProperty("User-Agent",
					"Profile/MIDP-2.0 Configuration/CLDC-1.0");
			c.setRequestProperty("Content-Language", "en-US");
			c.setRequestProperty("Accept-Encoding", "identity");

			// Getting the output stream may flush the headers
			os = c.getOutputStream();
			os.write(params.getBytes());
			os.flush();

			// Getting the response code will open the connection,
			// send the request, and read the HTTP response headers.
			// The headers are stored until requested.
			rc = c.getResponseCode();
			if (rc != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP response code: " + rc);
			}

			is = c.getInputStream();

			// Get the length and process the data
			int len = (int) c.getContentLength();
			if (len > 0) {
				int actual = 0;
				int bytesread = 0;
				byte[] data = new byte[len];
				while ((bytesread != len) && (actual != -1)) {
					actual = is.read(data, bytesread, len - bytesread);
					bytesread += actual;
				}
				respString = new String(data);

			} else {
				byte[] data = new byte[8192];
				int ch;
				int i = 0;
				while ((ch = is.read()) != -1) {
					if (i < data.length)
						data[i] = ((byte) ch);
					i++;
				}
				respString = new String(data);
			}

		} catch (ClassCastException e) {
			debug.logW(TAG, "Not an HTTP URL");
			throw new IllegalArgumentException("Not an HTTP URL");
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
			if (c != null)
				c.disconnect();
		}

		return respString;
	}

	/**
	 * Trust every server - dont check for any certificate
	 */
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			Log.e(TAG, "trustAllHosts ERROR", e.getCause());
		}
	}

	public String getViaStreamConnection(String server, String path)
			throws IOException {
		HttpURLConnection c = null;
		InputStream s = null;

		URL url = new URL(server + path);
		String respString = null;

		try {

			c = (HttpURLConnection) url.openConnection();
			c.setRequestProperty("Accept-Encoding", "identity");

			s = c.getInputStream();

			byte[] data = new byte[8192];
			int ch;
			int i = 0;
			while ((ch = s.read()) != -1) {
				if (i < data.length)
					data[i] = ((byte) ch);
				i++;

			}
			respString = new String(data);

		} catch (Exception ex) {

			throw new IOException("Error: " + ex);

		} finally {
			if (s != null)
				s.close();
			if (c != null)
				c.disconnect();
		}
		return respString;
	}

	public int getListeners(int bcastId) throws IOException {
		try {

			String response = this.getViaStreamConnection(WSListen,
					"/stats?bcast_id=" + bcastId);

			Element rootElement = getRootElement(response);

			Integer listeners = Integer.parseInt(getNodeValue(rootElement,
					"string"));

			debug.logV(TAG, "getListeners got " + listeners);

			return listeners;

		} catch (Exception e) {
			debug.logE(TAG, "getListeners ERROR", e.getCause());
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public Hashtable<String, String>[] getAllComments(Element rootElement) {
		Hashtable<String, String>[] list = null;

		if (rootElement == null) {
			return list;
		}

		NodeList i = rootElement.getElementsByTagName("comment_item");

		debug.logD(TAG, "getAllComments(): called");
		if (i != null) {
			Node r = null;
			int n = 0;
			list = new Hashtable[i.getLength()];

			while ((r = i.item(n)) != null) {
				NodeList l = r.getChildNodes();
				String username = null;
				String text = null;
				for (int x = 0; x < l.getLength(); x++) {
					if (l.item(x).getNodeName().equalsIgnoreCase("username")) {
						username = this.getElementValue(l.item(x));
					}
					if (l.item(x).getNodeName().equalsIgnoreCase("text")) {
						text = this.getElementValue(l.item(x));
					}
				}

				if ((username != null) && (text != null)) {
					debug.logD(TAG, "getAllComments(): username: " + username
							+ " text: " + text);
					list[n] = new Hashtable<String, String>();
					list[n].put("username", username);
					list[n].put("comment", text);
				}
				n++;
			}
		}

		// debug.logD(TAG, "getAllComments(): number of comments: " +
		// list.length);
		return list;

	}

	public Hashtable<String, String>[] getComments(Integer bcast_id)
			throws IOException {
		String url = "/api/get_comments.xml/" + bcast_id;
		return getCommentsFromURL(url);
	}

	public Hashtable<String, String>[] getComments(User user)
			throws IOException {
		String url = "/api/get_comments_live.xml/" + user.getUsername()
				+ "?cid=" + last_id;
		return getCommentsFromURL(url);
	}

	private Hashtable<String, String>[] getCommentsFromURL(String url)
			throws IOException {

		try {
			String response = this.getViaStreamConnection(WSServer, url);

			debug.logD(TAG, "getComments(): response " + response);
			Element rootElement = getRootElement(response);

			last_id = getLastID(rootElement);

			debug.logD(TAG, "getComments(): " + getAllComments(rootElement));

			return getAllComments(rootElement);

		} catch (Exception e) {
			debug.logE(TAG, "getCommentsFromURL ERROR", e.getCause());
		}
		return null;
	}

	private long getLastID(Element rootElement) {
		return 0;
	}

	public List<BroadcastDataSet> getTimelineAll(String token, Integer from,
			Integer to, Integer limit) throws IOException {
		return getTimeline(token, from, to, limit, ALL, null);
	}

	public List<BroadcastDataSet> getTimelineFriends(String token,
			Integer from, Integer to, Integer limit) throws IOException {
		return getTimeline(token, from, to, limit, FRIENDS, null);
	}

	public List<BroadcastDataSet> getTimelineHottest(String token,
			Integer from, Integer to, Integer limit) throws IOException {
		debug.logV(TAG, "getTimelineHottest, from " + from + " to " + to);
		return getTimeline(token, from, to, limit, HOTTEST, null);
	}

	public List<BroadcastDataSet> getTimelineProfile(String token,
			Integer from, Integer to, Integer limit) throws IOException {
		return getTimeline(token, from, to, limit, PROFILE, null);
	}

	public List<BroadcastDataSet> getTimelineUser(String token, String username,
			Integer from, Integer to, Integer limit) throws IOException {
		return getTimeline(token, from, to, limit, USER, username);
	}

	private List<BroadcastDataSet> getTimeline( String token, Integer from, Integer to, Integer limit, Integer type, String username ) throws IOException {
    	
		String from_str = "";
		String to_str = "";
		String limit_str = "";
		String username_str = "";
		
    	if ( from != null ) {
    		from_str = "&from=" + from.toString();
    	}
    	
    	if ( to != null ) {
    		to_str = "&to=" + to.toString(); 
    	}
    	
    	if ( limit != null ) {
    		limit_str = "&limit=" + limit.toString();
    	}
    	
    	if ( username != null ) {
    		username_str = "&username=" + username;
    	}
    	
    	String data = "access_token=" + token + "&list=" + type.toString() +
    		from_str +
    		to_str +
    		limit_str +
    		username_str;
    	String url = WSServer + "/api/get_timeline.xml";
    	
    	debug.logV(TAG, "getTimeline, data " + data);

    	return sendRequest(url, data);
    }
	
	public List<BroadcastDataSet> getTimelineSearch( String token, Integer from, Integer to, Integer limit, String search ) throws IOException {
		
		String from_str = "";
		String to_str = "";
		String limit_str = "";
		String search_str = "";
		
    	if ( from != null ) {
    		from_str = "&from=" + from.toString();
    	}
    	
    	if ( to != null ) {
    		to_str = "&to=" + to.toString();
    	}
    	
    	if ( limit != null ) {
    		limit_str = "&limit=" + limit.toString();
    	}
    	
    	if ( search != null ) {
    		search_str = "&keyword=" + search;
    	}
    	
    	String data = "access_token=" + token +
    		from_str +
    		to_str +
    		limit_str +
    		search_str;
    	
//    	debug.logV(TAG, "getTimelineSearch called with data " + data);
    	
    	String url = WSServer + "/api/search.xml";

    	return sendRequest(url, data);
    }

	public BroadcastDataSet getBroadcast(Integer bcastId) throws IOException {
		String data = "bcast_id=" + bcastId;
		String url = WSServer + "/api/get_broadcast.xml";

		List<BroadcastDataSet> retList = sendRequest(url, data);

		debug.logV(TAG, "getBroadcast for ID " + bcastId + " : " + retList);

		if (retList == null)
			return null;

		if (retList.size() == 1) {
			return retList.get(0);
		}

		return null;
	}
	
	public FlipUser getUser(String username, String token) throws IOException {
		String data = "username=" + username + "&access_token=" + token;
		String url = WSServer + "/api/get_user.xml";
			
		debug.logV(TAG, "getUser for username " + username);
		
		DefaultHttpClient hc = new DefaultHttpClient();

		ResponseHandler<String> res = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response)
					throws HttpResponseException, IOException {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				HttpEntity entity = response.getEntity();
				return entity == null ? null : EntityUtils.toString(entity,
						"UTF-8");
			}
		};

		HttpPost postMethod = new HttpPost(url);

		postMethod.getParams().setParameter(
				CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);

		if (data != null) {
			StringEntity tmp = null;
			try {
				tmp = new StringEntity(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				debug.logE(TAG, "getUser ERROR", e.getCause());
				return null;
			}

			postMethod.setEntity(tmp);
		}

		String response = hc.execute(postMethod, res);

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			UserHandler myUserHandler = new UserHandler();
			xr.setContentHandler(myUserHandler);

			InputSource inputSource = new InputSource();
			inputSource.setEncoding("UTF-8");
			inputSource.setCharacterStream(new StringReader(response));

			xr.parse(inputSource);

			FlipUser parsedData = myUserHandler.getParsedData();

			return parsedData;

		} catch (ParserConfigurationException e) {
			return null;
		} catch (SAXException e) {
			return null;
		}
	}
	
	private List<BroadcastDataSet> sendRequest(String url, String data)
			throws IOException {
		DefaultHttpClient hc = new DefaultHttpClient();

		ResponseHandler<String> res = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response)
					throws HttpResponseException, IOException {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				HttpEntity entity = response.getEntity();
				return entity == null ? null : EntityUtils.toString(entity,
						"UTF-8");
			}
		};

		HttpPost postMethod = new HttpPost(url);

		postMethod.getParams().setParameter(
				CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);

		if (data != null) {
			StringEntity tmp = null;
			try {
				tmp = new StringEntity(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				debug.logE(TAG, "sendRequest ERROR", e.getCause());
				return null;
			}

			postMethod.setEntity(tmp);
		}

		String response = hc.execute(postMethod, res);

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			TimelineHandler myTimelineHandler = new TimelineHandler();
			xr.setContentHandler(myTimelineHandler);

			InputSource inputSource = new InputSource();
			inputSource.setEncoding("UTF-8");
			inputSource.setCharacterStream(new StringReader(response));

			xr.parse(inputSource);

			List<BroadcastDataSet> parsedDataSet = myTimelineHandler
					.getParsedData();

			return parsedDataSet;

		} catch (ParserConfigurationException e) {
			return null;
		} catch (SAXException e) {
			return null;
		}
	}
	
	public void postComment(User user, String comment, Integer bcastId)
			throws IOException {
		String data = "access_token=" + user.getToken() + "&comment_txt="
				+ comment;

		String url = WSServer + "/api/post_comment.xml/" + bcastId;

		List<BroadcastDataSet> retList = sendRequest(url, data);

		debug.logV(TAG, "postComment for ID " + bcastId + " : " + retList);
	}

	public boolean isLive(String username) {
		/* we take an optimistic approach here, and return true in case of error */

		JSONArray resp = getStatus(username);

		if (resp == null) {
			return true;
		}

		String status = "LIVE";
		try {
			status = resp.getString(3);
			debug.logV(TAG, "isLive status is " + status);
		} catch (JSONException e) {
			return true;
		}

		if (status.equalsIgnoreCase("OFFLINE")) {
			return false;
		}

		return true;

	}

	public JSONArray getStatus(String username) {
		String url = WSServer + "/ajax/get_status/" + username;

		JSONArray response = null;
		try {
			response = sendJson(url, null);
		} catch (IOException e) {
			debug.logE(TAG, "getStatus ERROR", e.getCause());
		}

		return response;
	}

	private JSONArray sendJson(String url, String data) throws IOException {
		DefaultHttpClient hc = new DefaultHttpClient();

		ResponseHandler<String> res = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response)
					throws HttpResponseException, IOException {
				StatusLine statusLine = response.getStatusLine();
		
				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				HttpEntity entity = response.getEntity();
				return entity == null ? null : EntityUtils.toString(entity,
						"UTF-8");
			}
		};

		HttpPost postMethod = new HttpPost(url);

		postMethod.getParams().setParameter(
				CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);

		if (data != null) {
			StringEntity tmp = null;
			try {
				tmp = new StringEntity(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				debug.logE(TAG, "sendJson ERROR", e.getCause());
				return null;
			}

			postMethod.setEntity(tmp);
		}

		String response = hc.execute(postMethod, res);

		JSONTokener tokener = new JSONTokener(response);
		JSONArray jobj = null;
		try {
			jobj = new JSONArray(tokener);
		} catch (JSONException e) {
			debug.logE(TAG, "sendJson got exception " + response, e.getCause());
			return null;
		}

		debug.logV(TAG, "sendJson got " + jobj);

		return jobj;
	}

	public void playAircast(String bcastId) {
		debug.logV(TAG, "playAircast called for " + bcastId);
		String url = WSServer + "/ajax/play_aircast/" + bcastId;

		try {
			sendRequest(url, null);
		} catch (IOException e) {
			debug.logE(TAG, "playAircast ERROR", e.getCause());
		}
	}
	
	public boolean setFollow(String username, String token) throws IOException {
		FlipUser u = setFollowUnfollow(username, token, true);
		if ( u.isAuthorized()) {
			return true;
		}
		return false;
	}
	
	public boolean setUnfollow(String username, String token) throws IOException {
		FlipUser u = setFollowUnfollow(username, token, false);
		if ( u.isAuthorized() ) {
			return true;
		}
		return false;
	}
	
	private FlipUser setFollowUnfollow(String username, String token, boolean follow) throws IOException {
		String data = "username=" + username + "&access_token=" + token;
		String url;
		if ( follow ) {
			url = WSServer + "/api/set_follow.xml";	
		} else {
			url = WSServer + "/api/set_unfollow.xml";
		}
		
			
		debug.logV(TAG, "setFollow for username " + username);
		
		DefaultHttpClient hc = new DefaultHttpClient();

		ResponseHandler<String> res = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response)
					throws HttpResponseException, IOException {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() >= 300) {
					throw new HttpResponseException(statusLine.getStatusCode(),
							statusLine.getReasonPhrase());
				}

				HttpEntity entity = response.getEntity();
				return entity == null ? null : EntityUtils.toString(entity,
						"UTF-8");
			}
		};

		HttpPost postMethod = new HttpPost(url);

		postMethod.getParams().setParameter(
				CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);

		if (data != null) {
			StringEntity tmp = null;
			try {
				tmp = new StringEntity(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				debug.logE(TAG, "getUser ERROR", e.getCause());
				return null;
			}

			postMethod.setEntity(tmp);
		}

		String response = hc.execute(postMethod, res);

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			UserHandler myUserHandler = new UserHandler();
			xr.setContentHandler(myUserHandler);

			InputSource inputSource = new InputSource();
			inputSource.setEncoding("UTF-8");
			inputSource.setCharacterStream(new StringReader(response));

			xr.parse(inputSource);

			FlipUser parsedData = myUserHandler.getParsedData();

			return parsedData;

		} catch (ParserConfigurationException e) {
			return null;
		} catch (SAXException e) {
			return null;
		}
	}
	
	public boolean deleteAircast(Integer bcast_id, String access_token) {
		debug.logV(TAG, "deleteAircast called for " + bcast_id.toString());
		debug.logV(TAG, "deleteAircast called for token" + access_token);
		String url = WSServer + "/api/delete_aircast_id/" + bcast_id.toString();
		
		String data = "&access_token=" + access_token;

		try {
			sendRequest(url, data);
		} catch (IOException e) {
			debug.logE(TAG, "deleteAircast ERROR", e.getCause());
			return false;
		}
		
		return true;
	}		
	
}
