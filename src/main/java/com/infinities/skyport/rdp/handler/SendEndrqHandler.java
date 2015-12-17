package com.infinities.skyport.rdp.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.lixia.rdp.RdpPacket_Localised;

//ISO.connect
//MCS.connect.sendConnectInitial
public class SendEndrqHandler extends FrameDecoder {

	private static final Logger logger = LoggerFactory.getLogger(SendEndrqHandler.class);
	private RdpPacket_Localised data;
	private RDPSession session;


	public SendEndrqHandler(RdpPacket_Localised data, RDPSession session) {
		this.data = data;
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		MCSUtils.receiveConnectResponse(ctx, data, buffer, session);
		logger.debug("connect response received");
		MCSUtils.send_edrq(ctx);
		MCSUtils.send_aurq(ctx);
		ctx.getPipeline().remove(this);
		return null;
	}
}
