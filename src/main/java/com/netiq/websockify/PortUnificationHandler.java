/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.netiq.websockify;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.vnc.bootstrap.ConsoleBootstrap;
import com.infinities.skyport.vnc.handler.PasswordHandler;
import com.infinities.skyport.websockify.SSLSetting;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class PortUnificationHandler extends FrameDecoder {

	protected static long connectionToFirstMessageTimeout = 5000;
	private final ClientSocketChannelFactory cf;
	private final IProxyTargetResolver resolver;
	// private final String keystore;
	// private final String keystorePassword;
	// private final String keystoreKeyPassword;
	// private final String webDirectory;
	private final ServerConfiguration configuration;

	private static final Logger logger = LoggerFactory.getLogger(PortUnificationHandler.class);
	private final ConsoleBootstrap bootstrap;
	private final SSLSetting sslSetting;
	private final org.apache.shiro.mgt.SecurityManager securityManager;
	private Timer msgTimer = null;
	private long directConnectTimerStart = 0;


	private PortUnificationHandler(ClientSocketChannelFactory cf, IProxyTargetResolver resolver, ConsoleBootstrap bootstrap,
			SSLSetting sslSetting, ServerConfiguration configuration, final ChannelHandlerContext ctx,
			org.apache.shiro.mgt.SecurityManager securityManager) {
		this(cf, resolver, bootstrap, sslSetting, configuration, securityManager);
		startDirectConnectionTimer(ctx);
	}

	public PortUnificationHandler(ClientSocketChannelFactory cf, IProxyTargetResolver resolver, ConsoleBootstrap bootstrap,
			SSLSetting sslSetting, ServerConfiguration configuration, org.apache.shiro.mgt.SecurityManager securityManager) {
		this.cf = cf;
		this.resolver = resolver;
		this.bootstrap = bootstrap;
		this.sslSetting = sslSetting;
		this.configuration = configuration;
		this.securityManager = securityManager;
	}

	public static long getConnectionToFirstMessageTimeout() {
		return connectionToFirstMessageTimeout;
	}

	public static void setConnectionToFirstMessageTimeout(long connectionToFirstMessageTimeout) {
		PortUnificationHandler.connectionToFirstMessageTimeout = connectionToFirstMessageTimeout;
	}

	// In cases where there will be a direct VNC proxy connection
	// The client won't send any message because VNC servers talk first
	// So we'll set a timer on the connection - if there's no message by the
	// time
	// the timer fires we'll create the proxy connection to the target
	@Override
	public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		logger.info("receive a request from {}", ctx.getChannel().getRemoteAddress());
		startDirectConnectionTimer(ctx);
	}

	private void startDirectConnectionTimer(final ChannelHandlerContext ctx) {
		// cancel any outstanding timer
		cancelDirectConnectionTimer();

		// direct proxy connection disabled
		if (connectionToFirstMessageTimeout <= 0)
			return;

		directConnectTimerStart = System.currentTimeMillis();

		// cancelling a timer makes it unusable again, so we have to create
		// another one
		msgTimer = new Timer();
		msgTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				switchToDirectProxy(ctx);
			}

		}, connectionToFirstMessageTimeout);

	}

	private void cancelDirectConnectionTimer() {
		if (directConnectTimerStart > 0) {
			long directConnectTimerCancel = System.currentTimeMillis();
			logger.debug("Direct connection timer canceled after {} milliseconds",
					(directConnectTimerCancel - directConnectTimerStart));
		}

		if (msgTimer != null) {
			msgTimer.cancel();
			msgTimer = null;
		}

	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		// Will use the first two bytes to detect a protocol.
		if (buffer.readableBytes() < 2) {
			return null;
		}

		cancelDirectConnectionTimer();

		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);

		if (isSsl(magic1)) {
			enableSsl(ctx);
		} else if (isFlashPolicy(magic1, magic2)) {
			switchToFlashPolicy(ctx);
		} else {
			switchToWebsocketProxy(ctx);
		}

		// Forward the current read buffer as is to the new handlers.
		return buffer.readBytes(buffer.readableBytes());
	}

	private boolean isSsl(int magic1) {
		if (sslSetting != SSLSetting.OFF) {
			switch (magic1) {
			case 20:
			case 21:
			case 22:
			case 23:
			case 255:
				return true;
			default:
				return magic1 >= 128;
			}
		}
		return false;
	}

	private boolean isFlashPolicy(int magic1, int magic2) {
		return (magic1 == '<' && magic2 == 'p');
	}

	private void enableSsl(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();
		try {
			String keystoreType = configuration.getKeystoreType();
			String keystore = configuration.getKeystoreFile();
			String keystorePassword = configuration.getKeystorePass();
			String keystoreKeyPassword = configuration.getKeystoreKeyPass();
			if (keystoreKeyPassword == null || keystoreKeyPassword.isEmpty()) {
				keystoreKeyPassword = keystorePassword;
			}
			logger.info("SSL request from {}.", ctx.getChannel().getRemoteAddress());
			logger.info("keystore: {}, {}, {}, {}", new Object[] { keystoreType, keystore, keystorePassword,
					keystoreKeyPassword });
			SSLEngine engine = WebsockifySslContext
					.getInstance(keystoreType, keystore, keystorePassword, keystoreKeyPassword).getServerContext()
					.createSSLEngine();
			engine.setUseClientMode(false);

			p.addLast("ssl", new SslHandler(engine));
			p.addLast("unificationA", new PortUnificationHandler(cf, resolver, bootstrap, SSLSetting.OFF, configuration,
					ctx, securityManager));
			// p.addLast("unificationA", new PortUnificationHandler(bootstrap,
			// SSLSetting.OFF, ctx, securityManager));
			p.remove(this);
		} catch (Exception e) {
			throw new RuntimeException("Cannot create SSL channel", e);
		}
	}

	private void switchToWebsocketProxy(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();
		logger.info("Websocket proxy request from {}", ctx.getChannel().getRemoteAddress());
		String webDirectory = configuration.getWebDirectory();
		p.addLast("decoder", new HttpRequestDecoder());
		p.addLast("aggregator", new HttpChunkAggregator(65536));
		p.addLast("encoder", new HttpResponseEncoder());
		p.addLast("chunkedWriter", new ChunkedWriteHandler());
		// p.addLast("authhandler", new BasicHttpAuthenticationHandler());

		// TODO remove if use xvpvnc
		ByteBuffer byteBuffer = ByteBuffer.allocate(16);
		p.addLast("passwordhandler", new PasswordHandler(byteBuffer, bootstrap.getProfile().getToken(), bootstrap
				.getProfile().getPassword()));
		p.addLast("handler", new WebsockifyProxyHandler(cf, resolver, webDirectory, bootstrap, byteBuffer));

		p.remove(this);
	}

	private void switchToFlashPolicy(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();

		logger.debug("Flash policy request from {}", ctx.getChannel().getRemoteAddress());

		p.addLast("flash", new FlashPolicyHandler());

		p.remove(this);
	}

	private void switchToDirectProxy(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();

		logger.debug("Direct proxy request from {}", ctx.getChannel().getRemoteAddress());

		p.addLast("proxy", new DirectProxyHandler(ctx.getChannel(), cf, resolver, bootstrap));

		p.remove(this);
	}

	// cancel the timer if channel is closed - prevents useless stack traces
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logger.debug("channel close");
		cancelDirectConnectionTimer();
		bootstrap.close();
	}

	// cancel the timer if exception is caught - prevents useless stack traces
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		cancelDirectConnectionTimer();
		logger.error("Exception on connection", e.getCause());
		bootstrap.close();
	}
}
