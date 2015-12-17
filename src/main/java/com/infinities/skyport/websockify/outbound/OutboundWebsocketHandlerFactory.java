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

import org.jboss.netty.channel.Channel;

import com.infinities.skyport.exception.SkyportException;

public class OutboundWebsocketHandlerFactory {

	private OutboundWebsocketHandlerFactory() {

	}

	public final static OutboundWebsocketHandler getHandler(OutboundWebsocketHandler.Type type, Channel inboundChannel,
			Object trafficLock) {

		switch (type) {
		case Binary:
			return new OutboundBinaryWebsocketHandler(inboundChannel, trafficLock);
		case Base64Text:
			return new OutboundTextWebsocketHandler(inboundChannel, trafficLock);
		default:
			throw new SkyportException("unimplemented OutboundWebsocketHandler.Type:" + type);

		}
	}

}
