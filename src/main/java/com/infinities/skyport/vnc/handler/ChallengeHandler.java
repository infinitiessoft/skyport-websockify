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
