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

import java.util.regex.PatternSyntaxException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class FlipzuRecorderHandler  extends SimpleChannelUpstreamHandler {
	private Debug debug = new Debug();
	private static final String TAG = "FlipzuRecorderHandler";
	
    @Override
    public void channelConnected ( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    	debug.logV(TAG, "channelConnected()");
    	
    	ctx.sendUpstream(e);
    }
	
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    	String resp = (String) e.getMessage();
    	
    	debug.logV(TAG, "messageReceived(), got " + resp);
    	
    	if ( resp.startsWith("AUTH OK")) {
    		ctx.getPipeline().remove("string-encoder");
			String[] split = null;
			Integer bcastId = 0;
			try {
				split = resp.split(" ");
			} catch (PatternSyntaxException err) {
				debug.logE(TAG, "messageReceived error", err.getCause());
			}
			
			if (split.length == 3) {
				bcastId = Integer.parseInt(split[2].trim());
			}
    		Broadcast.getInstance().setAuthorized(true);
    		Broadcast.getInstance().setBcastId(bcastId);
    	} else {
    		debug.logV(TAG, "Unauthorized, closing channel");
    		Broadcast.getInstance().setAuthorized(false);
    		e.getChannel().close();
    	}
    	
    	ctx.sendUpstream(e);
    }
    
    @Override
    public void channelClosed (ChannelHandlerContext ctx, ChannelStateEvent e) {
    	debug.logV(TAG, "channelClosed()");
    	
    	ctx.sendUpstream(e);
    }
    
    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, ExceptionEvent e) {
            debug.logE(TAG, "exceptionCaught", e.getCause());
    }
}
