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
*  		Nicolás Gschwing <nicolas@gschwind.com.ar>
*/
package com.flipzu.flipzu.WebServer;
import java.io.InputStream;
import java.net.URL;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.stream.ChunkedStream;

import android.os.AsyncTask;
import android.util.Log;

import com.flipzu.flipzu.FlipzuPlayerService;
import com.flipzu.flipzu.R;


public class FlipzuHandler extends SimpleChannelHandler{
	
	private FlipzuPlayerService service;
	private String mUrl;
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception { 
		
		Log.e("Netty", e.getMessage().toString());
		
		Channel ch = e.getChannel();
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		
		InputStream is = this.service.getResources().openRawResource(R.raw.silence);
		byte[] buf = new byte[4096];
		int result = is.read(buf);
		while(result != -1){
			buffer.writeBytes(buf);
			result = is.read(buf);
		}

		ch.write(buffer);

/*    	ChunkedStream cs = new ChunkedStream(this.service.getResources().openRawResource(R.raw.output));
 
		while (cs.hasNextChunk() && ch.isWritable()){
			ch.write(cs.nextChunk());
		}
*/
		int rand = (int) (Math.random() * 10000);
		String eurl = mUrl + "?rand=" + rand;
		URL url = new URL (eurl);
		
		Log.e("Netty", "Silencio completado... cargando " + url.toExternalForm());
		
		new PlayTask().execute(url, ch);
		
		ctx.sendUpstream(e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		//super.exceptionCaught(ctx, e);
		e.getFuture().addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
	}
	
	public void setService(FlipzuPlayerService service) {
		this.service = service;
	}

	public void setUrl(String url) {
		this.mUrl = url;
	}
	
	private class PlayTask extends AsyncTask<Object, Void, Void>{
		
		@Override
		protected Void doInBackground(Object... params) {
			URL url = (URL) params[0];
			Channel ch = (Channel) params[1];
			ChannelFuture f = null;
	    	ChunkedStream cs2;
			try {
				cs2 = new ChunkedStream(url.openStream());
				while (cs2.hasNextChunk()  && ch.isWritable()){
					f = ch.write(cs2.nextChunk());
				}
				if(f != null)
					f.addListener(ChannelFutureListener.CLOSE);
			} catch (Exception e) {
				ch.close();
				e.printStackTrace();
			}
			return null;
		}
	}

}
