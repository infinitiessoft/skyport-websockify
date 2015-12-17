package com.infinities.skyport.vnc.handler;

import java.io.Serializable;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyportIdleStateHandler extends IdleStateHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SkyportIdleStateHandler.class);


	public SkyportIdleStateHandler(Timer timer, int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
		super(timer, readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
	}

	@Override
	protected void channelIdle(ChannelHandlerContext ctx, IdleState state, long lastActivityTimeMillis) throws Exception {
		super.channelIdle(ctx, state, lastActivityTimeMillis);
		logger.debug("Acceptor Idle, unbinding {}", state);
		ctx.getChannel().close();
	}

}
