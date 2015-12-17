package com.infinities.skyport.rdp.handler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.infinities.skyport.rdp.custom.Message;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;

public class ISOReceiveHandler extends OneToOneDecoder {

	private static final int DATA_TRANSFER = 0xF0;


	// private static final Logger logger =
	// LoggerFactory.getLogger(ISOReceiveHandler.class);

	public ISOReceiveHandler() {
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			return receive((Message) msg);
		} else {
			return msg;
		}
	}

	private Message receive(Message msg) throws RdesktopException {
		int type = msg.getType();
		RdpPacket_Localised buffer = msg.getPacket();
		if (buffer == null)
			return new Message();
		if (type != DATA_TRANSFER) {
			throw new RdesktopException("Expected DT got:" + type);
		}

		return msg;
	}

}
