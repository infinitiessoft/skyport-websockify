package com.infinities.skyport.rdp.handler;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.Message;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.RDPUtils;
import com.infinities.skyport.rdp.handler.ISOReceiveMessageHandler.DecoderState;
import com.lixia.rdp.HexDump;
import com.lixia.rdp.RdpPacket_Localised;

public class ISOReceiveMessageHandler extends ReplayingDecoder<DecoderState> {

	private static final int DATA_TRANSFER = 0xF0;
	private static final Logger logger = LoggerFactory.getLogger(ISOReceiveMessageHandler.class);
	private RdpPacket_Localised s = null;
	private int length, version;
	private RDPSession session;

	private HexDump dump = null;


	public enum DecoderState {
		READ_LENGTH, READ_CONTENT;
	}


	public ISOReceiveMessageHandler(RDPSession session) {
		super(DecoderState.READ_LENGTH);
		this.session = session;
		dump = new HexDump();
	}

	@Override
	protected Object decode(ChannelHandlerContext context, Channel channel, ChannelBuffer buffer, DecoderState state)
			throws Exception {
		next_packet: while (true) {
			switch (state) {
			case READ_LENGTH:
				logger.debug("next_packet");
				s = tcp_recv(null, 4, buffer);
				checkpoint(DecoderState.READ_CONTENT);
				if (s == null) {
					return new Message();
				}
				version = s.get8();
				logger.debug("version: {}", version);
				if (version == 3) {
					s.incrementPosition(1); // pad
					length = s.getBigEndian16();
					logger.debug("length: {}", length);
				} else {
					length = s.get8();
					if ((length & 0x80) != 0) {
						length &= ~0x80;
						length = (length << 8) + s.get8();
					}
					logger.debug("length(version != 3): {}", length);
				}
				logger.debug("read again");

			case READ_CONTENT:
				s = tcp_recv(s, length - 4, buffer);
				checkpoint(DecoderState.READ_LENGTH);
				logger.debug("s==null? {}", String.valueOf(s == null));
				if (s == null)
					return new Message();
				logger.debug("version & 3: {}", String.valueOf(version & 3));
				if ((version & 3) == 0) {
					logger.debug("Processing rdp5 packet");
					RDPUtils.rdp5_process(context, s, (version & 0x80) != 0, session);
					logger.debug("Processing rdp5 packet end");
					state = DecoderState.READ_LENGTH;
					continue next_packet;
				} else
					break next_packet;
			default:
				throw new Error("Should't reach here.");
			}
		}

		s.get8();
		int type = s.get8();
		logger.debug("type[0]: {}", type);
		Message message = new Message();
		message.setType(type);
		message.setPacket(s);
		if (type == DATA_TRANSFER) {
			logger.debug("Data Transfer Packet");
			s.incrementPosition(1); // eot
			logger.debug("readable byte:{}", super.actualReadableBytes());
			return message;
		}

		s.incrementPosition(5); // dst_ref, src_ref, class
		logger.debug("readable byte:{}", super.actualReadableBytes());
		return message;
	}

	private RdpPacket_Localised tcp_recv(RdpPacket_Localised p, int length, ChannelBuffer input) throws IOException {
		// logger.debug("ISO.tcp_recv");
		RdpPacket_Localised buffer = null;

		byte[] packet = new byte[length];

		// in.readFully(packet, 0, length);
		logger.debug("buffer size: {}", input.readableBytes());
		logger.debug("read byte length: {}", length);

		input.readBytes(packet, 0, length);
		dump.encode(packet, "RECEIVE" /* System.out */);
		// try{ }
		// catch(IOException e){ logger.warn("IOException: " + e.getMessage());
		// return null; }

		if (p == null) {
			logger.debug("p==null");
			buffer = new RdpPacket_Localised(length);
			buffer.copyFromByteArray(packet, 0, 0, packet.length);
			buffer.markEnd(length);
			buffer.setStart(buffer.getPosition());
		} else {
			logger.debug("p!=null");
			buffer = new RdpPacket_Localised((p.getEnd() - p.getStart()) + length);
			buffer.copyFromPacket(p, p.getStart(), 0, p.getEnd());
			buffer.copyFromByteArray(packet, 0, p.getEnd(), packet.length);
			buffer.markEnd(p.size() + packet.length);
			buffer.setPosition(p.getPosition());
			buffer.setStart(0);
		}

		return buffer;
	}

}
