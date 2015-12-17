package com.infinities.skyport.websockify.outbound;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class OutboundTextWebsocketHandler extends OutboundWebsocketHandler {

	// private static final Logger logger =
	// LoggerFactory.getLogger(OutboundWebsocketHandler.class);

	public OutboundTextWebsocketHandler(Channel inboundChannel, Object trafficLock) {
		super(inboundChannel, trafficLock);
	}

	@Override
	protected Object processMessage(ChannelBuffer buffer) {
		ChannelBuffer base64Msg = Base64.encode(buffer, false);
		TextWebSocketFrame frame = new TextWebSocketFrame(base64Msg);
		return frame;
	}
}