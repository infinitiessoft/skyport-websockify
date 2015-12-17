package com.infinities.skyport.websockify.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

public class WebSocketClient {

	private final URI uri;


	public WebSocketClient(URI uri) {
		this.uri = uri;
	}

	public void run() throws Exception {
		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		Channel ch = null;

		try {
			String protocol = uri.getScheme();
			if (!"ws".equals(protocol)) {
				throw new IllegalArgumentException("Unsupported protocol: " + protocol);
			}

			HashMap<String, String> customHeaders = new HashMap<String, String>();
			customHeaders.put("MyHeader", "MyValue");

			// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08
			// or V00.
			// If you change it to V00, ping is not supported and remember to
			// change
			// HttpResponseDecoder to WebSocketHttpResponseDecoder in the
			// pipeline.
			final WebSocketClientHandshaker handshaker = new WebSocketClientHandshakerFactory().newHandshaker(uri,
					WebSocketVersion.V13, null, false, customHeaders);

			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline pipeline = Channels.pipeline();

					pipeline.addLast("decoder", new HttpResponseDecoder());
					pipeline.addLast("encoder", new HttpRequestEncoder());
					pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker));
					return pipeline;
				}
			});

			// Connect
			System.out.println("WebSocket Client connecting");
			ChannelFuture future = bootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
			future.syncUninterruptibly();

			ch = future.getChannel();
			handshaker.handshake(ch).syncUninterruptibly();

			// Send 10 messages and wait for responses
			System.out.println("WebSocket Client sending message");
			for (int i = 0; i < 1000; i++) {
				// ch.write(new TextWebSocketFrame("Message #" + i));
			}

			// Ping
			System.out.println("WebSocket Client sending ping");
			// ch.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new
			// byte[] { 1, 2, 3, 4, 5, 6 })));

			// Close
			System.out.println("WebSocket Client sending close");
			// ch.write(new CloseWebSocketFrame());

			// WebSocketClientHandler will close the connection when the server
			// responds to the CloseWebSocketFrame.
			ch.getCloseFuture().awaitUninterruptibly();
		} finally {
			if (ch != null) {
				ch.close();
			}
			bootstrap.releaseExternalResources();
		}
	}

	public static void main(String[] args) throws Exception {
		URI uri;
		if (args.length > 0) {
			uri = new URI(args[0]);
		} else {
			uri = new URI("ws://localhost:10900");
		}
		new WebSocketClient(uri).run();
	}
}
