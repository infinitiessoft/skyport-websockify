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
