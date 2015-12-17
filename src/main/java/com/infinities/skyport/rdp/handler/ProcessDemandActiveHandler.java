package com.infinities.skyport.rdp.handler;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.Message;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.RDPUtils;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.crypto.CryptoException;

//ISO.connect
//MCS.connect.sendConnectInitial
public class ProcessDemandActiveHandler extends OneToOneDecoder {

	private static final Logger logger = LoggerFactory.getLogger(ProcessDemandActiveHandler.class);
	private static final int RDP_INPUT_SYNCHRONIZE = 0;
	private RDPSession session;
	// private RdpPacket_Localised stream = null;
	// private int next_packet = 0;
	// private int type[] = new int[1];
	private String[] EXPECTED = new String[] { "RDP_PDU_SYNCHRONIZE", "RDP_CTL_COOPERATE", "RDP_CTL_GRANT_CONTROL" };
	private int index = 0;


	// private boolean sendSync = false;

	public ProcessDemandActiveHandler(RDPSession session) {
		logger.debug("add ProcessDemandActiveHandler");
		this.session = session;
		// this.stream = stream;
		// this.next_packet = next_packet;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			return processDemandActive(ctx, (Message) msg);
		} else {
			return msg;
		}
	}

	private Object processDemandActive(ChannelHandlerContext ctx, Message msg) throws RdesktopException, IOException,
			CryptoException {
		index++;
		logger.debug("handle msg index:{}", index);
		if (index == EXPECTED.length) {
			logger.debug("send input synchronize");
			RDPUtils.sendInput(0, RDP_INPUT_SYNCHRONIZE, 0, 0, 0, session, ctx);
			RDPUtils.sendFonts(1, session, ctx);
			RDPUtils.sendFonts(2, session, ctx);
//			session.getOrders().resetOrderState();
			
		} else if (index > EXPECTED.length) {
			ctx.getPipeline().remove(this);
			if (ctx.getPipeline().get("inputLocalisedHandler") == null) {
				ctx.getPipeline().addLast("inputLocalisedHandler", new InputLocalisedHandler(session));
			}
			this.session.getOrders().resetOrderState();
			logger.debug("ready to send (got past licence negotiation)");
		}

		return null;
	}

}
