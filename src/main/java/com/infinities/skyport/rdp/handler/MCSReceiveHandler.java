package com.infinities.skyport.rdp.handler;

import java.io.EOFException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.Message;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;

public class MCSReceiveHandler extends OneToOneDecoder {

	private static final int SDIN = 26; /* Send Data Indication */
	private static final int DPUM = 8; /* Disconnect Provider Ultimatum */
	private static final Logger logger = LoggerFactory.getLogger(MCSReceiveHandler.class);


	public MCSReceiveHandler() {
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			return receive((Message) msg);
		} else {
			return msg;
		}
	}

	private Message receive(Message msg) throws RdesktopException, EOFException {
		logger.debug("receive");
		int opcode = 0, appid = 0, length = 0;
		RdpPacket_Localised buffer = msg.getPacket();
		if (buffer == null)
			return msg;
		buffer.setHeader(RdpPacket_Localised.MCS_HEADER);
		opcode = buffer.get8();

		appid = opcode >> 2;

		if (appid != SDIN) {
			if (appid != DPUM) {
				throw new RdesktopException("Expected data got" + opcode);
			}
			throw new EOFException("End of transmission!");
		}

		buffer.incrementPosition(2); // Skip UserID
		int channel = buffer.getBigEndian16(); // Get ChannelID
		msg.setChannel(channel);
		// logger.debug("Channel ID = " + channel[0]);
		buffer.incrementPosition(1); // Skip Flags

		length = buffer.get8();

		if ((length & 0x80) != 0) {
			buffer.incrementPosition(1);
		}
		buffer.setStart(buffer.getPosition());

		return msg;
	}

}
