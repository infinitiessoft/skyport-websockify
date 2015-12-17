package com.infinities.skyport.rdp.handler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.RDPSession;

//ISO.connect
//MCS.connect.sendConnectInitial
public class SendCjrq2Handler extends FrameDecoder {

	// private static final Logger logger =
	// LoggerFactory.getLogger(SendCjrqHandler.class);
	private RDPSession session;


	public SendCjrq2Handler(RDPSession session) {
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		MCSUtils.receive_cjcf(ctx, buffer, session);
		MCSUtils.send_cjrq(MCSUtils.MCS_GLOBAL_CHANNEL, session.getMcsUserID(), ctx);
		ctx.getPipeline().remove(this);
		return null;
	}
}
