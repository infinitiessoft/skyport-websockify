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