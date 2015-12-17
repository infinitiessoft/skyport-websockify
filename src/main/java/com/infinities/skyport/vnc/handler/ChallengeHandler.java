package com.infinities.skyport.vnc.handler;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netiq.websockify.OutboundHandler;

public class ChallengeHandler extends OutboundHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(ChallengeHandler.class);
	private ByteBuffer byteBuffer;


	public ChallengeHandler(Channel inboundChannel, Object trafficLock, ByteBuffer byteBuffer) {
		super(inboundChannel, trafficLock);
		this.byteBuffer = byteBuffer;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		logger.debug("challenge handler pass");
		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		boolean isChallenge = false;
		// Encode the message to base64
		ChannelBuffer orig = buffer.copy();
		logger.debug("orig length {}", orig.array().length);

		if (orig.array().length == 16) {
			byteBuffer.put(orig.array());
			String str = new String(buffer.array());
			logger.debug("challenge received<<< {}", new Object[] { str });
			isChallenge = true;
		}

		if (isChallenge) {
			ctx.getPipeline().remove(this);
		}
		ctx.sendUpstream(e);
	}

}
