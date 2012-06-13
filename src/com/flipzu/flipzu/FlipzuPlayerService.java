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

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.flipzu.flipzu.WebServer.WebServer;

public class FlipzuPlayerService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener,
		MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener,
		MediaPlayer.OnSeekCompleteListener {

	private int NOTIFICATION = R.string.fz_player_service_started;

	private Debug debug = new Debug();
	private static final String TAG = "FlipzuPlayerService";
	public static final String INTENT_FILTER = "com.flipzu.flipzu.FZ_PLAYER_SERVICE";
	public static final String ACTION_PLAY = "com.flipzu.flipzu.ACTION_PLAY";
	public static final String ACTION_STOP = "com.flipzu.flipzu.ACTION_STOP";
	public static final String ACTION_PAUSE = "com.flipzu.flipzu.ACTION_PAUSE";
	public static final String ACTION_RESUME = "com.flipzu.flipzu.ACTION_RESUME";
	public static final String REQUEST_STATUS = "com.flipzu.flipzu.REQUEST_STATUS";
	public static final String REQUEST_TIMER = "com.flipzu.flipzu.REQUEST_TIMER";

	public static final String STATUS_PLAYING = "com.flipzu.flipzu.STATUS_PLAYING";
	public static final String STATUS_STOPPED = "com.flipzu.flipzu.STATUS_STOPPED";
	public static final String STATUS_PAUSED = "com.flipzu.flipzu.STATUS_PAUSED";
	public static final String STATUS_LOADING = "com.flipzu.flipzu.STATUS_LOADING";
	public static final String STATUS_ERROR = "com.flipzu.flipzu.STATUS_ERROR";
	public static final String STATUS_FINISHED = "com.flipzu.flipzu.STATUS_FINISHED";
	public static final String STATUS_UPDATE = "com.flipzu.flipzu.STATUS_UPDATE";

	public static final String MEDIAPLAYER_SEEK = "com.flipzu.flipzu.MEDIAPLAYER_SEEK";

	private boolean isLoading = false;

	WifiLock wifilock = null;

	private String mUrl;
	private String mTitle;
	private boolean mLive;

	private Integer mConnectionType = -1; // disconnected

	Intent intent;

	MediaPlayer mediaPlayer = null;

	private Handler seekBarHandler;
	private int bufferPercent = 0;

	/* handler phone state changes */
	PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				// Incoming call: Pause music
				pauseOrStop();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// Not in call: Play music
				// playOrResume();
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// A call is dialing, active or on hold
				pauseOrStop();
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				super.handleMessage(msg);
			}
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/* monitors network state */
	private final BroadcastReceiver networkMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				return;
			}
			NetworkInfo networkInfo = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if (mConnectionType == -1) {
				mConnectionType = networkInfo.getType();
			} else {

				if (networkInfo.isConnected()
						&& mConnectionType != networkInfo.getType()) {
					mConnectionType = networkInfo.getType();
					// new connection type, reload bcast

					if (mediaPlayer.isPlaying() && mLive) {
						debug.logV(TAG, "networkMonitor, new network type "
								+ networkInfo.getTypeName() + ". Restarting");
						mediaPlayer.stop();
						mediaPlayer.reset();
						start();
					}

				}

			}
		}
	};

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		FlipzuPlayerService getService() {
			return FlipzuPlayerService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return START_STICKY;

		if (intent.getAction() != null) {
			debug.logV(TAG, "onStartCommand(): Received start id " + startId
					+ ": " + intent.getAction());
			if (intent.getAction().equals(ACTION_STOP)) {
				// releaseWifiLock();
				// stop();
			}
			if (intent.getAction().equals(ACTION_PAUSE)) {
				releaseWifiLock();
				pause();
			}
			if (intent.getAction().equals(ACTION_RESUME)) {
				grabWifiLock();
				resume();
			}
			if (intent.getAction().equals(ACTION_PLAY)) {
				grabWifiLock();
				String url = intent.getDataString();
				mTitle = intent.getStringExtra("title");
				mLive = intent.getBooleanExtra("live", false);
				debug.logV(TAG, "URI " + url);
				if (url != null) {
					if (mLive
							&& android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
						WebServer webServer = WebServer.startWebServer(
								FlipzuPlayerService.this, url);
						synchronized (this) {
							do {
								try {
									this.wait(100);
								} catch (InterruptedException e) {
									break;
								}
							} while (!webServer.isReady());
						}
						mUrl = "http://127.0.0.1:46812";
					} else
						mUrl = url;

					start();
				}

			}
			if (intent.getAction().equals(REQUEST_STATUS)) {
				sendStatus();
			}
			if (intent.getAction().equals(REQUEST_TIMER)) {
				sendTimer();
			}
			if (intent.getAction().equals(MEDIAPLAYER_SEEK)) {
				int position = intent.getIntExtra("position", mediaPlayer
						.getCurrentPosition());
				if (position < (mediaPlayer.getDuration() * bufferPercent / 100))
					mediaPlayer.seekTo(intent.getIntExtra("position",
							mediaPlayer.getCurrentPosition()));
			}
		} else {
			debug.logV(TAG, "onStartCommand(): Received start id " + startId
					+ ": " + intent);
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		debug.logV(TAG, "onCreate()");

		super.onCreate();

		if (mediaPlayer == null) {
			initMediaPlayer();
		}

		if (wifilock == null) {
			debug.logV(TAG, "onCreate, creating wifilock");
			wifilock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
					.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		}

		intent = new Intent(INTENT_FILTER);

		setPhoneStateListener();

		registerReceiver(networkMonitor, new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE"));

	}

	private void sendTimer() {
		debug.logV(TAG, "entered sendTimer()");

		if (mediaPlayer == null)
			return;

		// don't use timer if we're live
		if (mLive) {
			return;
		}

		Integer duration = mediaPlayer.getDuration();
		Integer position = mediaPlayer.getCurrentPosition();

		String pos = millisToSecs(position);
		String dur = millisToSecs(duration);

		debug.logV(TAG, "sendTimer, got " + pos + "/" + dur);

		intent.putExtra("duration", dur);
		intent.putExtra("position", pos);
		intent.putExtra("state", STATUS_UPDATE);
		sendBroadcast(intent);
	}

	private void sendStatus() {
		debug.logV(TAG, "entered sendStatus()");
		if (mediaPlayer == null) {
			sendStopped();
			return;
		}

		if (mediaPlayer.isPlaying()) {
			sendPlaying();
			return;
		}

		if (isLoading) {
			sendLoading();
			return;
		}

		sendStopped();
	}

	private void sendPlaying() {
		debug.logV(TAG, "entered sendPlaying()");

		intent.putExtra("state", STATUS_PLAYING);
		sendBroadcast(intent);
		if(!mLive){
			seekBarHandler = new Handler();
			seekBarHandler.post(seekBarRunnable);
		}
	}

	private void sendLoading() {
		debug.logV(TAG, "entered sendLoading()");
		isLoading = true;
		intent.putExtra("state", STATUS_LOADING);
		sendBroadcast(intent);
	}

	private void sendStopped() {
		debug.logV(TAG, "entered sendStopped()");
		intent.putExtra("state", STATUS_STOPPED);
		sendBroadcast(intent);
	}

	private void sendFinished() {
		debug.logV(TAG, "entered sendFinished()");
		intent.putExtra("state", STATUS_FINISHED);
		sendBroadcast(intent);
	}

	private void sendPaused() {
		debug.logV(TAG, "entered sendPaused()");
		intent.putExtra("state", STATUS_PAUSED);
		sendBroadcast(intent);
		if (!mLive && seekBarHandler != null)
			seekBarHandler.removeCallbacks(seekBarRunnable);
	}

	private void sendError() {
		debug.logV(TAG, "sendError()");
		intent.putExtra("state", STATUS_ERROR);
		sendBroadcast(intent);
	}

	private void initMediaPlayer() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
	}

	private void prepareMediaPlayer() {

		mediaPlayer.reset();
		try {

			if (mUrl == null) {
				debug.logV(TAG,
						"prepareMediaPlayer(), setDataSource with NULL mUrl!");
				return;
			}
			String url;
			// fuck web cache
			if ( !mLive || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1 ) {
				url = mUrl;
			} else {
				int rand = (int) (Math.random() * 10000);
				url = mUrl + "?rand=" + rand;					
			}
			debug.logV(TAG, "prepareMediaPlayer(), setDataSource with " + url);
			mediaPlayer.setDataSource(url);
		} catch (IllegalArgumentException e) {
			debug.logE(TAG, "prepareMediaPlayer ERROR", e.getCause());
			sendError();
			return;
		} catch (IllegalStateException e) {
			debug.logE(TAG, "prepareMediaPlayer ERROR", e.getCause());
			sendError();
			return;
		} catch (IOException e) {
			debug.logE(TAG, "prepareMediaPlayer ERROR", e.getCause());
			sendError();
			return;
		}
		mediaPlayer.prepareAsync();
	}

	public void resume() {
		if (mediaPlayer == null)
			return;

		mediaPlayer.start();

		sendPlaying();
	}

	public void start() {

		debug.logV(TAG, "start()");

		if (mediaPlayer == null) {
			initMediaPlayer();
			return;
		}

		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			mediaPlayer.reset();
		}

		sendLoading();
		prepareMediaPlayer();
	}

	public void pause() {
		if (mediaPlayer == null)
			return;

		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		}

		sendPaused();
		debug.logV(TAG, "pause()");
	}

	public void stop() {

		this.stopForeground(true);

		if (seekBarHandler != null) {
			seekBarHandler.removeCallbacks(seekBarRunnable);
		}

		if (mediaPlayer == null) {
			return;
		}

		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
		}

		sendStopped();

		mediaPlayer.reset();

		debug.logV(TAG, "stop()");
	}

	public boolean isRunning() {
		if (mediaPlayer != null && mediaPlayer.isPlaying())
			return true;

		return false;
	}

	@Override
	public void onDestroy() {
		debug.logV(TAG, "onDestroy()");

		sendStopped();

		if (mediaPlayer != null)
			mediaPlayer.release();
		
		if (seekBarHandler != null) {
			seekBarHandler.removeCallbacks(seekBarRunnable);
		}

		releaseWifiLock();
		releasePhoneStateListener();

		unregisterReceiver(networkMonitor);
	}

	@Override
	public IBinder onBind(Intent intent) {
		debug.logV(TAG, "onBind()");
		return mMessenger.getBinder();
	}

	private Runnable seekBarRunnable = new Runnable() {
		@Override
		public void run() {
			
			try {
				if ( mediaPlayer.isPlaying() ) {
					int total = mediaPlayer.getDuration();

					Intent intent = new Intent(INTENT_FILTER);
					intent.putExtra("seekBarUpdate", true);
					intent.putExtra("bufferPercent",
							(int) (total * bufferPercent / 100));
					intent
							.putExtra("currentPosition", mediaPlayer
									.getCurrentPosition());
					intent.putExtra("total", total);
					sendBroadcast(intent);
				}
			} catch (Exception e) {
				debug.logE(TAG, "seekBarRunnable error " + e.getMessage(), e.getCause());
			}
			seekBarHandler.postDelayed(this, 1000);
		}
	};

	@Override
	public void onPrepared(MediaPlayer mp) {
		debug.logV(TAG, "onPrepared()");
		// mp.start();
		debug.logV(TAG, "getDuration() " + mp.getDuration());

		if (mLive
				&& android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1)
			mp.seekTo(157500);
		else {
			mp.start();
			showNotification();
			sendPlaying();
		}
		isLoading = false;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if (mediaPlayer != null) {
			mediaPlayer.reset();
		}

		isLoading = false;

		debug.logV(TAG, "onError() called");
		sendError();
		return false;
	}

	private void showNotification() {
		// assign the song name to songName
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
				0, new Intent(getApplicationContext(), Player.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification();
		notification.tickerText = mTitle;
		notification.icon = R.drawable.ic_stat_example;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(getApplicationContext(),
				"FlipZu Player", mTitle, pi);

		this.startForeground(NOTIFICATION, notification);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		debug.logV(TAG, "onCompletion()");
		sendFinished();
		stop();
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {		
		if (!mLive && mp.isPlaying()) {
			bufferPercent = percent;

			Intent intent = new Intent(INTENT_FILTER);
			intent.putExtra("bufferUpdate", true);
			intent.putExtra("bufferPercent", (int)(mp.getDuration()
			 * bufferPercent / 100));
			intent.putExtra("total", mp.getDuration());
			sendBroadcast(intent);
		}

		debug.logV(TAG, "onBufferingUpdate() " + Integer.toString(percent));
		// if ( percent == 0 ) {
		// sendLoading();
		// } else {
		// if ( mp.isPlaying() ) {
		// sendPlaying();
		// }
		// }
		// debug.logV(TAG, "onBufferingUpdate() player state " +
		// mediaPlayer.getCurrentPosition() + "/" + mediaPlayer.getDuration());
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		debug.logV(TAG, "onInfo() called with " + what);

		switch (what) {
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			if (mp != null) {
				debug.logV(TAG, "onInfo() player state "
						+ mp.getCurrentPosition() + "/" + mp.getDuration());
			} else {
				debug.logV(TAG, "onInfo() player state, mp is null");
			}
			return true;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			debug.logV(TAG, "onInfo() media not seekable");
			return true;
		}

		return false;
	}

	private String millisToSecs(Integer milliseconds) {

		int seconds = (int) ((milliseconds / 1000) % 60);
		int minutes = (int) ((milliseconds / 1000) / 60);

		String ret = String.format("%d:%02d", minutes, seconds);

		return ret;
	}

	private void grabWifiLock() {
		if (wifilock != null && !wifilock.isHeld()) {
			debug.logV(TAG, "grabWifiLock, acquiring");
			wifilock.acquire();
		}
	}

	private void releaseWifiLock() {
		if (wifilock != null && wifilock.isHeld()) {
			debug.logV(TAG, "releaseWifiLock, releasing");
			wifilock.release();
		}
	}

	private void setPhoneStateListener() {
		debug.logV(TAG, "setPhoneStateListener");
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr
					.listen(phoneStateListener,
							PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	private void releasePhoneStateListener() {
		debug.logV(TAG, "releasePhoneStateListener");
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
	}

	private void pauseOrStop() {
		debug.logV(TAG, "pauseOrStop called");
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				if (mLive) {
					stop();
				} else {
					pause();
				}
			}
		}
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		debug.logV(TAG, "onSeekCompleted()");
		mp.start();

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				showNotification();
				sendPlaying();
			}
		}, 3000);
	}
}
