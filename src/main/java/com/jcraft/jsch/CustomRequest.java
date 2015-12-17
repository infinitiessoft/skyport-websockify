package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.ssh.handler.RequestReadTimeoutHandler;

public abstract class CustomRequest {

	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(CustomRequest.class);
	private boolean reply = false;
	private CustomSession session = null;
	private CustomChannel channel = null;


	public void request(CustomSession session, CustomChannel channel, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		this.session = session;
		this.channel = channel;
		if (channel.connectTimeout > 0) {
			setReply(true);
		}
	}

	public boolean waitForReply() {
		return reply;
	}

	public void setReply(boolean reply) {
		this.reply = reply;
	}

	public void write(String handlername, CustomPacket packet, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		session.write(packet, context, evt);
		RequestReadTimeoutHandler timeoutHandler = new RequestReadTimeoutHandler(channel, new HashedWheelTimer(), (int) 3);
		logger.debug("add timeouthandler");
		context.getPipeline().addFirst(handlername, timeoutHandler);
	}
}
