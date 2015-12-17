/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.vnc.bootstrap;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.vnc.handler.SslHandler;
import com.infinities.skyport.vnc.handler.XvpVncAuthHandler;
import com.infinities.skyport.vnc.util.SSLHelper;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler.Type;

public class XvpVncBootstrap extends AbstractBootstrap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(XvpVncBootstrap.class);

	private Channel inboundChannel;
	private WebsockifyProfile profile;
	private InetSocketAddress remoteAddress;
	private String token;
	private Object trafficLock;
	private SslHandler handler;


	public XvpVncBootstrap(WebsockifyProfile profile, ChannelFactory channelFactory) {
		super(channelFactory);
		this.profile = profile;
		this.token = profile.getPassword();
		remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
	}

	@Override
	public ChannelFuture connect() {
		SocketAddress remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());

		getPipeline().addBefore("handler", "xvphandler", new XvpVncAuthHandler(inboundChannel, trafficLock));
		logger.debug("is internalSSL? {}", profile.isInternalSSL());
		if (profile.isInternalSSL()) {
			handler = SSLHelper.getInstance().getTrustAllSSLHandler();
			// getPipeline().remove("challengehandler");
			getPipeline().replace(getPipeline().get("challengehandler"), "ignoreTwoByteMessageHandler",
					new IgnoreTwoByteMessageHandler());
			getPipeline().addFirst("ssl", handler);
		} else {
			getPipeline().remove("challengehandler");
		}
		inboundChannel.getPipeline().remove("passwordhandler");

		// getPipeline().addFirst("logger", new LogHandler());
		// getPipeline().addFirst("encoder", new StringEncoder());
		// getPipeline().addFirst("decoder", new StringDecoder());
		final ChannelFuture future = connect(remoteAddress);
		future.addListener(createChannelFutureListener(remoteAddress, future.getChannel()));

		return future;
	}

	private ChannelFutureListener createChannelFutureListener(final SocketAddress target, final Channel outboundChannel) {
		checkNotNull(inboundChannel);
		checkNotNull(token);

		return new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					// Connection attempt succeeded:
					// Begin to accept incoming traffic.
					logger.info("connection to {} succeeded", target);
					if (handler != null) {
						logger.info("Your session is protected by {}" + " cipher suite.\n", handler.getEngine().getSession()
								.getCipherSuite());

						ChannelFuture handshakeFuture = handler.handshake();
						handshakeFuture.addListener(new Greeter(token));
					} else {
						String line = token;// + "\r\n";
						logger.debug("token=" + line);
						ChannelBuffer msgdata = new BigEndianHeapChannelBuffer(line.getBytes());
						future.getChannel().write(msgdata);
					}
					inboundChannel.setReadable(true);
				} else {
					logger.error("Failed to create outbound connection to {}", target);
					// Close the connection if the connection attempt has
					// failed.
					inboundChannel.close();
				}
			}
		};
	}

	public Channel getInboundChannel() {
		return inboundChannel;
	}

	public void setInboundChannel(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Object getTrafficLock() {
		return trafficLock;
	}

	public void setTrafficLock(Object trafficLock) {
		this.trafficLock = trafficLock;
	}

	@Override
	public ChannelFuture connect(Channel inboundChannel, Object trafficLock, Map<String, List<String>> queryParams) {
		this.inboundChannel = inboundChannel;
		this.trafficLock = trafficLock;

		return connect();
	}

	@Override
	public InetSocketAddress getTarget() {
		return remoteAddress;
	}

	@Override
	public WebsockifyProfile getProfile() {
		return profile;
	}


	private static final class IgnoreTwoByteMessageHandler extends SimpleChannelUpstreamHandler {

		@Override
		public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
			logger.debug("IgnoreTwoByteMessageHandler handler pass");
			ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
			ChannelBuffer orig = buffer.copy();
			logger.debug("orig length {}", orig.array().length);

			if (orig.array().length >= 12 && orig.array().length < 70) {
				ctx.sendUpstream(e);
				ctx.getPipeline().remove(this);
				return;
			} else if (orig.array().length >= 70) {
				ctx.sendUpstream(e);
				return;
			} else if (orig.array().length < 12) {
				// ignore
				return;
			}
		}
	}

	private static final class Greeter implements ChannelFutureListener {

		private String token;


		private Greeter(String token) {
			this.token = token;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				String line = token;// + "\r\n";
				logger.debug("token=" + line);
				ChannelBuffer msgdata = new BigEndianHeapChannelBuffer(line.getBytes());
				future.getChannel().write(msgdata);

			} else {
				future.getChannel().close();
			}
		}
	}


	@Override
	public Type getType() {
		return Type.Base64Text;
	}

}
