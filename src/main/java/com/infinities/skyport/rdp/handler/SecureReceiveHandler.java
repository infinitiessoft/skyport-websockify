package com.infinities.skyport.rdp.handler;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.Message;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.SecureUtils;
import com.lixia.rdp.Constants;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

public class SecureReceiveHandler extends OneToOneDecoder {

	private static final int SEC_ENCRYPT = 0x0008;
	private static final int SEC_LICENCE_NEG = 0x0080;
	private static final Logger logger = LoggerFactory.getLogger(SecureReceiveHandler.class);
	private RDPSession session;


	public SecureReceiveHandler(RDPSession session) {
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			return receive(ctx, (Message) msg);
		} else {
			return msg;
		}
	}

	private Message receive(ChannelHandlerContext ctx, Message msg) throws RdesktopException, CryptoException, IOException {
		int sec_flags = 0;
		RdpPacket_Localised buffer = null;
		// logger.debug("channelBuffer readableBytes:{}",
		// channelBuffer.readableBytes());

		while (true) {
			int channel = msg.getChannel();
			logger.debug("channel: {}", channel);
			buffer = msg.getPacket();
			if (buffer == null) {
				return msg;
			}
			buffer.setHeader(RdpPacket_Localised.SECURE_HEADER);
			if (Constants.encryption || (!session.isLicenceIssued())) {

				sec_flags = buffer.getLittleEndian32();
				logger.debug("sec_flags:{}", sec_flags);
				logger.debug("sec_flags & SEC_LICENCE_NEG: {}", sec_flags & SEC_LICENCE_NEG);
				if ((sec_flags & SEC_LICENCE_NEG) != 0) {
					session.getLicence().process(buffer, ctx);
					logger.debug("continue loop");
					return null;
				}
				logger.debug("sec_flags & SEC_ENCRYPT: {}", sec_flags & SEC_ENCRYPT);
				if ((sec_flags & SEC_ENCRYPT) != 0) {
					buffer.incrementPosition(8); // signature
					byte[] data = new byte[buffer.size() - buffer.getPosition()];
					buffer.copyToByteArray(data, 0, buffer.getPosition(), data.length);
					byte[] packet = SecureUtils.decrypt(data, session);

					buffer.copyFromByteArray(packet, 0, buffer.getPosition(), packet.length);
					// buffer.setStart(buffer.getPosition());
					// return buffer;
				}
			}
			logger.debug("channel[0]: {}", channel);
			if (channel != MCSUtils.MCS_GLOBAL_CHANNEL) {
				session.getChannels().channel_process(buffer, channel, session, ctx);
				logger.debug("continue loop2");
				return null;
			}

			buffer.setStart(buffer.getPosition());
			return msg;
		}
	}

}
