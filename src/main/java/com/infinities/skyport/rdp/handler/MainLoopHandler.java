package com.infinities.skyport.rdp.handler;

//import java.awt.Cursor;
//import java.awt.Toolkit;
import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.NDC;

import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.Message;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.RDPUtils;
import com.infinities.skyport.rdp.custom.SecureUtils;
import com.lixia.rdp.Constants;
import com.lixia.rdp.OrderException;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

public class MainLoopHandler extends OneToOneDecoder {

	private static final Logger logger = LoggerFactory.getLogger(MainLoopHandler.class);
	private static final int RDP_PDU_DEMAND_ACTIVE = 1;
	private static final int RDP_PDU_CONFIRM_ACTIVE = 3;
	private static final int RDP_PDU_DEACTIVATE = 6;
	private static final int RDP_PDU_DATA = 7;
	private static final int RDP_DATA_PDU_UPDATE = 2;
	private static final int RDP_DATA_PDU_CONTROL = 20;
	private static final int RDP_DATA_PDU_POINTER = 27;
	private static final int RDP_DATA_PDU_INPUT = 28;
	private static final int RDP_DATA_PDU_SYNCHRONISE = 31;
	private static final int RDP_DATA_PDU_BELL = 34;
	private static final int RDP_DATA_PDU_LOGON = 38;
	// private static final int RDP_DATA_PDU_FONT2 = 39;
	private static final int RDP_DATA_PDU_DISCONNECT = 47;
	// private static final int RDP_INPUT_SYNCHRONIZE = 0;
	private static final int RDP_CTL_REQUEST_CONTROL = 1;
	private static final int RDP_CTL_COOPERATE = 4;
	private static final int RDP_NULL_POINTER = 0;
	// Update PDU Types
	private static final int RDP_UPDATE_ORDERS = 0;
	private static final int RDP_UPDATE_BITMAP = 1;
	private static final int RDP_UPDATE_PALETTE = 2;
	private static final int RDP_UPDATE_SYNCHRONIZE = 3;
	// Pointer PDU Types
	private static final int RDP_POINTER_SYSTEM = 1;
	private static final int RDP_POINTER_MOVE = 3;
	private static final int RDP_POINTER_COLOR = 6;
	private static final int RDP_POINTER_CACHED = 7;
	/* RDP capabilities */
	private static final int RDP_CAPSET_GENERAL = 1;
	private static final int RDP_CAPLEN_GENERAL = 0x18;
	// private static final int OS_MAJOR_TYPE_UNIX = 4;
	// private static final int OS_MINOR_TYPE_XSERVER = 7;
	private static final int RDP_CAPSET_BITMAP = 2;
	private static final int RDP_CAPLEN_BITMAP = 0x1C;
	private static final int RDP_CAPSET_ORDER = 3;
	private static final int RDP_CAPLEN_ORDER = 0x58;
	// private static final int ORDER_CAP_NEGOTIATE = 2;
	// private static final int ORDER_CAP_NOSUPPORT = 4;
	// private static final int RDP_CAPSET_BMPCACHE = 4;
	private static final int RDP_CAPLEN_BMPCACHE = 0x28;
	private static final int RDP_CAPSET_CONTROL = 5;
	private static final int RDP_CAPLEN_CONTROL = 0x0C;
	private static final int RDP_CAPSET_ACTIVATE = 7;
	private static final int RDP_CAPLEN_ACTIVATE = 0x0C;
	private static final int RDP_CAPSET_POINTER = 8;
	private static final int RDP_CAPLEN_POINTER = 0x08;
	private static final int RDP_CAPSET_SHARE = 9;
	private static final int RDP_CAPLEN_SHARE = 0x08;
	private static final int RDP_CAPSET_COLCACHE = 10;
	private static final int RDP_CAPLEN_COLCACHE = 0x08;
	// private static final int RDP_CAPSET_UNKNOWN = 13;
	private static final int RDP_CAPLEN_UNKNOWN = 0x9C;
	private static final int RDP_CAPSET_BMPCACHE2 = 19;
	private static final int RDP_CAPLEN_BMPCACHE2 = 0x28;
	/* RDP bitmap cache (version 2) constants */
	public static final int BMPCACHE2_C0_CELLS = 0x78;
	public static final int BMPCACHE2_C1_CELLS = 0x78;
	public static final int BMPCACHE2_C2_CELLS = 0x150;
	public static final int BMPCACHE2_NUM_PSTCELLS = 0x9f6;
	private static final int BMPCACHE2_FLAG_PERSIST = (1 << 31);
	private RDPSession session;
	// private boolean keep_running = true;
	private boolean[] deactivated = new boolean[1];
	private int[] ext_disc_reason = new int[1];
	// private RdpPacket_Localised stream = null;
	// private int next_packet = 0;
	private static byte caps_0x0d[] = { 0x01, 0x00, 0x00, 0x00, 0x09, 0x04, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	private static byte caps_0x0c[] = { 0x01, 0x00, 0x00, 0x00 };
	private static byte caps_0x0e[] = { 0x01, 0x00, 0x00, 0x00 };
	// private static byte caps_0x10[] = { (byte) 0xFE, 0x00, 0x04, 0x00, (byte)
	// 0xFE, 0x00, 0x04, 0x00, (byte) 0xFE, 0x00,
	// 0x08, 0x00, (byte) 0xFE, 0x00, 0x08, 0x00, (byte) 0xFE, 0x00, 0x10, 0x00,
	// (byte) 0xFE, 0x00, 0x20, 0x00,
	// (byte) 0xFE, 0x00, 0x40, 0x00, (byte) 0xFE, 0x00, (byte) 0x80, 0x00,
	// (byte) 0xFE, 0x00, 0x00, 0x01, 0x40, 0x00,
	// 0x00, 0x08, 0x00, 0x01, 0x00, 0x01, 0x02, 0x00, 0x00, 0x00 };
	private static final int RDP5_FLAG = 0x0030;
	private static final byte[] RDP_SOURCE =
			{ (byte) 0x4D, (byte) 0x53, (byte) 0x54, (byte) 0x53, (byte) 0x43, (byte) 0x00 };


	public MainLoopHandler(RDPSession session) {
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			mainLoop((Message) msg, ctx);
			return null;
		} else {
			return msg;
		}
	}

	private void mainLoop(Message message, ChannelHandlerContext context) throws Exception {
		int type = message.getType();
		RdpPacket_Localised data = message.getPacket();
		// boolean disc = false; /* True when a disconnect PDU was received */
		if (data == null) {
			return;
		}

		logger.debug("type? {}", type);

		switch (type) {

		case (RDP_PDU_DEMAND_ACTIVE):
			logger.debug("Rdp.RDP_PDU_DEMAND_ACTIVE");
			// get this after licence negotiation, just before the 1st
			// order...
			NDC.push("processDemandActive");
			this.processDemandActive(data, context);
			// can use this to trigger things that have to be done before
			// 1st order
			// logger.debug("ready to send (got past licence negotiation)");
			// RdesktopSwing.readytosend = true;
			// session.getSurface().triggerReadyToSend();
			NDC.pop();
			deactivated[0] = false;
			break;

		case (RDP_PDU_DEACTIVATE):
			// get this on log off
			deactivated[0] = true;
			session.setStream(null); // ty this fix
			break;

		case (RDP_PDU_DATA):
			// logger.debug("Rdp.RDP_PDU_DATA");
			// all the others should be this
			NDC.push("processData");

			try {
				this.processData(context, data, ext_disc_reason);
			} catch (Exception ex) {
				logger.error("failed", ex);
			}
			NDC.pop();
			break;

		case 0:
			break; // 32K keep alive fix, see receive() - rdesktop 1.2.0.

		default:
			throw new RdesktopException("Unimplemented type in main loop :" + type);
		}

		return;
	}

	private void processDemandActive(RdpPacket_Localised data, ChannelHandlerContext context) throws Exception {
		// int type[] = new int[1];
		int len_src_descriptor, len_combined_caps;

		session.setRdp_shareid(data.getLittleEndian32());
		len_src_descriptor = data.getLittleEndian16();
		len_combined_caps = data.getLittleEndian16();
		data.incrementPosition(len_src_descriptor);
		processServerCaps(data, len_combined_caps);
		context.getPipeline().addBefore("mainLoopHandler", "processDemandActiveHandler",
				new ProcessDemandActiveHandler(session));
		this.sendConfirmActive(context);
		this.sendSynchronize(context);
		this.sendControl(context, RDP_CTL_COOPERATE);
		this.sendControl(context, RDP_CTL_REQUEST_CONTROL);

		// this.receive(type); // Receive RDP_PDU_SYNCHRONIZE
		// this.receive(type); // Receive RDP_CTL_COOPERATE
		// this.receive(type); // Receive RDP_CTL_GRANT_CONTROL
		//
		// this.sendInput(0, RDP_INPUT_SYNCHRONIZE, 0, 0, 0);
		// this.sendFonts(1);
		// this.sendFonts(2);
		//
		// this.receive(type); // Receive an unknown PDU Code = 0x28
		//
		// this.session.getOrders().resetOrderState();
	}

	private void processServerCaps(RdpPacket_Localised data, int length) {
		int n;
		int next, start;
		int ncapsets, capset_type, capset_length;

		start = data.getPosition();

		ncapsets = data.getLittleEndian16(); // in_uint16_le(s, ncapsets);
		data.incrementPosition(2); // in_uint8s(s, 2); /* pad */

		for (n = 0; n < ncapsets; n++) {
			if (data.getPosition() > start + length)
				return;

			capset_type = data.getLittleEndian16(); // in_uint16_le(s,
													// capset_type);
			capset_length = data.getLittleEndian16(); // in_uint16_le(s,
														// capset_length);

			next = data.getPosition() + capset_length - 4;

			switch (capset_type) {
			case RDP_CAPSET_GENERAL:
				processGeneralCaps(data);
				break;

			case RDP_CAPSET_BITMAP:
				processBitmapCaps(data);
				break;
			}

			data.setPosition(next);
		}
	}

	private void processGeneralCaps(RdpPacket_Localised data) {
		int pad2octetsB; /* rdp5 flags? */

		data.incrementPosition(10); // in_uint8s(s, 10);
		pad2octetsB = data.getLittleEndian16(); // in_uint16_le(s, pad2octetsB);

		if (pad2octetsB == 0)
			session.getOptions().setUse_rdp5(false);
	}

	private void processBitmapCaps(RdpPacket_Localised data) {
		int width, height, depth;

		depth = data.getLittleEndian16(); // in_uint16_le(s, bpp);
		data.incrementPosition(6); // in_uint8s(s, 6);

		width = data.getLittleEndian16(); // in_uint16_le(s, width);
		height = data.getLittleEndian16(); // in_uint16_le(s, height);

		logger.debug("setting desktop size and depth to: " + width + "x" + height + "x" + depth);

		/*
		 * The server may limit bpp and change the size of the desktop (for
		 * example when shadowing another session).
		 */
		if (session.getOptions().getServer_bpp() != depth) {
			logger.warn("colour depth changed from " + session.getOptions().getServer_bpp() + " to " + depth);
			session.getOptions().set_bpp(depth);
		}
		if (session.getOptions().getWidth() != width || session.getOptions().getHeight() != height) {
			logger.warn("screen size changed from " + session.getOptions().getWidth() + "x"
					+ session.getOptions().getHeight() + " to " + width + "x" + height);
			session.getOptions().setWidth(width);
			session.getOptions().setHeight(height);
		}
	}

	private void sendConfirmActive(ChannelHandlerContext context) throws RdesktopException, IOException, CryptoException {
		int caplen =
				RDP_CAPLEN_GENERAL + RDP_CAPLEN_BITMAP + RDP_CAPLEN_ORDER + RDP_CAPLEN_BMPCACHE + RDP_CAPLEN_COLCACHE
						+ RDP_CAPLEN_ACTIVATE + RDP_CAPLEN_CONTROL + RDP_CAPLEN_POINTER + RDP_CAPLEN_SHARE
						+ RDP_CAPLEN_UNKNOWN + 4; // this
													// is
													// a
													// fix
													// for
													// W2k.
													// Purpose
													// unknown

		int sec_flags = session.getOptions().isEncryption() ? (RDP5_FLAG | SecureUtils.SEC_ENCRYPT) : RDP5_FLAG;

		RdpPacket_Localised data = SecureUtils.init(sec_flags, 6 + 14 + caplen + RDP_SOURCE.length, session);

		// RdpPacket_Localised data = this.init(14 + caplen +
		// RDP_SOURCE.length);

		data.setLittleEndian16(2 + 14 + caplen + RDP_SOURCE.length);
		data.setLittleEndian16((RDP_PDU_CONFIRM_ACTIVE | 0x10));
		data.setLittleEndian16(MCSUtils.getUserID(session) /* McsUserID() */+ 1001);

		data.setLittleEndian32(session.getRdp_shareid());
		data.setLittleEndian16(0x3ea); // user id
		data.setLittleEndian16(RDP_SOURCE.length);
		data.setLittleEndian16(caplen);

		data.copyFromByteArray(RDP_SOURCE, 0, data.getPosition(), RDP_SOURCE.length);
		data.incrementPosition(RDP_SOURCE.length);
		data.setLittleEndian16(0xd); // num_caps
		data.incrementPosition(2); // pad

		this.sendGeneralCaps(data);
		// ta.incrementPosition(this.RDP_CAPLEN_GENERAL);
		this.sendBitmapCaps(data);
		this.sendOrderCaps(data);

		// if (Options.use_rdp5 && Options.persistent_bitmap_caching) {
		if (session.getOptions().isUse_rdp5()) {
			logger.info("Persistent caching enabled");
			this.sendBitmapcache2Caps(data);
		} else
			this.sendBitmapcacheCaps(data);

		this.sendColorcacheCaps(data);
		this.sendActivateCaps(data);
		this.sendControlCaps(data);
		this.sendPointerCaps(data);
		this.sendShareCaps(data);
		// this.sendUnknownCaps(data);

		//

		this.sendUnknownCaps(data, 0x0d, 0x58, caps_0x0d); // rdp_out_unknown_caps(s,
															// 0x0d, 0x58,
															// caps_0x0d); /*
															// international? */
		this.sendUnknownCaps(data, 0x0c, 0x08, caps_0x0c); // rdp_out_unknown_caps(s,
															// 0x0c, 0x08,
															// caps_0x0c);
		this.sendUnknownCaps(data, 0x0e, 0x08, caps_0x0e); // rdp_out_unknown_caps(s,
															// 0x0e, 0x08,
															// caps_0x0e);
															// this.sendUnknownCaps(data,
															// 0x10, 0x34,
															// caps_0x10); //
															// rdp_out_unknown_caps(s,
		// 0x10, 0x34,
		// caps_0x10); /*
		// glyph cache? */
		// this.sendGlyphCacheCaps(data);
		this.sendGlyphCacheCaps(data);

		data.markEnd();
		logger.debug("confirm active");
		// this.send(data, RDP_PDU_CONFIRM_ACTIVE);
		SecureUtils.send(data, sec_flags, session, context);
	}

	private void sendSynchronize(ChannelHandlerContext context) throws RdesktopException, IOException, CryptoException {
		RdpPacket_Localised data = this.initData(4);

		data.setLittleEndian16(1); // type
		data.setLittleEndian16(1002);

		data.markEnd();
		logger.debug("sync");
		this.sendData(context, data, RDP_DATA_PDU_SYNCHRONISE);
	}

	private void sendControl(ChannelHandlerContext context, int action) throws RdesktopException, IOException,
			CryptoException {

		RdpPacket_Localised data = this.initData(8);

		data.setLittleEndian16(action);
		data.setLittleEndian16(0); // userid
		data.setLittleEndian32(0); // control id

		data.markEnd();
		logger.debug("control");
		this.sendData(context, data, RDP_DATA_PDU_CONTROL);
	}

	// private RdpPacket_Localised receive(int[] type) throws Exception {
	// int length = 0;
	// logger.debug("receive data");
	// if ((this.session.getStream() == null) || (this.session.getNext_packet()
	// >= this.session.getStream().getEnd())) {
	// this.session.setStream(Secure.receive());
	// if (session.getStream() == null)
	// return null;
	// this.session.setNext_packet(this.session.getStream().getPosition());
	// } else {
	// this.session.getStream().setPosition(this.session.getNext_packet());
	// }
	// length = this.session.getStream().getLittleEndian16();
	//
	// /* 32k packets are really 8, keepalive fix - rdesktop 1.2.0 */
	// if (length == 0x8000) {
	// logger.warn("32k packet keepalive fix");
	// this.session.setNext_packet(this.session.getNext_packet() + 8);
	// type[0] = 0;
	// return session.getStream();
	// }
	// type[0] = this.session.getStream().getLittleEndian16() & 0xf;
	// if (session.getStream().getPosition() != session.getStream().getEnd()) {
	// session.getStream().incrementPosition(2);
	// }
	//
	// session.setNext_packet(this.session.getNext_packet() + length);
	// return session.getStream();
	// }

	private boolean processData(ChannelHandlerContext context, RdpPacket_Localised data, int[] ext_disc_reason)
			throws RdesktopException, OrderException {
		@SuppressWarnings("unused")
		int data_type, ctype, clen, len, roff, rlen;
		data_type = 0;

		data.incrementPosition(6); // skip shareid, pad, streamid
		len = data.getLittleEndian16();
		data_type = data.get8();
		ctype = data.get8(); // compression type
		clen = data.getLittleEndian16(); // compression length
		clen -= 18;

		logger.debug("data type? {}", data_type);
		switch (data_type) {

		case (RDP_DATA_PDU_UPDATE):
			// logger.debug("Rdp.RDP_DATA_PDU_UPDATE");
			this.processUpdate(context, data);
			break;

		case RDP_DATA_PDU_CONTROL:
			logger.debug(("Received Control PDU\n"));
			break;

		case RDP_DATA_PDU_SYNCHRONISE:
			logger.debug(("Received Sync PDU\n"));
			break;

		case (RDP_DATA_PDU_POINTER):
			logger.debug("Received pointer PDU");
			this.processPointer(data);
			break;
		case (RDP_DATA_PDU_BELL):
			logger.debug("Received bell PDU");
			// Toolkit tx = Toolkit.getDefaultToolkit();
			// tx.beep();
			break;
		case (RDP_DATA_PDU_LOGON):
			logger.debug("User logged on");
			// RdesktopSwing.loggedon = true;
			break;
		case RDP_DATA_PDU_DISCONNECT:
			/*
			 * Normally received when user logs out or disconnects from a
			 * console session on Windows XP and 2003 Server
			 */
			ext_disc_reason[0] = processDisconnectPdu(data);
			logger.info("Received disconnect PDU");
			// if ( ext_disc_reason[0] > 0) {
			// return true;
			// }
			break;

		default:
			logger.warn("Unimplemented Data PDU type " + data_type);

		}
		return false;
	}

	public void
			sendInput(ChannelHandlerContext context, int time, int message_type, int device_flags, int param1, int param2)
					throws RdesktopException, IOException, CryptoException {
		RdpPacket_Localised data = null;
		data = this.initData(16);

		data.setLittleEndian16(1); /* number of events */
		data.setLittleEndian16(0); /* pad */

		data.setLittleEndian32(time);
		data.setLittleEndian16(message_type);
		data.setLittleEndian16(device_flags);
		data.setLittleEndian16(param1);
		data.setLittleEndian16(param2);

		data.markEnd();
		// logger.info("input");
		// if(logger.isInfoEnabled()) logger.info(data);

		this.sendData(context, data, RDP_DATA_PDU_INPUT);
	}

	// private void sendFonts(int seq) throws RdesktopException, IOException,
	// CryptoException {
	//
	// RdpPacket_Localised data = this.initData(8);
	//
	// data.setLittleEndian16(0); /* number of fonts */
	// data.setLittleEndian16(0x3e); /* unknown */
	// data.setLittleEndian16(seq); /* unknown */
	// data.setLittleEndian16(0x32); /* entry size */
	//
	// data.markEnd();
	// logger.debug("fonts");
	// this.sendData(data, RDP_DATA_PDU_FONT2);
	// }

	private void sendGeneralCaps(RdpPacket_Localised data) {
		logger.debug("isUse_rdp5: {}", session.getOptions().isUse_rdp5());
		data.setLittleEndian16(RDP_CAPSET_GENERAL);
		data.setLittleEndian16(RDP_CAPLEN_GENERAL);

		data.setLittleEndian16(1); /* OS major type */
		data.setLittleEndian16(3); /* OS minor type */
		data.setLittleEndian16(0x200); /* Protocol version */
		data.setLittleEndian16(0); /* Compression types */
		data.setLittleEndian16(0); /* Pad */

		data.setLittleEndian16(0x40d);
		// data.setLittleEndian16(session.getOptions().isUse_rdp5() ? 0x1d04 :
		// 0); // this
		// seems
		/*
		 * Pad, according to T.128. 0x40d seems to trigger the server to start
		 * sending RDP5 packets. However, the value is 0x1d04 with W2KTSK and
		 * NT4MS. Hmm.. Anyway, thankyou, Microsoft, for sending such
		 * information in a padding field..
		 */
		data.setLittleEndian16(0); /* Update capability */
		data.setLittleEndian16(0); /* Remote unshare capability */
		data.setLittleEndian16(0); /* Compression level */
		data.setLittleEndian16(0); /* Pad */
	}

	private void sendBitmapCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_BITMAP);
		data.setLittleEndian16(RDP_CAPLEN_BITMAP);

		logger.debug("server_bpp:{}", session.getOptions().getServer_bpp());
		data.setLittleEndian16(session.getOptions().getServer_bpp()); /*
																	 * Preferred
																	 * BPP
																	 */
		data.setLittleEndian16(1); /* Receive 1 BPP */
		data.setLittleEndian16(1); /* Receive 4 BPP */
		data.setLittleEndian16(1); /* Receive 8 BPP */
		data.setLittleEndian16(session.getOptions().getWidth()); /* Desktop width */
		data.setLittleEndian16(session.getOptions().getHeight()); /*
																 * Desktop
																 * height
																 */
		data.setLittleEndian16(0); /* Pad */
		data.setLittleEndian16(1); /* Allow resize */
		logger.debug("isBitmap_compression:{}", session.getOptions().isBitmap_compression());
		data.setLittleEndian16(session.getOptions().isBitmap_compression() ? 1 : 0); /*
																					 * Support
																					 * compression
																					 */
		data.setLittleEndian16(0); /* Unknown */
		data.setLittleEndian16(1); /* Unknown */
		data.setLittleEndian16(0); /* Pad */
	}

	private void sendOrderCaps(RdpPacket_Localised data) {

		byte[] order_caps = new byte[32];
		order_caps[0] = 0; /* dest blt */
		order_caps[1] = 1; /* pat blt */// nb no rectangle orders if this is 0
		order_caps[2] = 1; /* screen blt */
		order_caps[3] = 0;// (byte) (session.getOptions().isBitmap_caching() ? 1
							// : 0); /* memblt */
		order_caps[4] = 0; /* triblt */
		order_caps[7] = 0; /* DRAWNINEGRID */
		order_caps[8] = 1; /* line */
		order_caps[9] = 0; /* line */
		order_caps[10] = 1; /* rect */
		order_caps[11] = 0;// (Constants.desktop_save ? 1 : 0); /* desksave */
		order_caps[13] = 0; /* memblt */
		order_caps[14] = 0; /* triblt */
		order_caps[15] = 0; /* MULTIDSTBLT_INDEX */
		order_caps[16] = 0; /* MULTIPATBLT_INDEX */
		order_caps[17] = 0; /* MULTISCRBLT_INDEX */
		order_caps[18] = 1; /* MULTISCRBLT_INDEX */
		order_caps[19] = 1; /* MULTISCRBLT_INDEX */
		order_caps[20] = 0;// (byte)
							// (session.getOptions().isPolygon_ellipse_orders()
							// ? 1 : 0); /* polygon */
		order_caps[21] = 0;// (byte)
							// (session.getOptions().isPolygon_ellipse_orders()
							// ? 1 : 0); /* polygon2 */
		order_caps[22] = 1; /* polyline */
		order_caps[24] = 1; /* polyline */
		order_caps[25] = 0;// (byte)
							// (session.getOptions().isPolygon_ellipse_orders()
							// ? 1 : 0); /* ellipse */
		order_caps[26] = 0;// (byte)
							// (session.getOptions().isPolygon_ellipse_orders()
							// ? 1 : 0); /* ellipse2 */
		order_caps[27] = 1; /* text2 */
		data.setLittleEndian16(RDP_CAPSET_ORDER);
		data.setLittleEndian16(RDP_CAPLEN_ORDER);

		data.incrementPosition(20); /* Terminal desc, pad */
		data.setLittleEndian16(1); /* Cache X granularity */
		data.setLittleEndian16(20); /* Cache Y granularity */
		data.setLittleEndian16(0); /* Pad */
		data.setLittleEndian16(1); /* Max order level */
		data.setLittleEndian16(0); /* Number of fonts */
		// data.setLittleEndian16(0x147);
		data.setLittleEndian16(0x2a); /* Capability flags */
		data.copyFromByteArray(order_caps, 0, data.getPosition(), 32); /*
																		 * Orders
																		 * supported
																		 */
		data.incrementPosition(32);
		// data.setLittleEndian16(0x6a1); /* Text capability flags */
		data.setLittleEndian16(0); /* Text capability flags */
		// data.incrementPosition(6); /* Pad */
		data.setLittleEndian16(0);
		data.setLittleEndian32(0);
		data.setLittleEndian32(230400); /*
										 * Desktop cache size
										 */
		// data.setLittleEndian32(0); /* Unknown */
		// data.setLittleEndian32(0); /* Unknown */
		// data.setLittleEndian32(0x4e4);
		data.setLittleEndian16(0);
		data.setLittleEndian16(0);
		data.setLittleEndian16(0);
		data.setLittleEndian16(0);
	}

	private void sendBitmapcacheCaps(RdpPacket_Localised data) {

		// data.setLittleEndian16(RDP_CAPSET_BMPCACHE);
		// data.setLittleEndian16(RDP_CAPLEN_BMPCACHE);
		// data.setLittleEndian32(0);
		// data.setLittleEndian32(0);
		// data.setLittleEndian32(0);
		// data.setLittleEndian32(0);
		// data.setLittleEndian32(0);
		// data.setLittleEndian32(0);
		// data.setLittleEndian16(200);
		// int bpp = (16 + 7) / 8;
		// int size = bpp * 256;
		// data.setLittleEndian16(size);
		// size = bpp * 1024;
		// data.setLittleEndian16(600);
		// data.setLittleEndian16(size);
		// size = bpp * 4096;
		// data.setLittleEndian16(1000);
		// data.setLittleEndian16(size);

		data.incrementPosition(24); /* unused */
		data.setLittleEndian16(0x258); /* entries */
		data.setLittleEndian16(0x100); /* max cell size */
		data.setLittleEndian16(0x12c); /* entries */
		data.setLittleEndian16(0x400); /* max cell size */
		data.setLittleEndian16(0x106); /* entries */
		data.setLittleEndian16(0x1000); /* max cell size */
	}

	/* Output bitmap cache v2 capability set */
	private void sendBitmapcache2Caps(RdpPacket_Localised data) {
		data.setLittleEndian16(RDP_CAPSET_BMPCACHE2); // out_uint16_le(s,
														// RDP_CAPSET_BMPCACHE2);
		data.setLittleEndian16(RDP_CAPLEN_BMPCACHE2); // out_uint16_le(s,
														// RDP_CAPLEN_BMPCACHE2);

		logger.debug("isPersistent_bitmap_caching?{}", session.getOptions().isPersistent_bitmap_caching());
		data.setLittleEndian16(session.getOptions().isPersistent_bitmap_caching() ? 2 : 0); /* version */

		data.setBigEndian16(3); /* number of caches in this set */

		/* max cell size for cache 0 is 16x16, 1 = 32x32, 2 = 64x64, etc */
		data.setLittleEndian32(BMPCACHE2_C0_CELLS); // out_uint32_le(s,
													// BMPCACHE2_C0_CELLS);
		data.setLittleEndian32(BMPCACHE2_C1_CELLS); // out_uint32_le(s,
													// BMPCACHE2_C1_CELLS);

		// data.setLittleEndian32(PstCache.pstcache_init(2) ?
		// (BMPCACHE2_NUM_PSTCELLS | BMPCACHE2_FLAG_PERSIST) :
		// BMPCACHE2_C2_CELLS);

		if (session.getPstCache().pstcache_init(2)) {
			logger.info("Persistent cache initialized");
			data.setLittleEndian32(BMPCACHE2_NUM_PSTCELLS | BMPCACHE2_FLAG_PERSIST);
		} else {
			logger.info("Persistent cache not initialized");
			data.setLittleEndian32(BMPCACHE2_C2_CELLS);
		}
		data.incrementPosition(20); // out_uint8s(s, 20); /* other bitmap caches
									// not used */
	}

	private void sendColorcacheCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_COLCACHE);
		data.setLittleEndian16(RDP_CAPLEN_COLCACHE);

		data.setLittleEndian16(6); /* cache size */
		data.setLittleEndian16(0); /* pad */
	}

	private void sendActivateCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_ACTIVATE);
		data.setLittleEndian16(RDP_CAPLEN_ACTIVATE);

		data.setLittleEndian16(0); /* Help key */
		data.setLittleEndian16(0); /* Help index key */
		data.setLittleEndian16(0); /* Extended help key */
		data.setLittleEndian16(0); /* Window activate */
	}

	private void sendControlCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_CONTROL);
		data.setLittleEndian16(RDP_CAPLEN_CONTROL);

		data.setLittleEndian16(0); /* Control capabilities */
		data.setLittleEndian16(0); /* Remote detach */
		data.setLittleEndian16(2); /* Control interest */
		data.setLittleEndian16(2); /* Detach interest */
	}

	private void sendPointerCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_POINTER);
		data.setLittleEndian16(RDP_CAPLEN_POINTER);

		data.setLittleEndian16(0); /* Colour pointer */
		data.setLittleEndian16(20); /* Cache size */
	}

	private void sendShareCaps(RdpPacket_Localised data) {

		data.setLittleEndian16(RDP_CAPSET_SHARE);
		data.setLittleEndian16(RDP_CAPLEN_SHARE);

		data.setLittleEndian16(0); /* userid */
		data.setLittleEndian16(0); /* pad */
	}

	private void sendGlyphCacheCaps(RdpPacket_Localised data) {
		logger.debug("send glyph cache");
		int length = 0x34;
		byte[] caps = new byte[] {
				// Glyph Cache (40 bytes)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x04, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x04, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x08, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x08, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x10, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x20, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x40, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x80, (byte) 0x00, // CacheMaximumCellSize: 4 (LE)

				(byte) 0xfe, (byte) 0x00, // CacheEntries: 254 (LE)

				(byte) 0x00, (byte) 0x01, // CacheMaximumCellSize: 4 (LE)

				(byte) 0x40, (byte) 0x00, // CacheEntries: 64 (LE)

				(byte) 0x00, (byte) 0x08, // CacheMaximumCellSize: 2048 (LE)

				// FragCache

				(byte) 0x00, (byte) 0x01, // CacheEntries: 256 (LE)

				(byte) 0x00, (byte) 0x01, // CacheMaximumCellSize: 256 (LE)

				//

				(byte) 0x00, (byte) 0x00, // GlyphSupportLevel:
											// GLYPH_SUPPORT_NONE (0x0,

				// LE)

				(byte) 0x00, (byte) 0x00, // Padding 2 bytes
		};

		data.setLittleEndian16(0x10 /* RDP_CAPSET_UNKNOWN */);
		data.setLittleEndian16(length /* 0x58 */);

		data.copyFromByteArray(caps, 0, data.getPosition(), /* RDP_CAPLEN_UNKNOWN */
				0x34 - 4);
		data.incrementPosition(/* RDP_CAPLEN_UNKNOWN */length - 4);
	}

	private void sendUnknownCaps(RdpPacket_Localised data, int id, int length, byte[] caps) {

		data.setLittleEndian16(id /* RDP_CAPSET_UNKNOWN */);
		data.setLittleEndian16(length /* 0x58 */);

		data.copyFromByteArray(caps, 0, data.getPosition(), /* RDP_CAPLEN_UNKNOWN */
				length - 4);
		data.incrementPosition(/* RDP_CAPLEN_UNKNOWN */length - 4);
	}

	private RdpPacket_Localised initData(int size) throws RdesktopException {
		RdpPacket_Localised buffer = null;

		buffer = SecureUtils.init(Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0, size + 18, session);
		buffer.pushLayer(RdpPacket_Localised.RDP_HEADER, 18);
		// buffer.setHeader(RdpPacket_Localised.RDP_HEADER);
		// buffer.incrementPosition(18);
		// buffer.setStart(buffer.getPosition());
		return buffer;
	}

	private void processUpdate(ChannelHandlerContext context, RdpPacket_Localised data) throws OrderException,
			RdesktopException {
		int update_type = 0;

		update_type = data.getLittleEndian16();

		switch (update_type) {

		case (RDP_UPDATE_ORDERS):
			data.incrementPosition(2); // pad
			int n_orders = data.getLittleEndian16();
			data.incrementPosition(2); // pad
			session.getOrders().processOrders(context, data, session.getNext_packet(), n_orders);
			break;
		case (RDP_UPDATE_BITMAP):
			RDPUtils.processBitmapUpdates(context, data, session);
			break;
		case (RDP_UPDATE_PALETTE):
			this.processPalette(data);
			break;
		case (RDP_UPDATE_SYNCHRONIZE):
			break;
		default:
			logger.warn("Unimplemented Update type " + update_type);
		}
	}

	private void processPointer(RdpPacket_Localised data) throws RdesktopException {
		int message_type = 0;
		// int x = 0, y = 0;

		message_type = data.getLittleEndian16();
		data.incrementPosition(2);
		logger.debug("pointer message type? {}", message_type);
		switch (message_type) {

		case (RDP_POINTER_MOVE):
			logger.debug("Rdp.RDP_POINTER_MOVE");
			// x =
			data.getLittleEndian16();
			// y =
			data.getLittleEndian16();

			// if (data.getPosition() <= data.getEnd()) {
			// session.getSurface().movePointer(x, y);
			// }
			break;

		case (RDP_POINTER_COLOR):
			process_colour_pointer_pdu(data);
			break;

		case (RDP_POINTER_CACHED):
			process_cached_pointer_pdu(data);
			break;

		case RDP_POINTER_SYSTEM:
			process_system_pointer_pdu(data);
			break;

		default:
			break;
		}
	}

	protected int processDisconnectPdu(RdpPacket_Localised data) {
		logger.debug("Received disconnect PDU");
		return data.getLittleEndian32();
	}

	private void process_system_pointer_pdu(RdpPacket_Localised data) {
		int system_pointer_type = 0;

		data.getLittleEndian16(system_pointer_type); // in_uint16(s,
		// system_pointer_type);
		switch (system_pointer_type) {
		case RDP_NULL_POINTER:
			logger.debug("RDP_NULL_POINTER");
			// session.getSurface().setCursor(null);
			// setSubCursor(null);
			break;

		default:
			logger.warn("Unimplemented system pointer message 0x" + Integer.toHexString(system_pointer_type));
			// unimpl("System pointer message 0x%x\n", system_pointer_type);
		}
	}

	// protected void processBitmapUpdates(ChannelHandlerContext context,
	// RdpPacket_Localised data) throws RdesktopException {
	// // logger.info("processBitmapUpdates");
	// int n_updates = 0;
	// int left = 0, top = 0, right = 0, bottom = 0, width = 0, height = 0;
	// // int cx = 0, cy = 0, size = 0;
	// int bitsperpixel = 0, compression = 0;
	// // int buffersize = 0;
	// byte[] pixel = null;
	//
	// int minX, minY, maxX, maxY;
	//
	// maxX = maxY = 0;
	// minX = session.getOptions().getWidth();
	// minY = session.getOptions().getHeight();
	//
	// n_updates = data.getLittleEndian16();
	//
	// for (int i = 0; i < n_updates; i++) {
	//
	// left = data.getLittleEndian16();
	// top = data.getLittleEndian16();
	// right = data.getLittleEndian16();
	// bottom = data.getLittleEndian16();
	// width = data.getLittleEndian16();
	// height = data.getLittleEndian16();
	// bitsperpixel = data.getLittleEndian16();
	// int Bpp = (bitsperpixel + 7) / 8;
	// compression = data.getLittleEndian16();
	// // buffersize =
	// data.getLittleEndian16();
	//
	// // cx = right - left + 1;
	// // cy = bottom - top + 1;
	//
	// if (minX > left)
	// minX = left;
	// if (minY > top)
	// minY = top;
	// if (maxX < right)
	// maxX = right;
	// if (maxY < bottom)
	// maxY = bottom;
	//
	// /* Server may limit bpp - this is how we find out */
	// if (session.getOptions().getServer_bpp() != bitsperpixel) {
	// logger.warn("Server limited colour depth to " + bitsperpixel + " bits");
	// session.getOptions().set_bpp(bitsperpixel);
	// }
	//
	// if (compression == 0) {
	// // logger.info("compression == 0");
	// pixel = new byte[width * height * Bpp];
	//
	// for (int y = 0; y < height; y++) {
	// data.copyToByteArray(pixel, (height - y - 1) * (width * Bpp),
	// data.getPosition(), width * Bpp);
	// data.incrementPosition(width * Bpp);
	// }
	//
	// // session.getSurface().displayImage(context,
	// // Bitmap.convertImage(session.getOptions(), pixel, Bpp), width,
	// // height, left, top, cx, cy);
	// continue;
	// }
	//
	// if ((compression & 0x400) != 0) {
	// // logger.info("compression & 0x400 != 0");
	// // size = buffersize;
	// } else {
	// // logger.info("compression & 0x400 == 0");
	// data.incrementPosition(2); // pad
	// // size =
	// data.getLittleEndian16();
	//
	// data.incrementPosition(4); // line size, final size
	//
	// }
	// if (Bpp == 1) {
	// // pixel = Bitmap.decompress(width, height, size, data, Bpp);
	// // if (pixel != null)
	// // session.getSurface().displayImage(context,
	// // Bitmap.convertImage(session.getOptions(), pixel, Bpp), width,
	// // height, left, top, cx, cy);
	// // else
	// // logger.warn("Could not decompress bitmap");
	// } else {
	//
	// if (session.getOptions().getBitmap_decompression_store() ==
	// Options.INTEGER_BITMAP_DECOMPRESSION) {
	// // int[] pixeli = Bitmap.decompressInt(session.getOptions(),
	// // width, height, size, data, Bpp);
	// // if (pixeli != null)
	// // session.getSurface().displayImage(context, pixeli, width,
	// // height, left, top, cx, cy);
	// // else
	// // logger.warn("Could not decompress bitmap");
	// } else if (session.getOptions().getBitmap_decompression_store() ==
	// Options.BUFFEREDIMAGE_BITMAP_DECOMPRESSION) {
	// // Image pix = Bitmap.decompressImg(session.getOptions(),
	// // context, width, height, size, data, Bpp, null);
	// // if (pix != null)
	// // session.getSurface().displayImage(pix, left, top);
	// // else
	// // logger.warn("Could not decompress bitmap");
	// } else {
	// // session.getSurface().displayCompressed(context, left,
	// // top, width, height, size, data, Bpp, null);
	// }
	// }
	// }
	// // session.getSurface().repaint(minX, minY, maxX - minX + 1, maxY - minY
	// // + 1);
	// }

	protected void processPalette(RdpPacket_Localised data) {
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

	protected void process_colour_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
		logger.debug("Rdp.RDP_POINTER_COLOR");
		// int x = 0, y = 0, width = 0, height = 0, cache_idx = 0;
		int masklen = 0, datalen = 0;
		byte[] mask = null, pixel = null;
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
		mask = new byte[masklen];
		pixel = new byte[datalen];
		data.copyToByteArray(pixel, 0, data.getPosition(), datalen);
		data.incrementPosition(datalen);
		data.copyToByteArray(mask, 0, data.getPosition(), masklen);
		data.incrementPosition(masklen);
		// cursor = session.getSurface().createCursor(x, y, width, height, mask,
		// pixel, cache_idx);
		// logger.info("Creating and setting cursor " + cache_idx);
		// session.getSurface().setCursor(cursor);
		// setSubCursor(cursor);
		// session.getCache().putCursor(cache_idx, cursor);
	}

	protected void process_cached_pointer_pdu(RdpPacket_Localised data) throws RdesktopException {
		logger.debug("Rdp.RDP_POINTER_CACHED");
		// int cache_idx =
		data.getLittleEndian16();
		// logger.info("Setting cursor "+cache_idx);
		// session.getSurface().setCursor(session.getCache().getCursor(cache_idx));
		// setSubCursor(session.getCache().getCursor(cache_idx));
	}

	// private void setSubCursor(Cursor cursor) {
	// session.getSurface().setCursor(cursor);
	// }

	private void sendData(ChannelHandlerContext context, RdpPacket_Localised data, int data_pdu_type)
			throws RdesktopException, IOException, CryptoException {

		RDPUtils.sendData(data, data_pdu_type, session, context);

		// CommunicationMonitor.lock(this);
		//
		// int length;
		//
		// data.setPosition(data.getHeader(RdpPacket_Localised.RDP_HEADER));
		// length = data.getEnd() - data.getPosition();
		//
		// data.setLittleEndian16(length);
		// data.setLittleEndian16(RDP_PDU_DATA | 0x10);
		// data.setLittleEndian16(SecureUtils.getUserID(session) + 1001);
		//
		// data.setLittleEndian32(session.getRdp_shareid());
		// data.set8(0); // pad
		// data.set8(1); // stream id
		// data.setLittleEndian16(length - 14);
		// data.set8(data_pdu_type);
		// data.set8(0); // compression type
		// data.setLittleEndian16(0); // compression length
		//
		// SecureLayer.send(data, Constants.encryption ? Secure.SEC_ENCRYPT :
		// 0);
		//
		// CommunicationMonitor.unlock(this);
	}
}
