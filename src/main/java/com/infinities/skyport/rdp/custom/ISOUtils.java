package com.infinities.skyport.rdp.custom;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lixia.rdp.HexDump;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;

public class ISOUtils {

	/* this for the ISO Layer */
	private static final Logger logger = LoggerFactory.getLogger(ISOUtils.class);
	// private static final int CONNECTION_REQUEST = 0xE0;
	// private static final int CONNECTION_CONFIRM = 0xD0;
	// private static final int DISCONNECT_REQUEST = 0x80;
	private static final int DATA_TRANSFER = 0xF0;
	// private static final int ERROR = 0x70;
	public static final int DISCONNECT_REQUEST = 0x80;
	private static final int PROTOCOL_VERSION = 0x03;
	private static final int EOT = 0x80;

	private static HexDump dump = new HexDump();


	/**
	 * Receive a data transfer message from the server
	 * 
	 * @return Packet containing message (as ISO PDU)
	 * @throws Exception
	 */
	public static RdpPacket_Localised receive(ChannelHandlerContext context, ChannelBuffer channelBuffer, RDPSession session)
			throws Exception {
		int[] type = new int[1];
		RdpPacket_Localised buffer = receiveMessage(context, type, channelBuffer, session);
		if (buffer == null)
			return null;
		if (type[0] != DATA_TRANSFER) {
			throw new RdesktopException("Expected DT got:" + type[0]);
		}

		return buffer;
	}

	/**
	 * Receive a message from the server
	 * 
	 * @param type
	 *            Array containing message type, stored in type[0]
	 * @return Packet object containing data of message
	 * @throws Exception
	 */
	public static RdpPacket_Localised receiveMessage(ChannelHandlerContext context, int[] type, ChannelBuffer buffer,
			RDPSession session) throws Exception {
		// logger.debug("ISO.receiveMessage");
		RdpPacket_Localised s = null;
		int length, version;
		// logger.debug("buffer:{} ", new Object[] {});
		next_packet: while (true) {
			logger.debug("next_packet");
			s = tcp_recv(null, 4, buffer);
			if (s == null) {
				return null;
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
			s = tcp_recv(s, length - 4, buffer);
			logger.debug("s==null? {}", String.valueOf(s == null));
			if (s == null)
				return null;
			logger.debug("version & 3: {}", String.valueOf(version & 3));
			if ((version & 3) == 0) {
				logger.debug("Processing rdp5 packet");
				RDPUtils.rdp5_process(context, s, (version & 0x80) != 0, session);
				logger.debug("Processing rdp5 packet end");
				continue next_packet;
			} else
				break;
		}

		s.get8();
		type[0] = s.get8();
		logger.debug("type[0]: {}", type[0]);
		if (type[0] == DATA_TRANSFER) {
			logger.debug("Data Transfer Packet");
			s.incrementPosition(1); // eot
			return s;
		}

		s.incrementPosition(5); // dst_ref, src_ref, class
		return s;
	}

	/**
	 * Receive a specified number of bytes from the server, and store in a
	 * packet
	 * 
	 * @param p
	 *            Packet to append data to, null results in a new packet being
	 *            created
	 * @param length
	 *            Length of data to read
	 * @return Packet containing read data, appended to original data if
	 *         provided
	 * @throws IOException
	 */
	private static RdpPacket_Localised tcp_recv(RdpPacket_Localised p, int length, ChannelBuffer input) throws IOException {
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

	/**
	 * Initialise an ISO PDU
	 * 
	 * @param length
	 *            Desired length of PDU
	 * @return Packet configured as ISO PDU, ready to write at higher level
	 */
	public static RdpPacket_Localised init(int length) {
		logger.debug("init length: {}", length);
		RdpPacket_Localised data = new RdpPacket_Localised(length + 7);// getMemory(length+7);
		data.incrementPosition(7);
		data.setStart(data.getPosition());
		return data;
	}

	/**
	 * Send a packet to the server, wrapped in ISO PDU
	 * 
	 * @param buffer
	 *            Packet containing data to send to server
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public static void send(RdpPacket_Localised buffer, ChannelHandlerContext context) throws RdesktopException, IOException {
		if (buffer.getEnd() < 0) {
			throw new RdesktopException("No End Mark!");
		} else {
			int length = buffer.getEnd();
			byte[] packet = new byte[length];
			// RdpPacket data = this.getMemory(length+7);
			buffer.setPosition(0);
			buffer.set8(PROTOCOL_VERSION); // Version
			buffer.set8(0); // reserved
			buffer.setBigEndian16(length); // length of packet

			buffer.set8(2); // length of header
			buffer.set8(DATA_TRANSFER);
			buffer.set8(EOT);
			buffer.copyToByteArray(packet, 0, 0, buffer.getEnd());
			dump.encode(packet, "SEND"/* System.out */);
			// buffer.copyToByteArray(packet, 0, 0, packet.length);
			logger.debug("packet length: {}", length);
			ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(packet);
			DownstreamMessageEvent messageEvent = new DownstreamMessageEvent(context.getChannel(), new DefaultChannelFuture(
					context.getChannel(), false), channelBuffer, context.getChannel().getRemoteAddress());
			// new DownstreamMessageEvent(context.getChannel(), evt.getFuture(),
			// channelBuffer, evt.getChannel().getRemoteAddress());
			context.sendDownstream(messageEvent);
		}
	}

	public static void sendMessage(int type, Channel toRDPChanel) throws IOException {
		RdpPacket_Localised buffer = new RdpPacket_Localised(11);// getMemory(11);
		byte[] packet = new byte[11];

		buffer.set8(PROTOCOL_VERSION); // send Version Info
		buffer.set8(0); // reserved byte
		buffer.setBigEndian16(11); // Length
		buffer.set8(6); // Length of Header

		buffer.set8(type); // where code = CR or DR
		buffer.setBigEndian16(0); // Destination reference ( 0 at CC and DR)

		buffer.setBigEndian16(0); // source reference should be a reasonable
									// address we use 0
		buffer.set8(0); // service class
		buffer.copyToByteArray(packet, 0, 0, packet.length);
		ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(packet);
		// DownstreamMessageEvent messageEvent = new
		// DownstreamMessageEvent(toRDPChanel, new DefaultChannelFuture(
		// toRDPChanel, false), channelBuffer, toRDPChanel.getRemoteAddress());
		// new DownstreamMessageEvent(context.getChannel(), evt.getFuture(),
		// channelBuffer, evt.getChannel().getRemoteAddress());
		toRDPChanel.write(channelBuffer);
		logger.debug("send disconnected");
	}
}
