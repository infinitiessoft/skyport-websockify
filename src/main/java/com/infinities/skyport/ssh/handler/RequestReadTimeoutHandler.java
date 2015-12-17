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

public class RequestReadTimeoutHandler extends ReadTimeoutHandler {

	private final static Logger logger = LoggerFactory.getLogger(RequestReadTimeoutHandler.class);
	private CustomChannel channel;


	public RequestReadTimeoutHandler(CustomChannel channel, Timer timer, int timeoutSeconds) {
		super(timer, timeoutSeconds, TimeUnit.SECONDS);
		this.channel = channel;
	}

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		logger.debug("timeoutHandler receive message: {}, {}, {}", new Object[] { channel.getRecipient(),
				channel.getSession().isConnected(), channel.isOpen_confirmation() });
		if (!(channel.getReply() == -1 && channel.getSession().isConnected())) {
			channel.setConnected(true);
			ctx.getPipeline().remove(this);
			logger.debug("remove timeout handler");
		}
		super.messageReceived(ctx, e);
	}

	protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
		channel.setReply(0);
		ctx.getPipeline().remove(this);
		logger.debug("remove timeout handler");
		fireExceptionCaught(ctx, new JSchException("channel request: timeout"));
	}

}
