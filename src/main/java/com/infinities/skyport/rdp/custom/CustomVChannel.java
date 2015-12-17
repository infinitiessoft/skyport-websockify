/* VChannel.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:39 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Abstract class for RDP5 channels
 */
package com.infinities.skyport.rdp.custom;

import java.io.IOException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lixia.rdp.Constants;
import com.lixia.rdp.Options;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

public abstract class CustomVChannel {

	protected static Logger logger = LoggerFactory.getLogger(CustomVChannel.class);
	private Options options;
	private int mcs_id = 0;


	public CustomVChannel(Options options) {
		this.options = options;
	}

	/**
	 * Provide the name of this channel
	 * 
	 * @return Channel name as string
	 */
	public abstract String name();

	/**
	 * Provide the set of flags specifying working options for this channel
	 * 
	 * @return Option flags
	 */
	public abstract int flags();

	/**
	 * Process a packet sent on this channel
	 * 
	 * @param data
	 *            Packet sent to this channel
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public abstract void process(RdpPacket data, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException;

	public int mcs_id() {
		return mcs_id;
	}

	/**
	 * Set the MCS ID for this channel
	 * 
	 * @param mcs_id
	 *            New MCS ID
	 */
	public void set_mcs_id(int mcs_id) {
		this.mcs_id = mcs_id;
	}

	/**
	 * Initialise a packet for transmission over this virtual channel
	 * 
	 * @param length
	 *            Desired length of packet
	 * @return Packet prepared for this channel
	 * @throws RdesktopException
	 */
	public RdpPacket_Localised init(int length, RDPSession session) throws RdesktopException {
		RdpPacket_Localised s;

		s = SecureUtils.init(options.isEncryption() ? SecureUtils.SEC_ENCRYPT : 0, length + 8, session);
		s.setHeader(RdpPacket.CHANNEL_HEADER);
		s.incrementPosition(8);

		return s;
	}

	/**
	 * Send a packet over this virtual channel
	 * 
	 * @param data
	 *            Packet to be sent
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public void send_packet(RdpPacket_Localised data, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {

		int length = data.size();
		logger.debug("send packet: {}", length);
		int data_offset = 0;
		// int packets_sent = 0;
		int num_packets = (length / CustomVChannels.CHANNEL_CHUNK_LENGTH);
		num_packets += length - (CustomVChannels.CHANNEL_CHUNK_LENGTH) * num_packets;

		while (data_offset < length) {
			logger.debug("data_offset: {}, length: {}, ", new Object[] { data_offset, length });
			int thisLength = Math.min(CustomVChannels.CHANNEL_CHUNK_LENGTH, length - data_offset);
			logger.debug("thisLength:{}", thisLength);
			RdpPacket_Localised s = SecureUtils.init(Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0, 8 + thisLength,
					session);
			s.setLittleEndian32(length);

			int flags = ((data_offset == 0) ? CustomVChannels.CHANNEL_FLAG_FIRST : 0);
			if (data_offset + thisLength >= length)
				flags |= CustomVChannels.CHANNEL_FLAG_LAST;

			if ((this.flags() & CustomVChannels.CHANNEL_OPTION_SHOW_PROTOCOL) != 0)
				flags |= CustomVChannels.CHANNEL_FLAG_SHOW_PROTOCOL;
			// System.out.printf("Sending %d bytes with flags %d\n", thisLength,
			// flags);
			logger.debug("flags:{}", flags);
			s.setLittleEndian32(flags);
			logger.debug("data_offset:{}, position:{}, thisLength:{}", new Object[] { data_offset, s.getPosition(),
					thisLength });
			s.copyFromPacket(data, data_offset, s.getPosition(), thisLength);
			s.incrementPosition(thisLength);
			s.markEnd();

			data_offset += thisLength;
			logger.debug("mcs_id: {}", this.mcs_id());
			SecureUtils.send_to_channel(s, Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0, this.mcs_id(), session,
					context);
			// packets_sent++;
		}
	}

}
