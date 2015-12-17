package com.infinities.skyport.rdp.handler;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.ISOUtils;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;

//ISO.connect
//MCS.connect.sendConnectInitial
public class SendCredentialHandler extends FrameDecoder {

	private static final Logger logger = LoggerFactory.getLogger(SendCredentialHandler.class);
	private static final int CONNECTION_CONFIRM = 0xD0;
	private static final int TAG_DOMAIN_PARAMS = 0x30;
	private static final int CONNECT_INITIAL = 0x7f65;
	private static final int BER_TAG_OCTET_STRING = 4;
	private static final int BER_TAG_BOOLEAN = 1;
	private static final int BER_TAG_INTEGER = 2;
	private RdpPacket_Localised data;
	private RDPSession session;


	public SendCredentialHandler(RdpPacket_Localised data, RDPSession session) {
		this.data = data;
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		logger.debug("receive buffer");
		int[] code = new int[1];
		ISOUtils.receiveMessage(ctx, code, buffer, session);
		logger.debug("receive code: {}", code[0]);
		if (code[0] != CONNECTION_CONFIRM) {
			throw new RdesktopException("Expected CC got:" + Integer.toHexString(code[0]).toUpperCase());
		}
		sendConnectInitial(data, ctx);
		ctx.getPipeline().remove(this);
		return null;
	}

	/**
	 * 
	 * Send an MCS_CONNECT_INITIAL message (encoded as ASN.1 Ber)
	 * 
	 * @param data
	 *            Packet in which to send the message
	 * @throws IOException
	 * @throws RdesktopException
	 */
	public void sendConnectInitial(RdpPacket_Localised data, ChannelHandlerContext ctx) throws IOException,
			RdesktopException {
		logger.debug("MCS.sendConnectInitial");
		int datalen = data.getEnd();
		logger.debug("datalen: {}", datalen);
		int length = 9 + domainParamSize(34, 2, 0, 0xffff) + domainParamSize(1, 1, 1, 0x420)
				+ domainParamSize(0xffff, 0xfc17, 0xffff, 0xffff) + 4 + datalen; // RDP5
																					// Code

		RdpPacket_Localised buffer = ISOUtils.init(length + 5);

		sendBerHeader(buffer, CONNECT_INITIAL, length);
		sendBerHeader(buffer, BER_TAG_OCTET_STRING, 1); // calling domain
		buffer.set8(1); // RDP5 Code
		sendBerHeader(buffer, BER_TAG_OCTET_STRING, 1); // called domain
		buffer.set8(1); // RDP5 Code

		sendBerHeader(buffer, BER_TAG_BOOLEAN, 1);
		buffer.set8(0xff); // upward flag

		sendDomainParams(buffer, 34, 2, 0, 0xffff); // target parameters // RDP5
													// Code
		sendDomainParams(buffer, 1, 1, 1, 0x420); // minimum parameters
		sendDomainParams(buffer, 0xffff, 0xfc17, 0xffff, 0xffff); // maximum
																	// parameters

		sendBerHeader(buffer, BER_TAG_OCTET_STRING, datalen);
		logger.debug("position:{}. end:{}", new Object[] { buffer.getPosition(), data.getEnd() });
		data.copyToPacket(buffer, 0, buffer.getPosition(), data.getEnd());
		buffer.incrementPosition(data.getEnd());
		buffer.markEnd();
		ISOUtils.send(buffer, ctx);
	}

	/**
	 * Determine the size of the domain parameters, encoded according to the ISO
	 * ASN.1 Basic Encoding Rules
	 * 
	 * @param max_channels
	 *            Maximum number of channels
	 * @param max_users
	 *            Maximum number of users
	 * @param max_tokens
	 *            Maximum number of tokens
	 * @param max_pdusize
	 *            Maximum size of an MCS PDU
	 * @return Number of bytes the domain parameters would occupy
	 */
	private int domainParamSize(int max_channels, int max_users, int max_tokens, int max_pdusize) {
		int endSize = BERIntSize(max_channels) + BERIntSize(max_users) + BERIntSize(max_tokens) + BERIntSize(1)
				+ BERIntSize(0) + BERIntSize(1) + BERIntSize(max_pdusize) + BERIntSize(2);
		return berHeaderSize(TAG_DOMAIN_PARAMS, endSize) + endSize;
	}

	/**
	 * Determine the size of a BER encoded integer with specified value
	 * 
	 * @param value
	 *            Value of integer
	 * @return Number of bytes the encoded data would occupy
	 */
	private int BERIntSize(int value) {
		if (value > 0xff)
			return 4;
		else
			return 3;
	}

	/**
	 * Determine the size of a BER header encoded for the specified tag and data
	 * length
	 * 
	 * @param tagval
	 *            Value of tag identifying data type
	 * @param length
	 *            Length of data header will precede
	 * @return
	 */
	private int berHeaderSize(int tagval, int length) {
		int total = 0;
		if (tagval > 0xff) {
			total += 2;
		} else {
			total += 1;
		}

		if (length >= 0x80) {
			total += 3;
		} else {
			total += 1;
		}
		return total;
	}

	/**
	 * Send a Header encoded according to the ISO ASN.1 Basic Encoding rules
	 * 
	 * @param buffer
	 *            Packet in which to send the header
	 * @param tagval
	 *            Data type for header
	 * @param length
	 *            Length of data header precedes
	 */
	private void sendBerHeader(RdpPacket_Localised buffer, int tagval, int length) {
		if (tagval > 0xff) {
			buffer.setBigEndian16(tagval);
		} else {
			buffer.set8(tagval);
		}

		if (length >= 0x80) {
			buffer.set8(0x82);
			buffer.setBigEndian16(length);
		} else {
			buffer.set8(length);
		}
	}

	/**
	 * send a DOMAIN_PARAMS structure encoded according to the ISO ASN.1 Basic
	 * Encoding rules
	 * 
	 * @param buffer
	 *            Packet in which to send the structure
	 * @param max_channels
	 *            Maximum number of channels
	 * @param max_users
	 *            Maximum number of users
	 * @param max_tokens
	 *            Maximum number of tokens
	 * @param max_pdusize
	 *            Maximum size for an MCS PDU
	 */
	private void sendDomainParams(RdpPacket_Localised buffer, int max_channels, int max_users, int max_tokens,
			int max_pdusize) {

		int size = BERIntSize(max_channels) + BERIntSize(max_users) + BERIntSize(max_tokens) + BERIntSize(1) + BERIntSize(0)
				+ BERIntSize(1) + BERIntSize(max_pdusize) + BERIntSize(2);

		sendBerHeader(buffer, TAG_DOMAIN_PARAMS, size);
		sendBerInteger(buffer, max_channels);
		sendBerInteger(buffer, max_users);
		sendBerInteger(buffer, max_tokens);

		sendBerInteger(buffer, 1); // num_priorities
		sendBerInteger(buffer, 0); // min_throughput
		sendBerInteger(buffer, 1); // max_height

		sendBerInteger(buffer, max_pdusize);
		sendBerInteger(buffer, 2); // ver_protocol
	}

	/**
	 * send an Integer encoded according to the ISO ASN.1 Basic Encoding Rules
	 * 
	 * @param buffer
	 *            Packet in which to store encoded value
	 * @param value
	 *            Integer value to store
	 */
	public void sendBerInteger(RdpPacket_Localised buffer, int value) {

		int len = 1;

		if (value > 0xff)
			len = 2;

		sendBerHeader(buffer, BER_TAG_INTEGER, len);

		if (value > 0xff) {
			buffer.setBigEndian16(value);
		} else {
			buffer.set8(value);
		}

	}

}
