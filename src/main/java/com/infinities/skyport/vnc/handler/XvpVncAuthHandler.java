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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netiq.websockify.OutboundHandler;

public class XvpVncAuthHandler extends OutboundHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(XvpVncAuthHandler.class);
	private static String HTTP_200 = "200 OK";


	// private static String HTTP11_200 = "HTTP/1.1 200";
	// private static String HTTP10_200 = "HTTP/1.0";

	public XvpVncAuthHandler(Channel inboundChannel, Object trafficLock) {
		super(inboundChannel, trafficLock);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		logger.debug("XvpVncAuthHandler pass");
		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		// Encode the message to base64
		String str = new String(buffer.array());
		if (str.contains(HTTP_200)) {
			logger.debug("get Xvp authentication OK information");
			ctx.getPipeline().remove(this);
		} else {
			ctx.sendUpstream(e);
		}
	}
}
