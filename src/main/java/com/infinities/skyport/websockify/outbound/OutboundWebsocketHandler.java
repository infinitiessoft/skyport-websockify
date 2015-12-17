package com.infinities.skyport.websockify.outbound;

import org.jboss.netty.channel.Channel;

import com.netiq.websockify.OutboundHandler;

public abstract class OutboundWebsocketHandler extends OutboundHandler {

	public enum Type {
		Base64Text, Binary
	}


	protected OutboundWebsocketHandler(Channel inboundChannel, Object trafficLock) {
		super(inboundChannel, trafficLock);
	}

}
