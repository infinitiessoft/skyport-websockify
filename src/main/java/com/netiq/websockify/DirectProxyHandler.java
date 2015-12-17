package com.netiq.websockify;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.vnc.bootstrap.ConsoleBootstrap;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandlerFactory;

public class DirectProxyHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(DirectProxyHandler.class);
	private final ConsoleBootstrap bootstrap;
	// private final ClientSocketChannelFactory cf;
	// private final IProxyTargetResolver resolver;

	// This lock guards against the race condition that overrides the
	// OP_READ flag incorrectly.
	// See the related discussion: http://markmail.org/message/x7jc6mqx6ripynqf
	final Object trafficLock = new Object();

	private volatile Channel outboundChannel;


	public DirectProxyHandler(final Channel inboundChannel, ClientSocketChannelFactory cf, IProxyTargetResolver resolver,
			ConsoleBootstrap bootstrap) {
		this.bootstrap = bootstrap;
		this.outboundChannel = null;
		ensureTargetConnection(inboundChannel, bootstrap.getType(), null);
	}

	private void ensureTargetConnection(final Channel inboundChannel, OutboundWebsocketHandler.Type type,
			final Object sendMsg) {
		if (outboundChannel == null) {
			// Suspend incoming traffic until connected to the remote host.
			inboundChannel.setReadable(false);
			logger.info("Inbound proxy connection from {}", inboundChannel.getRemoteAddress());

			// resolve the target
			final InetSocketAddress target = bootstrap.getTarget();
			if (target == null) {
				logger.error("Connection from {} failed to resolve target.", inboundChannel.getRemoteAddress());
				// there is no target
				inboundChannel.close();
				return;
			}
			// Start the connection attempt.
			// ConsoleBootstrap cb = new ConsoleBootstrap(cf);
			if (type != null) {
				OutboundWebsocketHandler handler = OutboundWebsocketHandlerFactory.getHandler(type, inboundChannel,
						trafficLock);
				bootstrap.getPipeline().addLast("handler", handler);
			} else {
				bootstrap.getPipeline().addLast("handler", new OutboundHandler(inboundChannel, trafficLock));
			}
			ChannelFuture f = bootstrap.connect(inboundChannel, trafficLock, new HashMap<String, List<String>>());

			outboundChannel = f.getChannel();
			if (sendMsg != null)
				outboundChannel.write(sendMsg);
			f.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						logger.info("Created outbound connection to {}", target);
						// Connection attempt succeeded:
						// Begin to accept incoming traffic.
						inboundChannel.setReadable(true);
					} else {
						logger.error("Failed to create outbound connection to {}", target);
						// Close the connection if the connection attempt has
						// failed.
						inboundChannel.close();
					}
				}
			});
		} else {
			if (sendMsg != null)
				outboundChannel.write(sendMsg);
		}
	}

	// In cases where there will be a direct VNC proxy connection
	// The client won't send any message so connect directly on channel open
	@Override
	public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		try {
			// make the proxy connection
			ensureTargetConnection(e.getChannel(), null, null);
		} catch (Exception ex) {
			// target connection failed, so close the client connection
			e.getChannel().close();
			logger.warn("target connection failed", ex);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		handleVncDirect(ctx, (ChannelBuffer) msg, e);
	}

	private void handleVncDirect(ChannelHandlerContext ctx, ChannelBuffer buffer, final MessageEvent e) throws Exception {
		// ensure the target connection is open and send the data
		ensureTargetConnection(e.getChannel(), null, buffer);
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// If inboundChannel is not saturated anymore, continue accepting
		// the incoming traffic from the outboundChannel.
		synchronized (trafficLock) {
			if (e.getChannel().isWritable() && outboundChannel != null) {
				outboundChannel.setReadable(true);
			}
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logger.info("Inbound proxy connection from {} closed", ctx.getChannel().getRemoteAddress());
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		logger.error("Exception on inbound proxy connection", e.getCause().getMessage());
		closeOnFlush(e.getChannel());
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isConnected()) {
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
