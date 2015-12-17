package com.infinities.skyport.rdp.custom;

import java.io.EOFException;
import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;

public class MCSUtils {

	private static final Logger logger = LoggerFactory.getLogger(MCSUtils.class);
	// private static final int CONNECT_INITIAL = 0x7f65;
	private static final int CONNECT_RESPONSE = 0x7f66;

	// private static final int BER_TAG_BOOLEAN = 1;
	private static final int BER_TAG_INTEGER = 2;
	private static final int BER_TAG_OCTET_STRING = 4;
	private static final int BER_TAG_RESULT = 10;
	private static final int TAG_DOMAIN_PARAMS = 0x30;

	public static final int MCS_GLOBAL_CHANNEL = 1003;
	public static final int MCS_USERCHANNEL_BASE = 1001;

	private static final int EDRQ = 1; /* Erect Domain Request */
	private static final int DPUM = 8; /* Disconnect Provider Ultimatum */
	private static final int AURQ = 10; /* Attach User Request */
	private static final int AUCF = 11; /* Attach User Confirm */
	private static final int CJRQ = 14; /* Channel Join Request */
	private static final int CJCF = 15; /* Channel Join Confirm */
	private static final int SDRQ = 25; /* Send Data Request */
	private static final int SDIN = 26; /* Send Data Indication */


	/**
	 * Receive and handle a connect response from the server
	 * 
	 * @param data
	 *            Packet containing response data
	 * @throws Exception
	 */
	public static void receiveConnectResponse(ChannelHandlerContext context, RdpPacket_Localised data,
			ChannelBuffer channelBuffer, RDPSession session) throws Exception {
		logger.debug("MCS.receiveConnectResponse");

		String[] connect_results = { "Successful", "Domain Merging", "Domain not Hierarchical", "No Such Channel",
				"No Such Domain", "No Such User", "Not Admitted", "Other User ID", "Parameters Unacceptable",
				"Token Not Available", "Token Not Possessed", "Too Many Channels", "Too Many Tokens", "Too Many Users",
				"Unspecified Failure", "User Rejected" };

		int result = 0;
		int length = 0;

		RdpPacket_Localised buffer = ISOUtils.receive(context, channelBuffer, session);
		logger.debug("Received buffer");
		length = berParseHeader(buffer, CONNECT_RESPONSE);
		length = berParseHeader(buffer, BER_TAG_RESULT);

		result = buffer.get8();
		if (result != 0) {
			throw new RdesktopException("MCS Connect failed: " + connect_results[result]);
		}
		length = berParseHeader(buffer, BER_TAG_INTEGER);
		length = buffer.get8(); // connect id
		parseDomainParams(buffer);
		length = berParseHeader(buffer, BER_TAG_OCTET_STRING);

		SecureUtils.processMcsData(buffer, session);
		logger.debug("receive connect response length: {}", length);
	}

	/**
	 * Parse a BER header and determine data length
	 * 
	 * @param data
	 *            Packet containing header at current read position
	 * @param tagval
	 *            Tag ID for data type
	 * @return Length of following data
	 * @throws RdesktopException
	 */
	public static int berParseHeader(RdpPacket_Localised data, int tagval) throws RdesktopException {
		int tag = 0;
		int length = 0;
		int len;

		if (tagval > 0x000000ff) {
			tag = data.getBigEndian16();
		} else {
			tag = data.get8();
		}

		if (tag != tagval) {
			throw new RdesktopException("Unexpected tag got " + tag + " expected " + tagval);
		}

		len = data.get8();

		if ((len & 0x00000080) != 0) {
			len &= ~0x00000080; // subtract 128
			length = 0;
			while (len-- != 0) {
				length = (length << 8) + data.get8();
			}
		} else {
			length = len;
		}

		return length;
	}

	/**
	 * Parse domain parameters sent by server
	 * 
	 * @param data
	 *            Packet containing domain parameters at current read position
	 * @throws RdesktopException
	 */
	public static void parseDomainParams(RdpPacket_Localised data) throws RdesktopException {
		int length;

		length = berParseHeader(data, TAG_DOMAIN_PARAMS);
		data.incrementPosition(length);

		if (data.getPosition() > data.getEnd()) {
			throw new RdesktopException();
		}
	}

	/**
	 * Transmit an EDrq message
	 * 
	 * @throws IOException
	 * @throws RdesktopException
	 */
	public static void send_edrq(ChannelHandlerContext ctx) throws IOException, RdesktopException {
		logger.debug("send_edrq");
		RdpPacket_Localised buffer = ISOUtils.init(5);
		buffer.set8(EDRQ << 2);
		buffer.setBigEndian16(1); // height
		buffer.setBigEndian16(1); // interval
		buffer.markEnd();
		ISOUtils.send(buffer, ctx);
	}

	/**
	 * Transmit an AUrq mesage
	 * 
	 * @throws IOException
	 * @throws RdesktopException
	 */
	public static void send_aurq(ChannelHandlerContext ctx) throws IOException, RdesktopException {
		RdpPacket_Localised buffer = ISOUtils.init(1);

		buffer.set8(AURQ << 2);
		buffer.markEnd();
		ISOUtils.send(buffer, ctx);
	}

	/**
	 * Receive an AUcf message
	 * 
	 * @return UserID specified in message
	 * @throws Exception
	 */
	public static int receive_aucf(ChannelHandlerContext context, ChannelBuffer channelBuffer, RDPSession session)
			throws Exception {
		logger.debug("receive_aucf");
		int opcode = 0, result = 0, UserID = 0;
		RdpPacket_Localised buffer = ISOUtils.receive(context, channelBuffer, session);

		opcode = buffer.get8();
		if ((opcode >> 2) != AUCF) {
			throw new RdesktopException("Expected AUCF got " + opcode);
		}

		result = buffer.get8();
		if (result != 0) {
			throw new RdesktopException("Expected AURQ got " + result);
		}

		if ((opcode & 2) != 0) {
			UserID = buffer.getBigEndian16();
		}

		if (buffer.getPosition() != buffer.getEnd()) {
			throw new RdesktopException();
		}
		return UserID;
	}

	/**
	 * Transmit a CJrq message
	 * 
	 * @param channelid
	 *            Id of channel to be identified in request
	 * @throws IOException
	 * @throws RdesktopException
	 */
	public static void send_cjrq(int channelid, int McsUserID, ChannelHandlerContext context) throws IOException,
			RdesktopException {
		logger.debug("channelid:{}, McsUserId:{}", new Object[] { channelid, McsUserID });
		RdpPacket_Localised buffer = ISOUtils.init(5);
		buffer.set8(CJRQ << 2);
		buffer.setBigEndian16(McsUserID); // height
		buffer.setBigEndian16(channelid); // interval
		buffer.markEnd();
		ISOUtils.send(buffer, context);
	}

	/**
	 * Receive and handle a CJcf message
	 * 
	 * @throws Exception
	 */
	public static void receive_cjcf(ChannelHandlerContext context, ChannelBuffer channelBuffer, RDPSession session)
			throws Exception {
		logger.debug("receive_cjcf");
		int opcode = 0, result = 0;
		RdpPacket_Localised buffer = ISOUtils.receive(context, channelBuffer, session);

		opcode = buffer.get8();
		if ((opcode >> 2) != CJCF) {
			throw new RdesktopException("Expected CJCF got" + opcode);
		}

		result = buffer.get8();
		if (result != 0) {
			throw new RdesktopException("Expected CJRQ got " + result);
		}

		buffer.incrementPosition(4); // skip userid, req_channelid

		if ((opcode & 2) != 0) {
			buffer.incrementPosition(2); // skip join_channelid
		}

		if (buffer.getPosition() != buffer.getEnd()) {
			throw new RdesktopException();
		}
	}

	/**
	 * Initialise a packet as an MCS PDU
	 * 
	 * @param length
	 *            Desired length of PDU
	 * @return
	 * @throws RdesktopException
	 */
	public static RdpPacket_Localised init(int length) throws RdesktopException {
		RdpPacket_Localised data = ISOUtils.init(length + 8);
		// data.pushLayer(RdpPacket_Localised.MCS_HEADER, 8);
		data.setHeader(RdpPacket_Localised.MCS_HEADER);
		data.incrementPosition(8);
		data.setStart(data.getPosition());
		return data;
	}

	/**
	 * Send a packet to a specified channel
	 * 
	 * @param buffer
	 *            Packet to send to channel
	 * @param channel
	 *            Id of channel on which to send packet
	 * @throws RdesktopException
	 * @throws IOException
	 */
	public static void send_to_channel(RdpPacket_Localised buffer, int channel, RDPSession session,
			ChannelHandlerContext context) throws RdesktopException, IOException {
		int length = 0;
		buffer.setPosition(buffer.getHeader(RdpPacket_Localised.MCS_HEADER));

		length = buffer.getEnd() - buffer.getHeader(RdpPacket_Localised.MCS_HEADER) - 8;
		length |= 0x8000;

		buffer.set8((SDRQ << 2));
		buffer.setBigEndian16(session.getMcsUserID());
		buffer.setBigEndian16(channel);
		buffer.set8(0x70); // Flags
		buffer.setBigEndian16(length);
		ISOUtils.send(buffer, context);
	}

	/**
	 * Receive an MCS PDU from the next channel with available data
	 * 
	 * @param channel
	 *            ID of channel will be stored in channel[0]
	 * @return Received packet
	 * @throws Exception
	 */
	public static RdpPacket_Localised receive(ChannelHandlerContext context, int[] channel, ChannelBuffer channelBuffer,
			RDPSession session) throws Exception {
		logger.debug("receive");
		int opcode = 0, appid = 0, length = 0;
		RdpPacket_Localised buffer = ISOUtils.receive(context, channelBuffer, session);
		if (buffer == null)
			return null;
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
		channel[0] = buffer.getBigEndian16(); // Get ChannelID
		// logger.debug("Channel ID = " + channel[0]);
		buffer.incrementPosition(1); // Skip Flags

		length = buffer.get8();

		if ((length & 0x80) != 0) {
			buffer.incrementPosition(1);
		}
		buffer.setStart(buffer.getPosition());

		return buffer;
	}

	public static int getUserID(RDPSession session) {
		return session.getMcsUserID();
	}

}
