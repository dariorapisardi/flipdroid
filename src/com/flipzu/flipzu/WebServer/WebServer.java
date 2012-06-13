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
package com.flipzu.flipzu.WebServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.flipzu.flipzu.FlipzuPlayerService;


public class WebServer {
	
	private static WebServer webServer;
	private static FlipzuHandler handler;
	private static boolean ready = false;
	private static ChannelFactory factory; 

    private WebServer(){
		factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(factory);
        handler = new FlipzuHandler();
        bootstrap.setPipelineFactory(new PipelineFactory(handler));

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        
        bootstrap.bind(new InetSocketAddress(46812));	
        ready = true; 
    } 
    
	public static WebServer startWebServer(FlipzuPlayerService service, String url){
		if(webServer == null)
			webServer = new WebServer();
		handler.setService(service);
		handler.setUrl(url);
		
		return webServer;
	}

	public boolean isReady() {
		return ready;
	}

	@Override
	protected void finalize() throws Throwable {
		factory.releaseExternalResources();
		super.finalize();
	}
}
