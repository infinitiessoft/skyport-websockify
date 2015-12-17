package com.infinities.skyport.rdp.custom;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.wsgate.UpdateUtils;
import com.lixia.rdp.CommunicationMonitor;
import com.lixia.rdp.Constants;
import com.lixia.rdp.OrderException;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

public class RDPUtils {

	private static final Logger logger = LoggerFactory.getLogger(RDPUtils.class);

	// PDU Types
	// private static final int RDP_PDU_DEMAND_ACTIVE = 1;
	// private static final int RDP_PDU_CONFIRM_ACTIVE = 3;
	// private static final int RDP_PDU_DEACTIVATE = 6;
	private static final int RDP_PDU_DATA = 7;
	// private static final int RDP_DATA_PDU_UPDATE = 2;
	// private static final int RDP_DATA_PDU_CONTROL = 20;
	// private static final int RDP_DATA_PDU_POINTER = 27;
	private static final int RDP_DATA_PDU_INPUT = 28;
	// private static final int RDP_DATA_PDU_SYNCHRONISE = 31;
	// private static final int RDP_DATA_PDU_BELL = 34;
	// private static final int RDP_DATA_PDU_LOGON = 38;
	private static final int RDP_DATA_PDU_FONT2 = 39;
	// private static final int RDP_DATA_PDU_DISCONNECT = 47;
	// private static final int RDP_INPUT_SYNCHRONIZE = 0;
	// private static final int RDP_CTL_REQUEST_CONTROL = 1;
	// private static final int RDP_CTL_COOPERATE = 4;
	// private static final int RDP_NULL_POINTER = 0;
	// Update PDU Types
	// private static final int RDP_UPDATE_ORDERS = 0;
	// private static final int RDP_UPDATE_BITMAP = 1;
	// private static final int RDP_UPDATE_PALETTE = 2;
	// private static final int RDP_UPDATE_SYNCHRONIZE = 3;
	// Pointer PDU Types
	// private static final int RDP_POINTER_SYSTEM = 1;
	// private static final int RDP_POINTER_MOVE = 3;
	// private static final int RDP_POINTER_COLOR = 6;
	// private static final int RDP_POINTER_CACHED = 7;
	/* RDP capabilities */
	// private static final int RDP_CAPSET_GENERAL = 1;
	// private static final int RDP_CAPLEN_GENERAL = 0x18;
	// private static final int OS_MAJOR_TYPE_UNIX = 4;
	// private static final int OS_MINOR_TYPE_XSERVER = 7;
	// private static final int RDP_CAPSET_BITMAP = 2;
	// private static final int RDP_CAPLEN_BITMAP = 0x1C;
	// private static final int RDP_CAPSET_ORDER = 3;
	// private static final int RDP_CAPLEN_ORDER = 0x58;
	// private static final int ORDER_CAP_NEGOTIATE = 2;
	// private static final int ORDER_CAP_NOSUPPORT = 4;
	// private static final int RDP_CAPSET_BMPCACHE = 4;
	// private static final int RDP_CAPLEN_BMPCACHE = 0x28;
	// private static final int RDP_CAPSET_CONTROL = 5;
	// private static final int RDP_CAPLEN_CONTROL = 0x0C;
	// private static final int RDP_CAPSET_ACTIVATE = 7;
	// private static final int RDP_CAPLEN_ACTIVATE = 0x0C;
	// private static final int RDP_CAPSET_POINTER = 8;
	// private static final int RDP_CAPLEN_POINTER = 0x08;
	// private static final int RDP_CAPSET_SHARE = 9;
	// private static final int RDP_CAPLEN_SHARE = 0x08;
	// private static final int RDP_CAPSET_COLCACHE = 10;
	// private static final int RDP_CAPLEN_COLCACHE = 0x08;
	// private static final int RDP_CAPSET_UNKNOWN = 13;
	// private static final int RDP_CAPLEN_UNKNOWN = 0x9C;
	// private static final int RDP_CAPSET_BMPCACHE2 = 19;
	// private static final int RDP_CAPLEN_BMPCACHE2 = 0x28;
	/* RDP bitmap cache (version 2) constants */
	public static final int BMPCACHE2_C0_CELLS = 0x78;
	public static final int BMPCACHE2_C1_CELLS = 0x78;
	public static final int BMPCACHE2_C2_CELLS = 0x150;
	public static final int BMPCACHE2_NUM_PSTCELLS = 0x9f6;


	// private static final int BMPCACHE2_FLAG_PERSIST = (1 << 31);

	public static void sendInput(int time, int message_type, int device_flags, int param1, int param2, RDPSession session,
			ChannelHandlerContext context) throws RdesktopException, IOException, CryptoException {
		RdpPacket_Localised data = null;
		data = initData(16, session);

		data.setLittleEndian16(1); /* number of events */
		data.setLittleEndian16(0); /* pad */

		data.setLittleEndian32(time);
		data.setLittleEndian16(message_type);
		data.setLittleEndian16(device_flags);
		data.setLittleEndian16(param1);
		data.setLittleEndian16(param2);
		data.markEnd();
		logger.debug("message_type:{}, device_flags:{}, param1:{}, param2:{}", new Object[] { message_type, device_flags,
				param1, param2 });

		// logger.info("input");
		// if(logger.isInfoEnabled()) logger.info(data);

		sendData(data, RDP_DATA_PDU_INPUT, session, context);
	}

	/**
	 * Send a packet on the RDP layer
	 * 
	 * @param data
	 *            Packet to send
	 * @param data_pdu_type
	 *            Type of data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public static void sendData(RdpPacket_Localised data, int data_pdu_type, RDPSession session,
			ChannelHandlerContext context) throws RdesktopException, IOException, CryptoException {

		CommunicationMonitor.lock(session);

		int length;

		data.setPosition(data.getHeader(RdpPacket_Localised.RDP_HEADER));
		length = data.getEnd() - data.getPosition();

		data.setLittleEndian16(length);
		data.setLittleEndian16(RDP_PDU_DATA | 0x10);
		data.setLittleEndian16(SecureUtils.getUserID(session) + 1001);

		data.setLittleEndian32(session.getRdp_shareid());
		data.set8(0); // pad
		data.set8(1); // stream id
		data.setLittleEndian16(length - 14);
		data.set8(data_pdu_type);
		data.set8(0); // compression type
		data.setLittleEndian16(0); // compression length

		SecureUtils.send(data, Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0, session, context);

		CommunicationMonitor.unlock(session);
	}

	public static void sendFonts(int seq, RDPSession session, ChannelHandlerContext context) throws RdesktopException,
			IOException, CryptoException {

		RdpPacket_Localised data = initData(8, session);

		data.setLittleEndian16(0); /* number of fonts */
		data.setLittleEndian16(0x3e); /* unknown */
		data.setLittleEndian16(seq); /* unknown */
		data.setLittleEndian16(0x32); /* entry size */

		data.markEnd();
		sendData(data, RDP_DATA_PDU_FONT2, session, context);
	}

	/**
	 * Initialise a packet for sending data on the RDP layer
	 * 
	 * @param size
	 *            Size of RDP data
	 * @return Packet initialised for RDP
	 * @throws RdesktopException
	 */
	public static RdpPacket_Localised initData(int size, RDPSession session) throws RdesktopException {
		RdpPacket_Localised buffer = null;

		buffer = SecureUtils.init(Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0, size + 18, session);
		buffer.pushLayer(RdpPacket_Localised.RDP_HEADER, 18);
		// buffer.setHeader(RdpPacket_Localised.RDP_HEADER);
		// buffer.incrementPosition(18);
		// buffer.setStart(buffer.getPosition());
		return buffer;
	}

	/**
	 * Process an RDP5 packet
	 * 
	 * @param s
	 *            Packet to be processed
	 * @param e
	 *            True if packet is encrypted
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public static void rdp5_process(ChannelHandlerContext context, RdpPacket_Localised s, boolean e, RDPSession session)
			throws RdesktopException, OrderException, CryptoException {
		rdp5_process(context, s, e, false, session);
	}

	/**
	 * Process an RDP5 packet
	 * 
	 * @param s
	 *            Packet to be processed
	 * @param encryption
	 *            True if packet is encrypted
	 * @param shortform
	 *            True if packet is of the "short" form
	 * @throws RdesktopException
	 * @throws OrderException
	 * @throws CryptoException
	 */
	public static void rdp5_process(ChannelHandlerContext context, RdpPacket_Localised s, boolean encryption,
			boolean shortform, RDPSession session) throws RdesktopException, OrderException, CryptoException {
		logger.debug("Processing RDP 5 order");

		// int length, count;
		// int type;
		// int next;

		if (encryption) {
			s.incrementPosition(8);/* signature */
			byte[] data = new byte[s.size() - s.getPosition()];
			s.copyToByteArray(data, 0, s.getPosition(), data.length);
			byte[] packet = SecureUtils.decrypt(data, session);
			s.copyFromByteArray(packet, 0, s.getPosition(), packet.length);
		}

		byte[] packet = new byte[s.size() - s.getPosition()];
		s.copyToByteArray(packet, 0, s.getPosition(), packet.length);
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(packet);
		logger.debug("packet length:{}", packet.length);

		UpdateUtils.send(context, buffer);

		// while (s.getPosition() < s.getEnd()) {
		// type = s.get8();
		//
		// if ((type & 0x80) > 0) {
		// type ^= 0x80;
		// } else {
		// }
		// length = s.getLittleEndian16();
		// /* next_packet = */next = s.getPosition() + length;
		// logger.debug("length:{}", length);
		// logger.debug("position:{}", s.getPosition());
		// logger.debug("RDP5: type = {}", type);
		// logger.debug("size:{}", s.getEnd() - s.getPosition());
		// logger.debug("next:{}", next);
		// UpdateUtils.sendBeginPaint(context);
		// switch (type) {
		// case 0: /* orders */
		// count = s.getLittleEndian16();
		// session.getOrders().processOrders(context, s, next, count);
		// break;
		// case 1: /* bitmap update (???) */
		// logger.debug("size:{}", s.getEnd() - s.getPosition());
		// s.incrementPosition(2); /* part length */
		// processBitmapUpdates(context, s, session);
		// logger.debug("size:{}", s.getEnd() - s.getPosition());
		// break;
		// case 2: /* palette */
		// s.incrementPosition(2);
		// processPalette(s, session);
		// UpdateUtils.sendPalette();
		// break;
		// case 3: /* probably an palette with offset 3. Weird */
		// UpdateUtils.sendSynchronize(context);
		// break;
		// case 5:
		// process_null_system_pointer_pdu(s, session);
		// break;
		// case 6: // default pointer
		// break;
		// case 9:
		// process_colour_pointer_pdu(s, session);
		// break;
		// case 10:
		// process_cached_pointer_pdu(s, session);
		// break;
		// default:
		// logger.warn("Unimplemented RDP5 opcode " + type);
		// }
		// UpdateUtils.sendEndPaint(context);
		// logger.debug("slice:{}", next - s.getPosition());
		// s.setPosition(next);
		// }
	}

	/**
	 * Process an RDP5 packet from a virtual channel
	 * 
	 * @param s
	 *            Packet to be processed
	 * @param channelno
	 *            Channel on which packet was received
	 */
	public void
			rdp5_process_channel(RdpPacket_Localised s, int channelno, RDPSession session, ChannelHandlerContext context) {
		CustomVChannel channel = session.getChannels().find_channel_by_channelno(channelno);
		if (channel != null) {
			try {
				channel.process(s, session, context);
			} catch (Exception e) {
			}
		}
	}

	public static void processBitmapUpdates(ChannelHandlerContext context, RdpPacket_Localised data, RDPSession session)
			throws RdesktopException {
		// logger.info("processBitmapUpdates");
		int n_updates = 0;
		int left = 0, top = 0, right = 0, bottom = 0, width = 0, height = 0;
		int cx = 0, cy = 0, bitsperpixel = 0, compression = 0, buffersize = 0, size = 0;
		byte[] pixel = null;

		int minX, minY, maxX, maxY;

		maxX = maxY = 0;
		minX = session.getOptions().getWidth();
		minY = session.getOptions().getHeight();

		n_updates = data.getLittleEndian16();
		logger.debug("n_updates: {}", n_updates);

		for (int i = 0; i < n_updates; i++) {

			left = data.getLittleEndian16(); // 1
			top = data.getLittleEndian16(); // 4
			right = data.getLittleEndian16(); // 8
			bottom = data.getLittleEndian16(); // 10
			width = data.getLittleEndian16(); // 20
			height = data.getLittleEndian16(); // 40
			bitsperpixel = data.getLittleEndian16(); // 80
			int Bpp = (bitsperpixel + 7) / 8;
			compression = data.getLittleEndian16(); // flags 100
			buffersize = data.getLittleEndian16(); // bitmapLength
			cx = right - left + 1;
			cy = bottom - top + 1;
			ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
			buffer.writeBytes(UpdateUtils.int32ToByteArray(UpdateUtils.WSOP_SC_BITMAP));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(left));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(top));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(width));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(height));

			buffer.writeBytes(UpdateUtils.int32ToByteArray(cx));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(cy));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(bitsperpixel));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(compression));
			buffer.writeBytes(UpdateUtils.int32ToByteArray(buffersize));
			logger.debug(
					"left:{}, top:{}, width:{}, height:{}, cx:{}, cy:{}, bitsperpixel:{}, comperssion:{}, buffersize:{}, Bpp:{}",
					new Object[] { left, top, width, height, cx, cy, bitsperpixel, compression, buffersize, Bpp });
			if (minX > left)
				minX = left;
			if (minY > top)
				minY = top;
			if (maxX < right)
				maxX = right;
			if (maxY < bottom)
				maxY = bottom;

			/* Server may limit bpp - this is how we find out */
			if (session.getOptions().getServer_bpp() != bitsperpixel) {
				logger.warn("Server limited colour depth to " + bitsperpixel + " bits");
				session.getOptions().set_bpp(bitsperpixel);
			}

			if (compression == 0) {
				logger.info("compression == 0");
				pixel = new byte[width * height * Bpp];

				for (int y = 0; y < height; y++) {
					logger.debug("Bpp: {}, offset: {}", new Object[] { Bpp, (height - y - 1) * (width * Bpp) });
					data.copyToByteArray(pixel, (height - y - 1) * (width * Bpp), data.getPosition(), width * Bpp);
					data.incrementPosition(width * Bpp);
				}

				// surface.displayImage(Bitmap.convertImage(pixel, Bpp), width,
				// height, left, top, cx, cy);
				continue;
			}

			if ((compression & 0x400) != 0) {
				logger.info("compression & 0x400 != 0");
				size = buffersize;
			} else {
				logger.info("compression & 0x400 == 0");
				data.incrementPosition(2); // pad
				size = data.getLittleEndian16();
				data.incrementPosition(4); // line size, final size
			}
			logger.debug("bmdata size:{}", size);
			byte[] compressed_pixel = new byte[size];
			data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
			data.incrementPosition(size);
			buffer.writeBytes(compressed_pixel);
			UpdateUtils.sendBitmapUpdates(context, buffer);
		}
	}

	protected static void processPalette(RdpPacket_Localised data, RDPSession session) {
		int n_colors = 0;
		// IndexColorModel cm = null;
		byte[] palette = null;

		byte[] red = null;
		byte[] green = null;
		byte[] blue = null;
		int j = 0;

		data.incrementPosition(2); // pad
		n_colors = data.getLittleEndian16(); // Number of Colors in Palette
		data.incrementPosition(2); // pad
		palette = new byte[n_colors * 3];
		red = new byte[n_colors];
		green = new byte[n_colors];
		blue = new byte[n_colors];
		data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
		data.incrementPosition(palette.length);
		for (int i = 0; i < n_colors; i++) {
			red[i] = palette[j];
			green[i] = palette[j + 1];
			blue[i] = palette[j + 2];
			j += 3;
		}
		// cm = new IndexColorModel(8, n_colors, red, green, blue);
		// session.getSurface().registerPalette(cm);
	}

	protected static void process_colour_pointer_pdu(RdpPacket_Localised data, RDPSession session) throws RdesktopException {
		logger.debug("Rdp.RDP_POINTER_COLOR");
		// int x = 0, y = 0, width = 0, height = 0, cache_idx = 0;
		int masklen = 0, datalen = 0;
		// byte[] mask = null, pixel = null;
		// Cursor cursor = null;

		// cache_idx =
		data.getLittleEndian16();
		// x =
		data.getLittleEndian16();
		// y =
		data.getLittleEndian16();
		// width =
		data.getLittleEndian16();
		// height =
		data.getLittleEndian16();
		masklen = data.getLittleEndian16();
		datalen = data.getLittleEndian16();
		// mask = new byte[masklen];
		// pixel = new byte[datalen];
		// data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
		data.incrementPosition(datalen);
		// data.copyToByteArray(mask, 0, data.getPosition(), masklen);
		data.incrementPosition(masklen);
		// cursor = session.getSurface().createCursor(x, y, width, height, mask,
		// pixel, cache_idx);
		// logger.info("Creating and setting cursor " + cache_idx);
		// session.getSurface().setCursor(cursor);
		// setSubCursor(cursor, session);
		// session.getCache().putCursor(cache_idx, cursor);
	}

	protected static void process_cached_pointer_pdu(RdpPacket_Localised data, RDPSession session) throws RdesktopException {
		// logger.debug("Rdp.RDP_POINTER_CACHED");
		int cache_idx = data.getLittleEndian16();
		logger.info("Setting cursor " + cache_idx);
		// session.getSurface().setCursor(session.getCache().getCursor(cache_idx));
		// setSubCursor(session.getCache().getCursor(cache_idx), session);
	}

	/* Process a null system pointer PDU */
	protected static void process_null_system_pointer_pdu(RdpPacket_Localised s, RDPSession session)
			throws RdesktopException {
		// FIXME: We should probably set another cursor here,
		// like the X window system base cursor or something.

		// if (session.getG_null_cursor() == null) {
		// byte[] null_pointer_mask = new byte[1], null_pointer_data = new
		// byte[24];
		//
		// null_pointer_mask[0] = (byte) 0x80;

		// session.setG_null_cursor(session.getSurface().createCursor(0, 0,
		// 1, 1, null_pointer_mask, null_pointer_data, 0));
		// }
		// session.getSurface().setCursor(session.getG_null_cursor());
		// setSubCursor(session.getG_null_cursor(), session);
		// surface.setCursor(cache.getCursor(0));
		// setSubCursor(cache.getCursor(0));
	}

	// private static void setSubCursor(Cursor cursor, RDPSession session) {
	// // session.getSurface().setCursor(cursor);
	// }

	/**
	 * Receive a packet from the RDP layer
	 * 
	 * @param type
	 *            Type of PDU received, stored in type[0]
	 * @return Packet received from RDP layer
	 * @throws Exception
	 */
	// public static RdpPacket_Localised receive(int[] type, ChannelBuffer
	// channelBuffer, RDPSession session,
	// ChannelHandlerContext context) throws Exception {
	// int length = 0;
	// logger.debug("receive data: {}, next_packet: {}", new Object[] {
	// session.getStream(), session.getNext_packet() });
	// if ((session.getStream() == null) || (session.getNext_packet() >=
	// session.getStream().getEnd())) {
	// session.setStream(SecureUtils.receive(channelBuffer, session, context));
	// // logger.debug("key:{}, value: {}, stream:{}",
	// // new Object[] { entry.getKey(), entry.getValue(),
	// // session.getStream() });
	// if (session.getStream() == null)
	// return null;
	// session.setNext_packet(session.getStream().getPosition());
	// } else {
	// session.getStream().setPosition(session.getNext_packet());
	// }
	// length = session.getStream().getLittleEndian16();
	//
	// /* 32k packets are really 8, keepalive fix - rdesktop 1.2.0 */
	// if (length == 0x8000) {
	// logger.warn("32k packet keepalive fix");
	// session.setNext_packet(session.getNext_packet() + 8);
	// type[0] = 0;
	// return session.getStream();
	// }
	// type[0] = session.getStream().getLittleEndian16() & 0xf;
	// if (session.getStream().getPosition() != session.getStream().getEnd()) {
	// session.getStream().incrementPosition(2);
	// }
	//
	// session.setNext_packet(session.getNext_packet() + length);
	// return session.getStream();
	// }
}
