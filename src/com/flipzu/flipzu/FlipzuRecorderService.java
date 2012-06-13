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

import static org.jboss.netty.buffer.ChannelBuffers.buffer;

import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class FlipzuRecorderService extends Service implements Observer {
	private int NOTIFICATION = R.string.fz_recorder_service_started;
	private int NOTIF_ID = R.string.fz_recorder_alert;
	private Debug debug = new Debug();
	private static final String TAG = "FlipzuRecorderService";
	Intent intent;
	public static final String INTENT_FILTER = "com.flipzu.flipzu.FZ_RECORDER_SERVICE";
	public static final String REQUEST_STATUS = "com.flipzu.flipzu.Recorder.REQUEST_STATUS";
	public static final String ACTION_REC = "com.flipzu.flipzu.Recorder.ACTION_REC";
	public static final String ACTION_STOP = "com.flipzu.flipzu.Recorder.ACTION_STOP";

	/* local status */
	public static final String STATUS_RECORDING = "com.flipzu.flipzu.Recorder.STATUS_RECORDING";
	public static final String STATUS_STOPPED = "com.flipzu.flipzu.Recorder.STATUS_STOPPED";
	public static final String STATUS_CONNECTING = "com.flipzu.flipzu.Recorder.STATUS_CONNECTING";
	public static final String STATUS_STOPPING = "com.flipzu.flipzu.Recorder.STATUS_STOPPING";
	public static final String STATUS_FINISHED_ENCODING = "com.flipzu.flipzu.Recorder.STATUS_FINISHED_ENCODING";
	private String mState = STATUS_STOPPED;

	/* Error Codes */
	static final int AUTH_FAILED = 1;
	static final int MIC_FAILED = 2;
	static final int SLOW_CPU = 3;
	static final int SLOW_NETWORK = 4;
	static final int NO_CONNECTION = 5;
	static final int CONNECTION_LOST = 6;
	private Integer mError = 0;

	private String mTitle;
	private String mKey;
	private Integer mBcastId;

	/* connection settings */
	private String mServerAddr = "live.flipzu.com";
	private Integer mServerPort = 443;
	ClientBootstrap mBootstrap = null;
	Channel mChannel = null;

	/* microphone aux buffer to measure amplitude */
	private static byte[] amp_buf = null;

	/* microphone */
	AudioRecord mic = null;
	int mFreq = 11025; // default

	/* buffers */
	byte[] readbuffer; // PCM
	int recBufferSize = 0;
	int write_sz = 1024; // packet size, in bytes

	/* shared buffers of raw/encoded data */
	/* 2MB should be more than enough for offline cache */
	private ByteBuffer pcm_buffer = ByteBuffer.allocateDirect(2097152);
	private ByteBuffer enc_buffer = ByteBuffer.allocateDirect(204800); // ~50secs
	// at
	// 32kbps

	/* position index for recording/encoding threads */
	private int rec_index = 0;
	private int enc_index = 0;

	/* signaling between threads */
	final Lock lock = new ReentrantLock();
	final Condition recording = lock.newCondition();
	final Condition has_audio = lock.newCondition();
	final Condition has_encoded = lock.newCondition();
	final Condition recording_ended = lock.newCondition();
	final Condition encoding_ended = lock.newCondition();

	/* task handlers */
	AsyncTask<Void, Integer, Void> connectTask = null;
	AsyncTask<Void, Integer, Void> recTask = null;
	AsyncTask<Void, Integer, Void> encodeTask = null;
	AsyncTask<Void, Integer, Void> bcastTask = null;

	/* start rec time */
	private long startTime = 0;
	
	/* wifi lock */
	WifiLock wifilock = null;

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
	private Integer mConnectionType = -1; // disconnected
    private final BroadcastReceiver networkMonitor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }
            NetworkInfo networkInfo =
                (NetworkInfo)intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if ( mConnectionType == -1 ) {
            	mConnectionType = networkInfo.getType();
            } else {
            	
                if (networkInfo.isConnected() && mConnectionType != networkInfo.getType() ) {
        			mConnectionType = networkInfo.getType();
        			// new connection type, restart bcast if recording
        			if ( mState == STATUS_RECORDING ) {
            			stopRec(true);
            			disconnect();
            			debug.logV(TAG, "networkMonitor, restarting bcast");
            			synchronized(this) {
            				try {
    							this.wait(3000); // wait 3 seconds....
    						} catch (InterruptedException e) {
    							// pass
    						}
            			}
            			startRec();        				
        			}
                }   
                
            }
        }
    };
    
    /* handler phone state changes */
    PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                //Incoming call: stop Rec
            	stopRec(false);
            	disconnect();
            } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                //Not in call: Play music
            } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                //A call is dialing, active or on hold
            	stopRec(false);
            	disconnect();
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
        

	@Override
	public void onCreate() {
		debug.logV(TAG, "onCreate()");
		
		super.onCreate();

		intent = new Intent(INTENT_FILTER);

		if (!isOnline()) {
			mState = STATUS_STOPPED;
			mError = NO_CONNECTION;
		}

		if (!initMicrophone()) {
			mState = STATUS_STOPPED;
			mError = MIC_FAILED;
		}

		/* catch incoming calles */
		setPhoneStateListener();

		/* network monitor */
		registerReceiver(networkMonitor, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		
		/* observer for netty (Broadcast class)*/
		Broadcast.getInstance().addObserver(this);

	}
	
    @Override
    public void onDestroy() {    
    	debug.logV(TAG, "onDestroy()");
    	
    	/* stop threads */
    	stopRec(true);
    	disconnect();

    	/* cleanup */
    	releaseWifiLock();
    	releasePhoneStateListener();
    	
    	unregisterReceiver(networkMonitor);
    	
    	/* remove observer */
    	Broadcast.getInstance().deleteObservers();
    }

	@Override
	public IBinder onBind(Intent intent) {
		debug.logV(TAG, "onBind()");
		return mMessenger.getBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return START_STICKY;

		if (intent.getAction() != null) {
			debug.logV(TAG, "onStartCommand(): Received start id " + startId
					+ ": " + intent.getAction());
			if (intent.getAction().equals(ACTION_REC)) {
				debug.logV(TAG, "ACTION_REC");
				mTitle = intent.getStringExtra("title");
				mKey = intent.getStringExtra("key");
				startRec();
				startTime = 0;
			}
			if (intent.getAction().equals(ACTION_STOP)) {
				debug.logV(TAG, "ACTION_STOP");
				stopRec();
				disconnect();
			}
			if (intent.getAction().equals(REQUEST_STATUS)) {
				debug.logV(TAG, "REQUEST_STATUS");
				sendStatus();
			}
		} else {
			debug.logV(TAG, "onStartCommand(): Received start id " + startId
					+ ": " + intent);
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	/* send status update */
	private void sendStatus() {
		debug.logV(TAG, "entered sendStatus()");
		
		if ( mError != 0 ) {
			showNotificationMessage();
		}

		intent.putExtra("state", mState);
		intent.putExtra("err_code", mError);
		intent.putExtra("start_time", startTime);
		intent.putExtra("bcast_id", mBcastId);
		sendBroadcast(intent);

		mError = 0;
	}

	/* start recording */
	private void startRec() {
		debug.logV(TAG, "startRec()");

		/* start microphone */
		if (!startMic()) {
			mState = STATUS_STOPPED;
			mError = MIC_FAILED;
			sendStatus();
			return;
		}
		
		/* connect */
		if ( connectTask == null ) {
			if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
				connectTask = new ConnectTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				connectTask = new ConnectTask().execute();	
			}
			
		}

	}
	
	private void stopRec() { 
		stopRec(false);
	}

	/* stop recording */
	private void stopRec(boolean force) {
		debug.logV(TAG, "stopRec()");
		
		/* update status */
		mState = STATUS_STOPPED;

		/* update global status */
		sendStatus();
		
		/* stop connect task */
		if ( connectTask != null ) {
			connectTask.cancel(force);
			connectTask = null;
		}
		
		/* stop recording */
		if (recTask != null) {
			recTask.cancel(force);
			recTask = null;
		}

		/* stop encoding */
		if (encodeTask != null) {
			encodeTask.cancel(force);
			encodeTask = null;
		}

		/* stop broadcasting */
		if (bcastTask != null) {
			bcastTask.cancel(force);
			bcastTask = null;
		}
		
		/* remove icon from notification area */
		this.stopForeground(true);
	}

	/* shows icon in notification area */
	private void showNotification() {
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
				0, new Intent(getApplicationContext(), Recorder.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification();
		notification.tickerText = mTitle;
		notification.icon = R.drawable.ic_rec;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(getApplicationContext(),
				"FlipZu Recorder", mTitle, pi);

		this.startForeground(NOTIFICATION, notification);
	}

	/* connect to streaming server */
	private boolean connect() {
		debug.logV(TAG, "connect()");
		
		/* don't turn wifi off */
		grabWifiLock();

		mState = STATUS_CONNECTING;
		sendStatus();
		
		mBootstrap = new ClientBootstrap( 
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
	
		mBootstrap.setPipelineFactory(new FlipzuRecorderFactory());
		
		mBootstrap.setOption("tcpNoDelay", true);
		mBootstrap.setOption("keepAlive", true);
		
		ChannelFuture future = mBootstrap.connect(new InetSocketAddress(
				mServerAddr, mServerPort));
		
		
		mChannel = future.awaitUninterruptibly().getChannel();
		
		if (!future.isSuccess()) {
			debug.logV(TAG, "connect(), can't get channel");
			mBootstrap.releaseExternalResources();
		}
		
		if ( mChannel.isWritable() ) {
			future = mChannel.write(mKey + "\n").awaitUninterruptibly();	
		}
				
		return true;

	}

	/* disconnect from streaming server */
	private void disconnect() {
		debug.logV(TAG, "disconnect()");
		
		/* release wifi lock */
		releaseWifiLock();
		
		/* disconnect */
		if ( mChannel != null ) {
			mChannel.close();
			mChannel = null;
		}
		if ( mBootstrap != null ) {
			mBootstrap.releaseExternalResources();
			mBootstrap = null;
		}		
	}

	/* initialize microphone */
	private boolean initMicrophone() {
		Integer[] freqs;
		/* Initialize microphone */
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ECLAIR_MR1) {
			Integer[] f = { 11025, 22050, 44100, 16000, 8000, 32000 };
			freqs = f.clone();
		} else {
			Integer[] f = { 22050, 11025, 44100, 16000, 8000, 32000 };
			freqs = f.clone();
		}

		int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

		for (Integer freq : freqs) {
			int bufferSize = AudioRecord.getMinBufferSize(freq,
					channelConfiguration, audioEncoding);

			if (bufferSize < 0) {
				debug.logD(TAG, "RecTask(): can't get buffer size");
				return false;
			}

			// read() 5 times per second.
			recBufferSize = (freq * 2) / 5;

			if (recBufferSize < bufferSize) {
				recBufferSize = bufferSize;
			}

			debug.logV(TAG, "initMicrophone: bufferSize: " + bufferSize
					+ " recBufferSize " + recBufferSize);
			debug.logV(TAG, "initMicrophone, trying " + freq);
			try {
				mic = new AudioRecord(MediaRecorder.AudioSource.MIC, freq,
						channelConfiguration, audioEncoding, recBufferSize);
				mFreq = freq;
				break;
			} catch (Exception e) {
				// pass
			}
		}

		/* create PCM and encoded data buffers */
		readbuffer = new byte[recBufferSize];

		/* helper buffer for getMaxAmplitude */
		amp_buf = new byte[recBufferSize];

		return true;
	}

	/* start recording from the microphone */
	private boolean startMic() {
		
		if ( mic == null ) {
			if(!initMicrophone()) {
				return false;
			}
		}
		
		if ( mic == null ) {
			return false;
		}
		
		try {
			mic.startRecording();
		} catch (IllegalStateException e) {
			debug.logE(TAG, "startMic() error", e.getCause());
			mic.release();
			mic = null;
			return false;
		}

		return true;
	}

	/* stop and release microphone */
	private void stopMic() {
		mic.stop();
		mic.release();
		mic = null;
	}
	
	class ConnectTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			debug.logD(TAG, "ConnecTask(): starting...");
			
			if (!connect()) {
				mState = STATUS_STOPPED;
				mError = NO_CONNECTION;
				sendStatus();
				return null;
			}
			
			/* start recording thread */
			if (recTask == null) {
				if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
					recTask = new StartRecTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);				
				} else {
					recTask = new StartRecTask().execute();
				}
				
			}
	
			/* start encoding thread */
			if (encodeTask == null) {
				if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
					encodeTask = new StartEncodeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);	
				} else {
					encodeTask = new StartEncodeTask().execute();
				}
				
			}
	
			/* start broadcast thread */
			if (bcastTask == null) {
				if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB ) {
					bcastTask = new StartBcastTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);	
				} else {
					bcastTask = new StartBcastTask().execute();
				}
				
			}
			
			return null;
		}
		
	}

	/* recording thread */
	class StartRecTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			debug.logD(TAG, "RecTask(): starting...");
			
			Amplitude maxAmp = Amplitude.getInstance();

			/* wait the for Broadcast task to start the recording. */
			lock.lock();
			try {
				recording.await(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				debug.logE(TAG,
						"StartRecTask: interrupted waiting for isRecording", e
								.getCause());
			}
			lock.unlock();

			if (mState != STATUS_RECORDING) {
				/* signal end of recording */
				lock.lock();
				recording_ended.signalAll();
				lock.unlock();
				debug.logD(TAG, "RecTask(): ending task, mState not RECORDING");
				return null;
			}

			/* start recording loop */
			int bytes;
			pcm_buffer.clear();
			while (pcm_buffer.hasRemaining()
					&& (mState == STATUS_RECORDING || mState == STATUS_CONNECTING)) {
				if ( mic == null ) {
					initMicrophone();
				}
				bytes = mic.read(readbuffer, 0, recBufferSize);
				// save for amplitude calculation
				amp_buf = readbuffer.clone();
				maxAmp.setAmplitude(getMaxAmplitude());
				if (bytes > 0) { // there's data in the mic
					lock.lock();
					pcm_buffer.position(rec_index);
					try {
						pcm_buffer.put(readbuffer, 0, bytes);
					} catch (BufferOverflowException e) {
						// out of buffer space. Let's stop this.
						debug
								.logE(
										TAG,
										"RecTask(): out of buffer space. Stopping bcast.",
										e.getCause());
						mState = STATUS_STOPPED;
						mError = SLOW_CPU;
						sendStatus();
						rec_index -= bytes;
					}
					rec_index += bytes;
					has_audio.signalAll();
					debug.logD(TAG, "RecTask(): read " + bytes + " to encode "
							+ rec_index + " bytes, remaining "
							+ pcm_buffer.remaining());
					lock.unlock();
				}
			}

			/* turn off microphone */			
			stopMic();

			/* signal end of recording */
			lock.lock();
			recording_ended.signalAll();
			lock.unlock();
			
			debug.logD(TAG, "RecTask(): ending task");

			return null;
		}
	}

	/* encoding thread */
	class StartEncodeTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			debug.logD(TAG, "EncodeTask(): started");

			// Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);

			/* wait the for Broadcast task to start the recording. */
			lock.lock();
			try {
				recording.await(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				debug.logE(TAG,
						"EncodeTask: interrupted waiting for isRecording", e
								.getCause());
			}
			lock.unlock();

			if (mState != STATUS_RECORDING) {
				debug.logD(TAG, "EncodeTask(): No recording, ending task");
				return null;
			}

			/* init encoder // 32kbps, <frequency>Hz, 1 channel */
			int frame_size = Encoder.encoderInit(32000, mFreq, 1);
			if (frame_size < 0) {
				debug
						.logW(TAG,
								"EncodeTask(): can't init encoder. Stopping and ending task");
				mState = STATUS_STOPPED;
				sendStatus();
				return null;
			}

			/* clear encoded buffer */
			enc_buffer.clear();

			/* start encoding loop */
			byte[] pcm_buf = new byte[frame_size];
			byte[] enc_buf = new byte[frame_size];

			int enc_bytes, index;
			while ((mState == STATUS_RECORDING || mState == STATUS_CONNECTING)
					&& enc_buffer.hasRemaining()) {

				lock.lock();
				index = rec_index;
				lock.unlock();
				if (index >= frame_size) { // there's data from the mic
					lock.lock();
					pcm_buffer.position(0);
					pcm_buffer.get(pcm_buf, 0, frame_size);
					pcm_buffer.compact();
					rec_index -= frame_size;
					lock.unlock();

					// encode data and put it in encoded buffer
					enc_bytes = Encoder.audioEncode(pcm_buf, frame_size,
							enc_buf);
					if (enc_bytes > 0) {
						lock.lock();
						try {
							enc_buffer.position(enc_index);
						} catch (IllegalArgumentException e) {
							mState = STATUS_STOPPED;
							sendStatus();
							debug
									.logW(TAG,
											"EncodeTask(): enc_index is bad! Stopping.");
						}

						try {
							enc_buffer.put(enc_buf, 0, enc_bytes);
						} catch (BufferOverflowException e) {
							// out of buffer space. Let's stop this.
							mState = STATUS_STOPPED;
							mError = SLOW_NETWORK;
							sendStatus();
							stopRec(true); /* force cleanup */
							disconnect();
							debug
									.logW(TAG,
											"EncodeTask(): out of buffer space, stopping bcast");
							enc_index -= enc_bytes;
						}
						enc_index += enc_bytes;
						has_encoded.signalAll();
						debug.logD(TAG, "EncodeTask(): encoded " + enc_bytes
								+ " to encode " + index + " bytes, remaining "
								+ enc_buffer.remaining());
						lock.unlock();
					}
				} else { // wait for more data from the microphone
					lock.lock();
					try {
						while ((rec_index < frame_size)
								&& (mState == STATUS_RECORDING)) {
							debug.logV(TAG,
									"EncodeTask(): wait for audio in mic");
							has_audio.await(5, TimeUnit.SECONDS);
						}
					} catch (InterruptedException e) {
						debug
								.logE(
										TAG,
										"EncodeTask(): interrupted waiting for audio from Mic.",
										e.getCause());
						lock.unlock();
					} finally {
						lock.unlock();
					}
				}
			}

			/* wait for recording to finish */
			if (mState == STATUS_STOPPED) {
				lock.lock();
				try {
					debug.logV(TAG,
							"EncodeTask: waiting for recording to finish");
					recording_ended.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					debug
							.logW(TAG,
									"EncoderTask: interrupted waiting for RecordingEnded");
				}
				lock.unlock();
			}

			/* encode the remaining of the buffer */
			debug.logD(TAG, "EncodeTask(): encoding remaining");
			lock.lock();
			index = rec_index;
			lock.unlock();
			while ((index >= frame_size) && (mState == STATUS_STOPPING)) {
				lock.lock();
				pcm_buffer.position(0);
				pcm_buffer.get(pcm_buf, 0, frame_size);
				pcm_buffer.compact();
				rec_index -= frame_size;
				lock.unlock();
				enc_bytes = Encoder.audioEncode(pcm_buf, frame_size, enc_buf);
				debug.logD(TAG, "EncodeTask(): encoded " + enc_bytes
						+ " to encode " + rec_index + " bytes, remaining "
						+ enc_buffer.remaining());
				lock.lock();
				try {
					if (enc_buffer.remaining() > enc_bytes)
						enc_buffer.put(enc_buf, 0, enc_bytes);
				} catch (BufferOverflowException e) {
					// out of buffer space. Let's stop this.
					debug
							.logW(TAG,
									"EncodeTask(): out of buffer space while encoding remaining, stopping bcast");
					lock.unlock();
					mState = STATUS_STOPPED;
					sendStatus();
					break;
				}
				enc_index += enc_bytes;
				has_encoded.signalAll();
				// rec_index -= frame_size;
				index = rec_index;
				lock.unlock();
			}

			mState = STATUS_FINISHED_ENCODING;

			/* close encoder */
			Encoder.encoderClose();

			/* signal end of encoding */
			lock.lock();
			encoding_ended.signalAll();
			lock.unlock();

			debug.logD(TAG, "EncodeTask(): ending task");

			return null;
		}
	}

	/* broadcasting thread */
	class StartBcastTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			debug.logD(TAG, "BcastTask(): stating.");

			/* number of times to retry to recover from a network error */
			int conn_nr = 0;
			int limit = 10;
			
			int index;
			
			byte[] write_buf = new byte[write_sz];

			debug.logV(TAG, "BcastTask(): restarting " + mTitle);

			/* only save original start time */
			if ( startTime == 0 ) {
				startTime = System.currentTimeMillis();	
			}
			
			
			debug.logD(TAG, "BcastTask(): bcast_id " + mBcastId);

			/* signal that we're broadcasting */
			mState = STATUS_RECORDING;
			showNotification();
			sendStatus();
			lock.lock();
			recording.signalAll();
			lock.unlock();

			/* start broadcast loop */

			debug.logD(TAG, "BcastTask(): starting broadcast loop");

			while (mState == STATUS_RECORDING || mState == STATUS_STOPPING) {
				lock.lock();
				index = enc_index;
				lock.unlock();
				if (index >= write_sz) { // there's encoded data
					// available
					debug.logD(TAG, "BcastTask(): encoding data available");
					lock.lock();
					enc_buffer.position(0);
					enc_buffer.get(write_buf, 0, write_sz);
					enc_buffer.compact();
					enc_index -= write_sz;
					lock.unlock();

					try {
						if ( mChannel.isWritable() ) {
							ChannelBuffer buf =  buffer(write_buf.length);
							buf.writeBytes(write_buf);
							
							if ( mChannel.write(buf).await(3, TimeUnit.SECONDS) ) {
								debug.logV(TAG, "BcastTask, write success");
								conn_nr = 0;
							} else {
								conn_nr++;
								debug.logV(TAG, "BcastTask, write failed, conn " + conn_nr);
								if ( conn_nr >= limit ) {
									mState = STATUS_STOPPED;
									mError = CONNECTION_LOST;
									debug.logV(TAG, "BcastTask, limit " + limit + " reached, stopping");
								}
							}
						} else {
							conn_nr++;
							debug.logV(TAG, "BcastTask, channel not writable, conn " + conn_nr);
							if ( conn_nr >= limit ) {
								mState = STATUS_STOPPED;
								mError = CONNECTION_LOST;
								debug.logV(TAG, "BcastTask, limit " + limit + " reached, stopping");
							}							
						}
						
					} catch (Exception e) {
						debug.logE(TAG,
								"BcastTask(): can't write into socket", e.getCause());
					}
				} else { // wait for more encoded data
					debug.logD(TAG, "BcastTask(): wait for data");
					lock.lock();
					try {
						while ((enc_index < write_sz)
								&& (mState == STATUS_RECORDING)) {
							has_encoded.await(5, TimeUnit.SECONDS);
						}
					} catch (InterruptedException e) {
						debug
								.logE(
										TAG,
										"BcastTask(): interrupted waiting for encoded data.",
										e.getCause());
						lock.unlock();
					} finally {
						lock.unlock();
					}
				}
			}


			/* wait for encoding to finish */
			 if ( mState == STATUS_STOPPING ) {
				 lock.lock();
				 try {
					 debug.logV(TAG, "BcastTask: waiting for encoding to finish");
					 encoding_ended.await(5, TimeUnit.SECONDS);
				 } catch (InterruptedException e) {
					 debug.logW(TAG, "BcastTask: interrupted waiting for EncodingEnded");
				 }
				 lock.unlock();
			 }

			/* broadcast the remaining ( enc_index > write_sz) */
			while ((enc_index >= write_sz)
					&& (mState == STATUS_FINISHED_ENCODING)) {
				debug.logV(TAG, "BcastTask(): writing remaining: " + enc_index + " state " + mState.toString());
				try {
					enc_buffer.position(0);
					enc_buffer.get(write_buf, 0, write_sz);
					enc_buffer.compact();
					try {
						ChannelBuffer buf =  buffer(write_buf.length);
						buf.writeBytes(write_buf);
						mChannel.write(buf).await(3, TimeUnit.SECONDS);
					} catch (Exception e) {
						debug
								.logE(
										TAG,
										"BcastTask(): can't write remaining into socket",
										e.getCause());
						mState = STATUS_STOPPED;
						sendStatus();
						break;
					}
				} catch (BufferUnderflowException e) {
					debug.logE(TAG, "BcastTask ERROR", e.getCause());
					mState = STATUS_STOPPED;
					sendStatus();
					break;
				}
				enc_index -= write_sz;
			}

			debug.logD(TAG, "BcastTask(): stopping");
			mState = STATUS_STOPPED;
			sendStatus();
			
			/* release all */
			stopRec(true);
			disconnect();

			debug.logD(TAG, "BcastTask(): ending task");

			return null;
		}
	}

	/* check if we have internet connectivity */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
	
	private void setPhoneStateListener() {
		debug.logV(TAG, "setPhoneStateListener");
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if(mgr != null) {
		    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
	}
	
	private void releasePhoneStateListener() {
		debug.logV(TAG, "releasePhoneStateListener");
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if(mgr != null) {
		    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
	}
	
	private void grabWifiLock() {
		if ( wifilock != null && !wifilock.isHeld()) {
			debug.logV(TAG, "grabWifiLock, acquiring");
			wifilock.acquire();
		}
	}
	
	private void releaseWifiLock() {
		if ( wifilock != null && wifilock.isHeld() ) {
			debug.logV(TAG, "releaseWifiLock, releasing");
			wifilock.release();
		}
	}
	
	/*
	 * returns the current Amplitude of the amp_buf buffer, which is an
	 * auxiliary buffer that contains the data hold by the microphone at any
	 * given time.
	 */
	public int getMaxAmplitude() {
		int amplitude = 0;

		if (amp_buf == null) {
//			debug.logD(TAG, "getMaxAmplitude(): amp_buf is null");
			return amplitude;
		}

		if (mState != STATUS_RECORDING) {
//			debug.logD(TAG, "getMaxAmplitude(): rec_task not running");
			return amplitude;
		}

		for (int i = 0; i < amp_buf.length / 2; i += 2) {
			short sample = getShort(amp_buf[i * 2], amp_buf[i * 2 + 1]);
			if (sample > amplitude)
				amplitude = sample;
		}

//		debug.logD(TAG, "getMaxAmplitude(): amplitude " + amplitude);

		return amplitude;
	}

	/* returns a short from 2 bytes */
	private short getShort(byte argB1, byte argB2) {
		return (short) (argB1 | (argB2 << 8));
	}
	
	/* called from the Broadcast object */
	@Override
	public void update(Observable observable, Object data) {
		debug.logV(TAG, "update() called");
		if ( observable instanceof Broadcast ) {
			Broadcast bcast = (Broadcast) observable;
			if ( bcast.isAuthorized()) {				
				mBcastId = bcast.getBcastId();
				debug.logV(TAG, "update() authorized, got bcastId " + mBcastId);
				sendStatus();
			} else {
				debug.logV(TAG, "update() auth failed");
				mBcastId = 0;
				mState = STATUS_STOPPED;
				mError = AUTH_FAILED;
				sendStatus();	
			}
		}
	}
	
	
	private void showNotificationMessage() {
		debug.logV(TAG, "showNotificationMessage called for " + mError);
		
		String tickerText = getText(R.string.connection_lost).toString();
		String contentText = tickerText;
		String contentTitle = "FlipZu: " + getText(R.string.stopped).toString();

		int icon = R.drawable.ic_stat_alert;        // icon from resources
		long when = System.currentTimeMillis();         // notification time
		Context context = getApplicationContext();      // application Context

		Intent notificationIntent = new Intent(this, Recorder.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// the next two lines initialize the Notification, using the configurations above
		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.notify(NOTIF_ID, notification);
	}

}
