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
package com.infinities.skyport.websockify.outbound;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class OutboundBinaryWebsocketHandler extends OutboundWebsocketHandler {

	// private static final Logger logger =
	// LoggerFactory.getLogger(BinaryOutboundWebsocketHandler.class);

	public OutboundBinaryWebsocketHandler(Channel inboundChannel, Object trafficLock) {
		super(inboundChannel, trafficLock);
	}

	@Override
	protected Object processMessage(ChannelBuffer buffer) {
		BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
		return frame;
	}
}