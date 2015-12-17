package com.infinities.skyport.rdp.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.RDPSession;

//ISO.connect
//MCS.connect.sendConnectInitial
public class SendCjrqHandler extends FrameDecoder {

	private static final Logger logger = LoggerFactory.getLogger(SendCjrqHandler.class);
	private RDPSession session;


	public SendCjrqHandler(RDPSession session) {
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		session.setMcsUserID(MCSUtils.receive_aucf(ctx, buffer, session));
		logger.debug("mcs id:{}", session.getMcsUserID());
		MCSUtils.send_cjrq(session.getMcsUserID() + MCSUtils.MCS_USERCHANNEL_BASE, session.getMcsUserID(), ctx);
		ctx.getPipeline().remove(this);
		return null;
	}
}
