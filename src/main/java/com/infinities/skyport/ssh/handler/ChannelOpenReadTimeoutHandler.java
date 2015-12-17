package com.infinities.skyport.ssh.handler;

import static org.jboss.netty.channel.Channels.fireExceptionCaught;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.CustomChannel;
import com.jcraft.jsch.JSchException;

public class ChannelOpenReadTimeoutHandler extends ReadTimeoutHandler {

	private final static Logger logger = LoggerFactory.getLogger(ChannelOpenReadTimeoutHandler.class);
	private CustomChannel channel;


	public ChannelOpenReadTimeoutHandler(CustomChannel channel, Timer timer, int timeoutSeconds) {
		super(timer, timeoutSeconds, TimeUnit.SECONDS);
		this.channel = channel;
	}

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!(channel.getRecipient() == -1 && channel.getSession().isConnected())) {
			channel.setConnected(true);
			ctx.getPipeline().remove(this);
			logger.debug("remove timeout handler");
		}
		super.messageReceived(ctx, e);
	}

	protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
		Exception e = null;
		if (!channel.getSession().isConnected()) {
			e = new JSchException("session is down");
		}
		if (channel.getRecipient() == -1) { // timeout
			e = new JSchException("channel is not opened.");
		}
		if (channel.isOpen_confirmation() == false) { // SSH_MSG_CHANNEL_OPEN_FAILURE
			e = new JSchException("channel is not opened.");
		}

		ctx.getPipeline().remove(this);
		logger.debug("remove timeout handler");
		if (e != null) {
			fireExceptionCaught(ctx, e);
		}
	}

}
