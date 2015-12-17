package com.infinities.skyport.vnc.handler;

import java.io.Serializable;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger logger = LoggerFactory.getLogger(LogHandler.class);


	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("loghandler down pass: {}, {}", new Object[] { evt.getClass(), evt.toString() });

		if (!(evt instanceof MessageEvent)) {
			context.sendDownstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (!(e.getMessage() instanceof ChannelBuffer)) {
			logger.debug("{}, {}", new Object[] { e.getMessage().getClass(), e.getMessage().toString() });
			context.sendDownstream(evt);
			return;
		}

		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		logger.debug("handledownstream: {}", new Object[] { new String(msg.array()) });
		context.sendDownstream(evt);
	}


	boolean noyet = true;


	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.debug("loghandler up pass: {}, {}", new Object[] { evt.getClass(), evt.toString() });

		if (!(evt instanceof MessageEvent)) {
			context.sendUpstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (!(e.getMessage() instanceof ChannelBuffer)) {
			logger.debug("{}, {}", new Object[] { e.getMessage().getClass(), e.getMessage().toString() });
			context.sendUpstream(evt);
			return;
		}

		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		logger.debug("handleupstream: {}", new Object[] { new String(msg.array()) });
		context.sendUpstream(evt);
	}
}
