/* Orders.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:31 $
 *
 * Copyright 2005, 2015005 Propero Limited
 *
 * Purpose: Encapsulates an RDP order
 */
package com.infinities.skyport.rdp.custom;

//import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.wsgate.PrimaryUtils;
import com.lixia.rdp.AltSecOrderState;
import com.lixia.rdp.Bitmap;
import com.lixia.rdp.Cache;
import com.lixia.rdp.Glyph;
import com.lixia.rdp.Options;
import com.lixia.rdp.OrderException;
import com.lixia.rdp.OrderState;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.orders.BoundsOrder;
import com.lixia.rdp.orders.Brush;
import com.lixia.rdp.orders.DeskSaveOrder;
import com.lixia.rdp.orders.DestBltOrder;
import com.lixia.rdp.orders.LineOrder;
import com.lixia.rdp.orders.MemBltOrder;
import com.lixia.rdp.orders.MultiOpaqueRectBltOrder;
import com.lixia.rdp.orders.PatBltOrder;
import com.lixia.rdp.orders.Pen;
import com.lixia.rdp.orders.PolyLineOrder;
import com.lixia.rdp.orders.RectangleOrder;
import com.lixia.rdp.orders.ScreenBltOrder;
import com.lixia.rdp.orders.Text2Order;
import com.lixia.rdp.orders.TriBltOrder;

public class CustomOrdersPanel {

	private static final Logger logger = LoggerFactory.getLogger(CustomOrdersPanel.class);

	private OrderState os = null;
	private AltSecOrderState altSecOs = null;

	// private CustomRdpBufferedImageCanvas surface = null;

	private Cache cache = null;

	/* RDP_BMPCACHE2_ORDER */
	private static final int ID_MASK = 0x0007;
	private static final int MODE_MASK = 0x0038;
	private static final int SQUARE = 0x0080;
	private static final int PERSIST = 0x0100;
	// private static final int FLAG_51_UNKNOWN = 0x0800;
	private static final int MODE_SHIFT = 3;
	private static final int LONG_FORMAT = 0x80;
	private static final int BUFSIZE_MASK = 0x3FFF; /* or 0x1FFF? */
	private static final int RDP_ORDER_STANDARD = 0x01;
	private static final int RDP_ORDER_SECONDARY = 0x02;
	private static final int RDP_ORDER_BOUNDS = 0x04;
	private static final int RDP_ORDER_CHANGE = 0x08;
	private static final int RDP_ORDER_DELTA = 0x10;
	private static final int RDP_ORDER_LASTBOUNDS = 0x20;
	private static final int RDP_ORDER_SMALL = 0x40;
	private static final int RDP_ORDER_TINY = 0x80;

	/* standard order types */
	private static final int RDP_ORDER_DESTBLT = 0;
	private static final int RDP_ORDER_PATBLT = 1;
	private static final int RDP_ORDER_SCREENBLT = 2;
	private static final int RDP_ORDER_LINE = 9;
	private static final int RDP_ORDER_RECT = 10;
	private static final int RDP_ORDER_DESKSAVE = 11;
	private static final int RDP_ORDER_MEMBLT = 13;
	private static final int RDP_ORDER_TRIBLT = 14;
	private static final int RDP_ORDER_MLUTI_OPAQUE_RECT = 18;
	private static final int RDP_ORDER_POLYLINE = 22;
	private static final int RDP_ORDER_TEXT2 = 27;
	private int rect_colour;

	/* secondary order types */
	private static final int RDP_ORDER_RAW_BMPCACHE = 0;
	private static final int RDP_ORDER_COLCACHE = 1;
	private static final int RDP_ORDER_BMPCACHE = 2;
	private static final int RDP_ORDER_FONTCACHE = 3;
	private static final int RDP_ORDER_RAW_BMPCACHE2 = 4;
	private static final int RDP_ORDER_BMPCACHE2 = 5;
	// private static final int MIX_TRANSPARENT = 0;
	// private static final int MIX_OPAQUE = 1;
	// private static final int TEXT2_VERTICAL = 0x04;
	// private static final int TEXT2_IMPLICIT_X = 0x20;

	// private static final int ORDER_TYPE_CREATE_OFFSCREEN_BITMAP = 0x01;
	// private static final int ORDER_TYPE_SWITCH_SURFACE = 0x00;
	// private static final int ORDER_TYPE_CREATE_NINE_GRID_BITMAP = 0x04;
	// private static final int ORDER_TYPE_FRAME_MARKER = 0x0d;
	// private static final int ORDER_TYPE_STREAM_BITMAP_FIRST = 0x02;
	// private static final int ORDER_TYPE_STREAM_BITMAP_NEXT = 0x03;
	// private static final int ORDER_TYPE_GDIPLUS_FIRST = 0x05;
	// private static final int ORDER_TYPE_GDIPLUS_NEXT = 0x06;
	// private static final int ORDER_TYPE_GDIPLUS_END = 0x07;
	// private static final int ORDER_TYPE_GDIPLUS_CACHE_FIRST = 0x08;
	// private static final int ORDER_TYPE_GDIPLUS_CACHE_NEXT = 0x09;
	// private static final int ORDER_TYPE_GDIPLUS_CACHE_END = 0x0A;
	// private static final int ORDER_TYPE_WINDOW = 0x0B;
	// private static final int ORDER_TYPE_COMPDESK_FIRST = 0x0C;
	// private static final int STREAM_BITMAP_V2 = 0x04;
	private Options options;
	private int top = 0, right = 0, bottom = 0, left = 0;


	public CustomOrdersPanel(Options options) {
		os = new OrderState();
		altSecOs = new AltSecOrderState();
		this.options = options;
		this.bottom = options.getHeight();
		this.right = options.getWidth();
	}

	public void resetOrderState() {
		this.os.reset();
		this.altSecOs.reset();
		os.setOrderType(RDP_ORDER_PATBLT);
	}

	private int inPresent(RdpPacket_Localised data, int flags, int size) {
		int present = 0;
		int bits = 0;
		int i = 0;

		if ((flags & RDP_ORDER_SMALL) != 0) {
			size--;
		}

		if ((flags & RDP_ORDER_TINY) != 0) {

			if (size < 2) {
				size = 0;
			} else {
				size -= 2;
			}
		}

		for (i = 0; i < size; i++) {
			bits = data.get8();
			present |= (bits << (i * 8));
		}
		return present;
	}

	/**
	 * Process a set of orders sent by the server
	 * 
	 * @param data
	 *            Packet packet containing orders
	 * @param next_packet
	 *            Offset of end of this packet (start of next)
	 * @param n_orders
	 *            Number of orders sent in this packet
	 * @throws OrderException
	 * @throws RdesktopException
	 */
	public void processOrders(ChannelHandlerContext context, RdpPacket_Localised data, int next_packet, int n_orders)
			throws OrderException, RdesktopException {

		int present = 0;
		// int n_orders = 0;
		int order_flags = 0, order_type = 0;
		int size = 0, processed = 0;
		boolean delta;
		logger.debug("n_orders:{}", n_orders);
		while (processed < n_orders) {

			order_flags = data.get8();
			logger.debug("flags:{}", order_flags);
			logger.debug("size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
			if ((order_flags & RDP_ORDER_STANDARD) == 0) {
				// processRecvAltsecOrder(data, order_flags);
				throw new OrderException("Order parsing failed!");
			} else if ((order_flags & RDP_ORDER_SECONDARY) != 0) {
				this.processSecondaryOrders(data);
			} else {

				if ((order_flags & RDP_ORDER_CHANGE) != 0) {
					os.setOrderType(data.get8());
				}
				logger.debug("size:{}, position: {}",
						new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });

				switch (os.getOrderType()) {
				case RDP_ORDER_TRIBLT:
				case RDP_ORDER_TEXT2:
					size = 3;
					break;

				case RDP_ORDER_PATBLT:
				case RDP_ORDER_MEMBLT:
				case RDP_ORDER_LINE:
				case RDP_ORDER_MLUTI_OPAQUE_RECT:
					size = 2;
					break;

				default:
					size = 1;
				}

				present = this.inPresent(data, order_flags, size);
				logger.debug("size:{}, position: {}",
						new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });

				if ((order_flags & RDP_ORDER_BOUNDS) != 0) {

					if ((order_flags & RDP_ORDER_LASTBOUNDS) == 0) {
						logger.debug("flags:{}", order_flags);
						this.parseBounds(data, os.getBounds());
					}
					setClip(os.getBounds());
//					UpdateUtils.sendSetBounds(context, os.getBounds());

				}

				delta = ((order_flags & RDP_ORDER_DELTA) != 0);

				logger.debug("OrderType:{}", os.getOrderType());
				logger.debug("size:{}, position: {}",
						new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
				switch (os.getOrderType()) {
				case RDP_ORDER_DESTBLT:
					logger.debug("DestBlt Order");
					this.processDestBlt(context, data, os.getDestBlt(), present, delta);
					break;

				case RDP_ORDER_PATBLT:
					logger.debug("PatBlt Order");
					this.processPatBlt(context, data, os.getPatBlt(), present, delta);
					break;

				case RDP_ORDER_SCREENBLT:
					logger.debug("ScreenBlt Order");
					this.processScreenBlt(context, data, os.getScreenBlt(), present, delta);
					break;

				case RDP_ORDER_LINE:
					logger.debug("Line Order");
					this.processLine(context, data, os.getLine(), present, delta);
					break;

				case RDP_ORDER_RECT:
					logger.debug("Rectangle Order");
					this.processRectangle(context, data, os.getRectangle(), present, delta);
					break;

				case RDP_ORDER_DESKSAVE:
					logger.debug("Desksave!");
					this.processDeskSave(context, data, os.getDeskSave(), present, delta);
					break;

				case RDP_ORDER_MEMBLT:
					logger.debug("MemBlt Order");
					this.processMemBlt(context, data, os.getMemBlt(), present, delta);
					break;

				case RDP_ORDER_TRIBLT:
					logger.debug("TriBlt Order");
					this.processTriBlt(context, data, os.getTriBlt(), present, delta);
					break;

				case RDP_ORDER_MLUTI_OPAQUE_RECT:
					logger.debug("Multi opaque Order");
					this.processMultiOpaqueRectBlt(context, data, os.getMultiOpaqueRectBlt(), present, delta);
					break;

				case RDP_ORDER_POLYLINE:
					logger.debug("Polyline Order");
					this.processPolyLine(context, data, os.getPolyLine(), present, delta);
					break;

				case RDP_ORDER_TEXT2:
					logger.debug("Text2 Order");
					this.processText2(context, data, os.getText2(), present, delta);
					break;

				default:
					logger.warn("Unimplemented Order type " + order_type);
					return;
				}
				logger.debug("size:{}, position: {}",
						new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
				// logger.debug("position:{}, next:{}", new Object[] {
				// data.getPosition(), next_packet });

				if ((order_flags & RDP_ORDER_BOUNDS) != 0) {
					resetClip();
//					UpdateUtils.sendSetBounds(context, null);
					logger.debug("Reset clip");
				}
			}

			processed++;
		}
		if (data.getPosition() != next_packet) {
			throw new OrderException("End not reached!");
		}
	}

	// private void processRecvAltsecOrder(RdpPacket_Localised data, int
	// order_flags) {
	// logger.debug("order_flags:{}", order_flags);
	// order_flags >>= 2;
	// logger.debug("order_flags>>2:{}", order_flags);
	// int orderType = order_flags;
	// logger.debug("recv_altec_order:{}", orderType);
	//
	// switch (orderType) {
	// case ORDER_TYPE_CREATE_OFFSCREEN_BITMAP:
	// updateReadCreateOffscreenBitmapOrder(data,
	// altSecOs.getCreateOffscreenBitmapOrder());
	// break;
	// case ORDER_TYPE_SWITCH_SURFACE:
	// updateReadSwitchSurfaceOrder(data, altSecOs.getSwitchSurfaceOrder());
	// break;
	// case ORDER_TYPE_CREATE_NINE_GRID_BITMAP:
	// updateReadCreateNineGridBitmap(data,
	// altSecOs.getCreateNineGridBitmapOrder());
	// break;
	// case ORDER_TYPE_FRAME_MARKER:
	// updateReadFrameMarkerOrder(data, altSecOs.getFrameMarkerOrder());
	// break;
	//
	// case ORDER_TYPE_STREAM_BITMAP_FIRST:
	// updateReadStreamBitmapFirstOrder(data,
	// altSecOs.getStreamBitmapFirstOrder());
	// break;
	//
	// case ORDER_TYPE_STREAM_BITMAP_NEXT:
	// updateReadStreamBitmapNextOrder(data,
	// altSecOs.getStreamBitmapNextOrder());
	// break;
	//
	// case ORDER_TYPE_GDIPLUS_FIRST:
	// updateReadDrawGdiplusFirstOrder(data,
	// altSecOs.getDrawGdiplusFirstOrder());
	// break;
	//
	// case ORDER_TYPE_GDIPLUS_NEXT:
	// updateReadDrawGdiplusNextOrder(data, altSecOs.getDrawGdiplusNextOrder());
	// break;
	//
	// case ORDER_TYPE_GDIPLUS_END:
	// updateReadDrawGdiplusEndOrder(data, altSecOs.getDrawGdiplusEndOrder());
	// break;
	// case ORDER_TYPE_GDIPLUS_CACHE_FIRST:
	// updateReadDrawGdiCacheFirstOrder(data,
	// altSecOs.getDrawGdiCacheFirstOrder());
	// break;
	//
	// case ORDER_TYPE_GDIPLUS_CACHE_NEXT:
	// updateReadDrawGdiplusCacheNextOrder(data,
	// altSecOs.getDrawGdiCacheNextOrder());
	// break;
	//
	// case ORDER_TYPE_GDIPLUS_CACHE_END:
	// updateReadDrawGdiCacheEndOrder(data, altSecOs.getDrawGdiCacheEndOrder());
	// break;
	// case ORDER_TYPE_WINDOW:
	// logger.debug("window order");
	// updateRecvAltsecWindowOrder(data);
	// return;
	// case ORDER_TYPE_COMPDESK_FIRST:
	// break;
	// default:
	// logger.warn("unimplemented command:" + orderType);
	// break;
	//
	// }
	// logger.debug("position after RecvAltsecOrder:{}", data.getPosition());
	// }
	//
	// private void updateRecvAltsecWindowOrder(RdpPacket_Localised data) {
	//
	// }
	//
	// private void updateReadDrawGdiCacheEndOrder(RdpPacket_Localised data,
	// DrawGdiCacheEndOrder order) {
	// order.setFlags(data.get8());
	// order.setCacheType(data.getLittleEndian16());
	// order.setCacheIndex(data.getLittleEndian16());
	// order.setCbSize(data.getLittleEndian16());
	// order.setCbTotalSize(data.getLittleEndian32());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadDrawGdiplusCacheNextOrder(RdpPacket_Localised
	// data, DrawGdiCacheNextOrder order) {
	// order.setFlags(data.get8());
	// order.setCacheType(data.getLittleEndian16());
	// order.setCacheIndex(data.getLittleEndian16());
	// order.setCbSize(data.getLittleEndian16());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadDrawGdiCacheFirstOrder(RdpPacket_Localised data,
	// DrawGdiCacheFirstOrder order) {
	// order.setFlags(data.get8());
	// order.setCacheType(data.getLittleEndian16());
	// order.setCacheIndex(data.getLittleEndian16());
	// order.setCbSize(data.getLittleEndian16());
	// order.setCbTotalSize(data.getLittleEndian32());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadDrawGdiplusEndOrder(RdpPacket_Localised data,
	// DrawGdiplusEndOrder order) {
	// data.get8();
	// order.setCbSize(data.getLittleEndian16());
	// order.setCbTotalSize(data.getLittleEndian32());
	// order.setCbTotalEmfSize(data.getLittleEndian32());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadDrawGdiplusNextOrder(RdpPacket_Localised data,
	// DrawGdiplusNextOrder order) {
	// data.get8();
	// order.setCbSize(data.getLittleEndian16());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	//
	// }
	//
	// private void updateReadDrawGdiplusFirstOrder(RdpPacket_Localised data,
	// DrawGdiplusFirstOrder order) {
	// data.get8();
	// order.setCbSize(data.getLittleEndian16());
	// order.setCbTotalSize(data.getLittleEndian32());
	// order.setCbTotalEmfSize(data.getLittleEndian32());
	// for (int i = 0; i < order.getCbSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadStreamBitmapNextOrder(RdpPacket_Localised data,
	// StreamBitmapNextOrder order) {
	// order.setBitmapFlags(data.get8());
	// order.setBitmapType(data.getLittleEndian16());
	// order.setBitmapBlockSize(data.getLittleEndian16());
	// for (int i = 0; i < order.getBitmapBlockSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadStreamBitmapFirstOrder(RdpPacket_Localised data,
	// StreamBitmapFirstOrder order) {
	// order.setBitmapFlags(data.get8());
	// order.setBitmapBpp(data.get8());
	// order.setBitmapType(data.getLittleEndian16());
	// order.setBitmapWidth(data.getLittleEndian16());
	// order.setBitmapHeight(data.getLittleEndian16());
	//
	// if ((order.getBitmapFlags() & STREAM_BITMAP_V2) != 0) {
	// order.setBitmapSize(data.getLittleEndian32());
	// } else {
	// order.setBitmapSize(data.getLittleEndian16());
	// }
	//
	// order.setBitmapBlockSize(data.getLittleEndian16());
	// for (int i = 0; i < order.getBitmapBlockSize(); i++) {
	// data.get8();
	// }
	// }
	//
	// private void updateReadFrameMarkerOrder(RdpPacket_Localised data,
	// FrameMarkerOrder order) {
	// order.setAction(data.getLittleEndian32());
	// }
	//
	// private void updateReadCreateNineGridBitmap(RdpPacket_Localised data,
	// CreateNineGridBitmapOrder order) {
	// order.setBitmapBpp(data.get8());
	// order.setBitmapId(data.getLittleEndian16());
	//
	// NineGridBitmapInfo nineGridInfo = order.getNineGridInfo();
	// nineGridInfo.setFlFlags(data.getLittleEndian32());
	// nineGridInfo.setUlLeftWidth(data.getLittleEndian16());
	// nineGridInfo.setUlRightWidth(data.getLittleEndian16());
	// nineGridInfo.setUlTopHeight(data.getLittleEndian16());
	// nineGridInfo.setUlBottomHeight(data.getLittleEndian16());
	// nineGridInfo.setCrTransparent(updateReadColorRef(data));
	// }
	//
	// private int updateReadColorRef(RdpPacket_Localised data) {
	// int b = data.get8();
	// int color = b;
	// b = data.get8();
	// color |= (b << 8);
	// b = data.get8();
	// color |= (b << 16);
	// data.get8();
	//
	// return color;
	// }
	//
	// private void updateReadSwitchSurfaceOrder(RdpPacket_Localised data,
	// SwitchSurfaceOrder order) {
	// order.setBitmapId(data.getLittleEndian16());
	// }
	//
	// private void updateReadCreateOffscreenBitmapOrder(RdpPacket_Localised
	// data, CreateOffscreenBitmapOrder order) {
	// int flags = data.getLittleEndian16();
	// order.setId(flags & 0x7FFF);
	// boolean deleteListPresent = (flags & 0x8000) != 0 ? true : false;
	// order.setCX(data.getLittleEndian16());
	// order.setCY(data.getLittleEndian16());
	// OffscreenDeleteList deleteList = order.getDeleteList();
	// logger.debug("position before deleteListPresent:{}, deleteListPresent:{}",
	// new Object[] { data.getPosition(),
	// deleteListPresent });
	// if (deleteListPresent) {
	// deleteList.setCIndices(data.getLittleEndian16());
	// if (deleteList.getCIndices() > deleteList.getSIndices()) {
	// int[] newIndices = new int[deleteList.getSIndices() * 2];
	// deleteList.setSIndices(deleteList.getCIndices());
	// deleteList.setIndices(newIndices);
	// }
	//
	// for (int i = 0; i < deleteList.getCIndices(); i++) {
	// deleteList.getIndices()[i] = data.getLittleEndian16();
	// }
	// } else {
	// deleteList.setCIndices(0);
	// }
	// logger.debug("position after deleteListPresent:{}", data.getPosition());
	// }

	private int ROP2_S(int rop3) {
		return (rop3 & 0x0f);
	}

	private int ROP2_P(int rop3) {
		return ((rop3 & 0x3) | ((rop3 & 0x30) >> 2));
	}

	/**
	 * Register an RdesktopJPanel with this Orders object. This surface is where
	 * all drawing orders will be carried out.
	 * 
	 * @param _canvas
	 *            Surface to register
	 */
	// public void registerDrawingSurface(CustomRdpBufferedImageCanvas _canvas)
	// {
	// this.surface = _canvas;
	// _canvas.registerCache(getCache());
	// }

	/**
	 * Handle secondary, or caching, orders
	 * 
	 * @param data
	 *            Packet containing secondary order
	 * @throws OrderException
	 * @throws RdesktopException
	 */
	private void processSecondaryOrders(RdpPacket_Localised data) throws OrderException, RdesktopException {
		int length = 0;
		int type = 0;
		int flags = 0;
		int next_order = 0;

		length = data.getLittleEndian16();
		flags = data.getLittleEndian16();
		type = data.get8();

		next_order = data.getPosition() + length + 7;

		logger.debug("process secondary orders type: {}", type);

		switch (type) {

		case RDP_ORDER_RAW_BMPCACHE:
			logger.debug("Raw BitmapCache Order");
			this.processRawBitmapCache(data);
			break;

		case RDP_ORDER_COLCACHE:
			logger.debug("Colorcache Order");
			this.processColorCache(data);
			break;

		case RDP_ORDER_BMPCACHE:
			logger.debug("Bitmapcache Order");
			this.processBitmapCache(data);
			break;

		case RDP_ORDER_FONTCACHE:
			logger.debug("Fontcache Order");
			this.processFontCache(data);
			break;

		case RDP_ORDER_RAW_BMPCACHE2:
			try {
				this.process_bmpcache2(data, flags, false);
			} catch (IOException e) {
				throw new RdesktopException(e.getMessage());
			} /* uncompressed */
			break;

		case RDP_ORDER_BMPCACHE2:
			try {
				this.process_bmpcache2(data, flags, true);
			} catch (IOException e) {
				throw new RdesktopException(e.getMessage());
			} /* compressed */
			break;

		default:
			logger.warn("Unimplemented 2ry Order type " + type);
		}

		data.setPosition(next_order);
	}

	/**
	 * Process a raw bitmap and store it in the bitmap cache
	 * 
	 * @param data
	 *            Packet containing raw bitmap data
	 * @throws RdesktopException
	 */
	private void processRawBitmapCache(RdpPacket_Localised data) throws RdesktopException {
		int cache_id = data.get8();
		data.incrementPosition(1); // pad
		int width = data.get8();
		int height = data.get8();
		int bpp = data.get8();
		logger.debug("1-bpp: " + bpp);
		int Bpp = (bpp + 7) / 8;
		int bufsize = data.getLittleEndian16();
		int cache_idx = data.getLittleEndian16();
		int pdata = data.getPosition();
		data.incrementPosition(bufsize);

		byte[] inverted = new byte[width * height * Bpp];
		int pinverted = (height - 1) * (width * Bpp);
		for (int y = 0; y < height; y++) {
			data.copyToByteArray(inverted, pinverted, pdata, width * Bpp);
			// data.copyToByteArray(inverted, (height - y - 1) * (width * Bpp),
			// y * (width * Bpp), width*Bpp);
			pinverted -= width * Bpp;
			pdata += width * Bpp;
		}

		// RawBitmap raw = new RawBitmap(bpp, bufsize, inverted, false, width,
		// height);
		// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
		getCache().putBitmap(cache_id, cache_idx,
				new Bitmap(options, Bitmap.convertImage(options, inverted, Bpp), width, height, 0, 0), 0);
	}

	/**
	 * Process and store details of a colour cache
	 * 
	 * @param data
	 *            Packet containing cache information
	 * @throws RdesktopException
	 */
	private void processColorCache(RdpPacket_Localised data) throws RdesktopException {
		byte[] palette = null;

		byte[] red = null;
		byte[] green = null;
		byte[] blue = null;
		int j = 0;

		// int cache_id =
		data.get8();
		int n_colors = data.getLittleEndian16(); // Number of Colors in
													// Palette

		palette = new byte[n_colors * 4];
		red = new byte[n_colors];
		green = new byte[n_colors];
		blue = new byte[n_colors];
		data.copyToByteArray(palette, 0, data.getPosition(), palette.length);
		data.incrementPosition(palette.length);
		for (int i = 0; i < n_colors; i++) {
			blue[i] = palette[j];
			green[i] = palette[j + 1];
			red[i] = palette[j + 2];
			// palette[j+3] is pad
			j += 4;
		}
		// IndexColorModel cm = new IndexColorModel(8, n_colors, red, green,
		// blue);
		// getCache().put_colourmap(cache_id, cm);
		// surface.registerPalette(cm);
	}

	/**
	 * Process a compressed bitmap and store in the bitmap cache
	 * 
	 * @param data
	 *            Packet containing compressed bitmap
	 * @throws RdesktopException
	 */
	private void processBitmapCache(RdpPacket_Localised data) throws RdesktopException {
		int bufsize, pad2, row_size, final_size, size;
		int pad1;

		bufsize = pad2 = row_size = final_size = size = 0;

		int cache_id = data.get8();
		pad1 = data.get8(); // pad
		int width = data.get8();
		int height = data.get8();
		int bpp = data.get8();
		logger.debug("2-bpp: " + bpp);
		int Bpp = (bpp + 7) / 8;
		bufsize = data.getLittleEndian16(); // bufsize
		int cache_idx = data.getLittleEndian16();

		/*
		 * data.incrementPosition(2); // pad int size =
		 * data.getLittleEndian16(); data.incrementPosition(4); // row_size,
		 * final_size
		 */

		if (options.isUse_rdp5()) {

			/* Begin compressedBitmapData */
			pad2 = data.getLittleEndian16(); // in_uint16_le(s, pad2); /* pad
												// */
			size = data.getLittleEndian16(); // in_uint16_le(s, size);
			row_size = data.getLittleEndian16(); // in_uint16_le(s,
													// row_size);
			final_size = data.getLittleEndian16(); // in_uint16_le(s,

		} else {
			data.incrementPosition(2); // pad
			size = data.getLittleEndian16();
			row_size = data.getLittleEndian16(); // in_uint16_le(s,
													// row_size);
			final_size = data.getLittleEndian16(); // in_uint16_le(s,
													// final_size);
			// this is what's in rdesktop, but doesn't seem to work
			// size = bufsize;
		}

		logger.info("BMPCACHE(cx=" + width + ",cy=" + height + ",id=" + cache_id + ",idx=" + cache_idx + ",bpp=" + bpp
				+ ",size=" + size + ",pad1=" + pad1 + ",bufsize=" + bufsize + ",pad2=" + pad2 + ",rs=" + row_size + ",fs="
				+ final_size + ")");

		if (Bpp == 1) {
			byte[] pixel = Bitmap.decompress(width, height, size, data, Bpp);
			if (pixel != null) {
				// RawBitmap raw = new RawBitmap(bpp, bufsize, pixel, true,
				// width, height);
				// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
				getCache().putBitmap(cache_id, cache_idx,
						new Bitmap(options, Bitmap.convertImage(options, pixel, Bpp), width, height, 0, 0), 0);
			} else
				logger.warn("Failed to decompress bitmap");
		} else {
			byte[] compressed_pixel = new byte[size];
			data.copyToByteArray(compressed_pixel, 0, data.getPosition(), size);
			int[] pixel = Bitmap.decompressInt(options, width, height, size, data, Bpp);
			if (pixel != null) {
				// RawBitmap raw = new RawBitmap(bpp, bufsize, compressed_pixel,
				// true, width, height);
				// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
				getCache().putBitmap(cache_id, cache_idx, new Bitmap(options, pixel, width, height, 0, 0), 0);
			} else
				logger.warn("Failed to decompress bitmap");
		}
	}

	/* Process a bitmap cache v2 order */

	/**
	 * Process a bitmap cache v2 order, storing a bitmap in the main cache, and
	 * the persistant cache if so required
	 * 
	 * @param data
	 *            Packet containing order and bitmap data
	 * @param flags
	 *            Set of flags defining mode of order
	 * @param compressed
	 *            True if bitmap data is compressed
	 * @throws RdesktopException
	 * @throws IOException
	 */
	private void process_bmpcache2(RdpPacket_Localised data, int flags, boolean compressed) throws RdesktopException,
			IOException {
		logger.debug(
				"options low_latency:{}, bulk_compression:{}, use_rdp5:{}, server_bpp:{}, Bpp:{}, bpp_mask:{}, bitmap_compression:{}, persistent_bitmap_caching:{}, bitmap_caching:{}, precache_bitmaps:{}, polygon_ellipse_orders:{}, sendmotion:{}, orders:{}, encryption:{}, packet_encryption:{}, console_session:{}, owncolmap:{}, rdp5_performanceflags:{}",
				new Object[] { options.isLow_latency(), options.isBulk_compression(), options.isUse_rdp5(),
						options.getServer_bpp(), options.getBpp(), options.getBpp_mask(), options.isBitmap_compression(),
						options.isPersistent_bitmap_caching(), options.isBitmap_caching(), options.isPrecache_bitmaps(),
						options.isPolygon_ellipse_orders(), options.isSendmotion(), options.isOrders(),
						options.isEncryption(), options.isPacket_encryption(), options.isConsole_session(),
						options.isOwncolmap(), options.getRdp5_performanceflags() });
		Bitmap bitmap;
		int y;
		int cache_id, cache_idx_low, width, height, Bpp;
		int cache_idx, bufsize;
		byte[] bmpdata, bitmap_id;

		bitmap_id = new byte[8]; /* prevent compiler warning */
		cache_id = flags & ID_MASK;
		Bpp = ((flags & MODE_MASK) >> MODE_SHIFT) - 2;
		// Bpp = Options.Bpp;
		if ((flags & PERSIST) != 0) {
			bitmap_id = new byte[8];
			data.copyToByteArray(bitmap_id, 0, data.getPosition(), 8);
		}

		if ((flags & SQUARE) != 0) {
			width = data.get8(); // in_uint8(s, width);
			height = width;
		} else {
			width = data.get8(); // in_uint8(s, width);
			height = data.get8(); // in_uint8(s, height);
		}

		bufsize = data.getBigEndian16(); // in_uint16_be(s, bufsize);
		bufsize &= BUFSIZE_MASK;
		cache_idx = data.get8(); // in_uint8(s, cache_idx);

		if ((cache_idx & LONG_FORMAT) != 0) {
			cache_idx_low = data.get8(); // in_uint8(s, cache_idx_low);
			cache_idx = ((cache_idx ^ LONG_FORMAT) << 8) + cache_idx_low;
		}

		// in_uint8p(s, data, bufsize);
		// logger.info("BMPCACHE2(compr=" + compressed + ",flags=" + flags
		// + ",width=" + width + ",height=" + height + ",cache_id=" + cache_id
		// + ",cache_idx=" + cache_idx + ",Bpp=" + Bpp + ",bufsize=" + bufsize +
		// ")");

		bmpdata = new byte[width * height * Bpp];
		int[] bmpdataInt = new int[width * height];
		logger.debug("compressed: {}, width:{}, height:{}, Bpp:{}", new Object[] { compressed, width, height, Bpp });
		if (compressed) {
			if (Bpp == 1) {
				// int bpp = 16;// (Bpp * 8) - 7;
				// logger.debug("3-bpp: " + bpp);
				// byte[] compressed_pixel = new byte[bufsize];
				// data.copyToByteArray(compressed_pixel, 0, data.getPosition(),
				// bufsize);
				// RawBitmap raw = new RawBitmap(bpp, bufsize, compressed_pixel,
				// true, width, height);
				// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
				bmpdataInt = Bitmap.convertImage(options, Bitmap.decompress(width, height, bufsize, data, Bpp), Bpp);
			} else {
				// int bpp = 16;
				// logger.debug("4-bpp: " + bpp);
				// byte[] compressed_pixel = new byte[bufsize];
				// data.copyToByteArray(compressed_pixel, 0, data.getPosition(),
				// bufsize);
				// RawBitmap raw = new RawBitmap(bpp, bufsize, compressed_pixel,
				// true, width, height);
				// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
				bmpdataInt = Bitmap.decompressInt(options, width, height, bufsize, data, Bpp);
			}
			if (bmpdataInt == null) {
				logger.debug("Failed to decompress bitmap data");
				// xfree(bmpdata);
				return;
			}
			bitmap = new Bitmap(options, bmpdataInt, width, height, 0, 0);
		} else {
			for (y = 0; y < height; y++) {
				data.copyToByteArray(bmpdata, y * (width * Bpp), (height - y - 1) * (width * Bpp) + data.getPosition(),
						width * Bpp);
			}
			// int bpp = 16;
			// logger.debug("5-bpp: " + bpp);
			// RawBitmap raw = new RawBitmap(bpp, bufsize, bmpdata, false,
			// width, height);
			// getCache().putRawBitmap(cache_id, cache_idx, raw, 0);
			// bmpdataInt = Bitmap.decompressInt(options, width, height,
			// bufsize, data, Bpp);
			bitmap = new Bitmap(options, Bitmap.convertImage(options, bmpdata, Bpp), width, height, 0, 0);
		}

		// bitmap = ui_create_bitmap(width, height, bmpdata);

		if (bitmap != null) {
			getCache().putBitmap(cache_id, cache_idx, bitmap, 0);
			// cache_put_bitmap(cache_id, cache_idx, bitmap, 0);
			if ((flags & PERSIST) != 0)
				getCache().getPstCache().pstcache_put_bitmap(cache_id, cache_idx, bitmap_id, width, height,
						width * height * Bpp, bmpdata);
		}
		// xfree(bmpdata);
	}

	/**
	 * Process a font caching order, and store font in the cache
	 * 
	 * @param data
	 *            Packet containing font cache order, with data for a series of
	 *            glyphs representing a font
	 * @throws RdesktopException
	 */
	private void processFontCache(RdpPacket_Localised data) throws RdesktopException {
		Glyph glyph = null;

		int font = 0, nglyphs = 0;
		int character = 0, offset = 0, baseline = 0, width = 0, height = 0;
		int datasize = 0;
		byte[] fontdata = null;

		font = data.get8();
		nglyphs = data.get8();

		for (int i = 0; i < nglyphs; i++) {
			character = data.getLittleEndian16();
			offset = data.getLittleEndian16();
			baseline = data.getLittleEndian16();
			width = data.getLittleEndian16();
			height = data.getLittleEndian16();
			datasize = (height * ((width + 7) / 8) + 3) & ~3;
			fontdata = new byte[datasize];

			data.copyToByteArray(fontdata, 0, data.getPosition(), datasize);
			data.incrementPosition(datasize);
			glyph = new Glyph(font, character, offset, baseline, width, height, fontdata);
			getCache().putFont(glyph);
		}
	}

	/**
	 * Process a dest blit order, and perform blit on drawing surface
	 * 
	 * @param data
	 *            Packet containing description of the order
	 * @param destblt
	 *            DestBltOrder object in which to store the blit description
	 * @param present
	 *            Flags defining the information available in the packet
	 * @param delta
	 *            True if the coordinates of the blit destination are described
	 *            as relative to the source
	 */
	private void processDestBlt(ChannelHandlerContext context, RdpPacket_Localised data, DestBltOrder destblt, int present,
			boolean delta) {
		if ((present & 0x01) != 0)
			destblt.setX(setCoordinate(data, destblt.getX(), delta));
		if ((present & 0x02) != 0)
			destblt.setY(setCoordinate(data, destblt.getY(), delta));
		if ((present & 0x04) != 0)
			destblt.setCX(setCoordinate(data, destblt.getCX(), delta));
		if ((present & 0x08) != 0)
			destblt.setCY(setCoordinate(data, destblt.getCY(), delta));
		if ((present & 0x10) != 0)
			destblt.setOpcode(ROP2_S(data.get8()));
		// if(logger.isInfoEnabled())
		// logger.info("opcode="+destblt.getOpcode());
		// surface.drawDestBltOrder(context, destblt);
	}

	/**
	 * Parse data defining a brush and store brush information
	 * 
	 * @param data
	 *            Packet containing brush data
	 * @param brush
	 *            Brush object in which to store the brush description
	 * @param present
	 *            Flags defining the information available within the packet
	 */
	private void parseBrush(RdpPacket_Localised data, Brush brush, int present) {
		if ((present & 0x01) != 0)
			brush.setXOrigin(data.get8());
		if ((present & 0x02) != 0)
			brush.setXOrigin(data.get8());
		if ((present & 0x04) != 0)
			brush.setStyle(data.get8());
		byte[] pat = brush.getPattern();
		if ((present & 0x08) != 0)
			pat[0] = (byte) data.get8();
		if ((present & 0x10) != 0)
			for (int i = 1; i < 8; i++)
				pat[i] = (byte) data.get8();
		brush.setPattern(pat);
	}

	// text index
	/**
	 * Parse data describing a pattern blit, and perform blit on drawing surface
	 * 
	 * @param data
	 *            Packet containing blit data
	 * @param patblt
	 *            PatBltOrder object in which to store the blit description
	 * @param present
	 *            Flags defining the information available within the packet
	 * @param delta
	 *            True if the coordinates of the blit destination are described
	 *            as relative to the source
	 */
	private void processPatBlt(ChannelHandlerContext context, RdpPacket_Localised data, PatBltOrder patblt, int present,
			boolean delta) {
		if ((present & 0x01) != 0)
			patblt.setX(setCoordinate(data, patblt.getX(), delta));
		if ((present & 0x02) != 0)
			patblt.setY(setCoordinate(data, patblt.getY(), delta));
		if ((present & 0x04) != 0)
			patblt.setCX(setCoordinate(data, patblt.getCX(), delta));
		if ((present & 0x08) != 0)
			patblt.setCY(setCoordinate(data, patblt.getCY(), delta));
		if ((present & 0x10) != 0) {
			int rop3 = data.get8();
			int rop2 = ROP2_P(rop3);
			patblt.setRop3(rop3);
			patblt.setOpcode(rop2);
		}
		if ((present & 0x20) != 0)
			patblt.setBackgroundColor(setColor(data));
		if ((present & 0x40) != 0)
			patblt.setForegroundColor(setColor(data));
		parseBrush(data, patblt.getBrush(), present >> 7);
		// if(logger.isInfoEnabled()) logger.info("opcode="+patblt.getOpcode());
		PrimaryUtils.sendPatBlt(context, patblt);
		// surface.drawPatBltOrder(context, patblt);
	}

	/**
	 * Parse data describing a screen blit, and perform blit on drawing surface
	 * 
	 * @param data
	 *            Packet containing blit data
	 * @param screenblt
	 *            ScreenBltOrder object in which to store blit description
	 * @param present
	 *            Flags defining the information available within the packet
	 * @param delta
	 *            True if the coordinates of the blit destination are described
	 *            as relative to the source
	 */
	private void processScreenBlt(ChannelHandlerContext context, RdpPacket_Localised data, ScreenBltOrder screenblt,
			int present, boolean delta) {
		if ((present & 0x01) != 0)
			screenblt.setX(setCoordinate(data, screenblt.getX(), delta));
		if ((present & 0x02) != 0)
			screenblt.setY(setCoordinate(data, screenblt.getY(), delta));
		if ((present & 0x04) != 0)
			screenblt.setCX(setCoordinate(data, screenblt.getCX(), delta));
		if ((present & 0x08) != 0)
			screenblt.setCY(setCoordinate(data, screenblt.getCY(), delta));
		if ((present & 0x10) != 0) {
			int rop3 = data.get8();
			int rop2 = ROP2_S(rop3);
			screenblt.setRop3(rop3);
			screenblt.setOpcode(rop2);
		}
		if ((present & 0x20) != 0)
			screenblt.setSrcX(setCoordinate(data, screenblt.getSrcX(), delta));
		if ((present & 0x40) != 0)
			screenblt.setSrcY(setCoordinate(data, screenblt.getSrcY(), delta));
		// if(logger.isInfoEnabled())
		// logger.info("rop3:{}", screenblt.getRop3());
		PrimaryUtils.sendScrBlt(context, screenblt, right, left, top, bottom);
		// surface.drawScreenBltOrder(context, screenblt);

	}

	public void setClip(BoundsOrder bounds) {
		this.top = bounds.getTop();
		this.left = bounds.getLeft();
		this.right = bounds.getRight();
		this.bottom = bounds.getBottom();
		logger.debug("set bound x:{}, y:{}, w:{}, h:{}", new Object[] { left, top, right, bottom });
	}

	public void resetClip() {
		this.top = 0;
		this.left = 0;
		this.right = options.getWidth() - 1; // changed
		this.bottom = options.getHeight() - 1; // changed
		logger.debug("set bound x:{}, y:{}, w:{}, h:{}", new Object[] { left, top, right, bottom });
	}

	/**
	 * Parse data describing a line order, and draw line on drawing surface
	 * 
	 * @param data
	 *            Packet containing line order data
	 * @param line
	 *            LineOrder object describing the line drawing operation
	 * @param present
	 *            Flags defining the information available within the packet
	 * @param delta
	 *            True if the coordinates of the end of the line are defined as
	 *            relative to the start
	 */
	private void processLine(ChannelHandlerContext context, RdpPacket_Localised data, LineOrder line, int present,
			boolean delta) {
		if ((present & 0x01) != 0)
			line.setMixmode(data.getLittleEndian16());
		logger.debug("1size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x02) != 0)
			line.setStartX(setCoordinate(data, line.getStartX(), delta));
		logger.debug("2size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x04) != 0)
			line.setStartY(setCoordinate(data, line.getStartY(), delta));
		logger.debug("3size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x08) != 0)
			line.setEndX(setCoordinate(data, line.getEndX(), delta));
		logger.debug("4size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x10) != 0)
			line.setEndY(setCoordinate(data, line.getEndY(), delta));
		logger.debug("5size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x20) != 0)
			line.setBackgroundColor(setColor(data));
		logger.debug("6size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		if ((present & 0x40) != 0) {
			int rop3 = data.get8();
			int rop2 = ROP2_S(rop3);
			line.setRop3(rop3);
			line.setOpcode(rop2);
		}
		logger.debug("7size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });
		// if ((present & 0x40) != 0)
		// line.setOpcode(data.get8());

		parsePen(data, line.getPen(), present >> 7);
		logger.debug("8size:{}, position: {}", new Object[] { data.getEnd() - data.getPosition(), data.getPosition() });

		// if(logger.isInfoEnabled()) logger.info("Line from
		// ("+line.getStartX()+","+line.getStartY()+") to
		// ("+line.getEndX()+","+line.getEndY()+")");

		if (line.getOpcode() < 0x01 || line.getOpcode() > 0x10) {
			logger.warn("bad ROP2 0x" + line.getOpcode());
			return;
		}

		PrimaryUtils.sendLineTo(context, line);
		// now draw the line
		// surface.drawLineOrder(context, line);
	}

	/**
	 * Parse data describing a rectangle order, and draw the rectangle to the
	 * drawing surface
	 * 
	 * @param data
	 *            Packet containing rectangle order
	 * @param rect
	 *            RectangleOrder object in which to store order description
	 * @param present
	 *            Flags defining information available in packet
	 * @param delta
	 *            True if the rectangle is described as (x,y,width,height), as
	 *            opposed to (x1,y1,x2,y2)
	 */
	private void processRectangle(ChannelHandlerContext context, RdpPacket_Localised data, RectangleOrder rect, int present,
			boolean delta) {
		if ((present & 0x01) != 0)
			rect.setX(setCoordinate(data, rect.getX(), delta));
		if ((present & 0x02) != 0)
			rect.setY(setCoordinate(data, rect.getY(), delta));
		if ((present & 0x04) != 0)
			rect.setCX(setCoordinate(data, rect.getCX(), delta));
		if ((present & 0x08) != 0)
			rect.setCY(setCoordinate(data, rect.getCY(), delta));
		if ((present & 0x10) != 0)
			this.rect_colour = (this.rect_colour & 0xffffff00) | data.get8(); // rect.setColor(setColor(data));
		if ((present & 0x20) != 0)
			this.rect_colour = (this.rect_colour & 0xffff00ff) | (data.get8() << 8); // rect.setColor(setColor(data));
		if ((present & 0x40) != 0)
			this.rect_colour = (this.rect_colour & 0xff00ffff) | (data.get8() << 16);

		rect.setColor(this.rect_colour);
		PrimaryUtils.sendOpaqueRect(context, rect);
		// surface.drawRectangleOrder(context, rect);
	}

	private void processMultiOpaqueRectBlt(ChannelHandlerContext context, RdpPacket_Localised data,
			MultiOpaqueRectBltOrder rect, int present, boolean delta) {
		// rect.reset();

		if ((present & 0x01) != 0)
			rect.setX(setCoordinate(data, rect.getX(), delta));

		if ((present & 0x02) != 0)
			rect.setY(setCoordinate(data, rect.getY(), delta));

		if ((present & 0x04) != 0)
			rect.setCX(setCoordinate(data, rect.getCX(), delta));

		if ((present & 0x08) != 0)
			rect.setCY(setCoordinate(data, rect.getCY(), delta));

		if ((present & 0x10) != 0)
			this.rect_colour = (this.rect_colour & 0xffffff00) | data.get8(); // rect.setColor(setColor(data));

		if ((present & 0x20) != 0)
			this.rect_colour = (this.rect_colour & 0xffff00ff) | (data.get8() << 8); // rect.setColor(setColor(data));

		if ((present & 0x40) != 0)
			this.rect_colour = (this.rect_colour & 0xff00ffff) | (data.get8() << 16);

		rect.setColor(this.rect_colour);
		// logger.debug("present & (1 << 7): {}, position:{}", new Object[] {
		// present & (1 << 7), data.getPosition() });
		if ((present & 0x80) != 0) {
			int size = data.get8();
			rect.setNumRectangles(size);
		}

		// logger.debug("present & 0x000100: {}, position:{}", new Object[] {
		// present & 0x0100, data.getPosition() });
		if ((present & 0x0100) != 0) {
			rect.setData(data.getLittleEndian16()); // cbData
			// logger.debug("cbData:{}, position after cbData:{}", new Object[]
			// { rect.getData(), data.getPosition() });
			updateReadDeltaRects(data, rect);
		}

		// logger.debug("position:{}, present:{}, numOfRect:{}, x:{}, y:{}, w:{}, h:{}",
		// new Object[] { data.getPosition(),
		// present, rect.getNumRectangles(), rect.getX(), rect.getY(),
		// rect.getCX(), rect.getCY() });
		PrimaryUtils.sendMultiOpaqueRect(context, rect);

		// surface.drawMultiOpaqueRect(context, rect);

	}

	private void updateReadDeltaRects(RdpPacket_Localised data, MultiOpaqueRectBltOrder multiOpaqueRectBlt) {
		int flags = 0;
		int[] zeroBits;

		int zeroBitsSize;
		int number = multiOpaqueRectBlt.getNumRectangles();

		if (number > 45) {
			number = 45;
		}
		zeroBitsSize = ((number + 1) / 2);

		RectangleOrder[] orders = new RectangleOrder[number + 1];
		for (int i = 0; i < number + 1; i++) {
			RectangleOrder order = new RectangleOrder();
			orders[i] = order;
		}
		multiOpaqueRectBlt.setRectangleOrders(orders);

		// logger.debug("zeroBitsSize:{} in bytes, data:{}, poisition:{}, num:{}",
		// new Object[] { zeroBitsSize,
		// multiOpaqueRectBlt.getData(), data.getPosition(), number });

		zeroBits = new int[zeroBitsSize];
		for (int i = 0; i < zeroBitsSize; i++) {
			zeroBits[i] = data.get8();
		}
		// logger.debug("poisition after copy:{}", new Object[] {
		// data.getPosition() });
		// data.incrementPosition(zeroBitsSize);
		// logger.debug("poisition after increment:{}", new Object[] {
		// data.getPosition() });
		int delta = multiOpaqueRectBlt.getData() - zeroBitsSize;
		int[] data2 = new int[delta];
		for (int i = 0; i < delta; i++) {
			data2[i] = data.get8();
		}
		// data.copyToByteArray(data2, 0, 0, delta);
		// data.incrementPosition(delta);
		AtomicInteger index = new AtomicInteger(0);

		// logger.debug("poisition after increment:{}, delta:{}", new Object[] {
		// data.getPosition(), delta });

		for (int i = 1; i < number + 1; i++) {
			if ((i - 1) % 2 == 0) {
				flags = zeroBits[(i - 1) / 2] & 0xff;
			}
			// logger.debug("{}, flags:{}", new Object[] { flags,
			// Integer.toBinaryString(flags) });

			if ((~flags & 0x80) != 0) {
				// logger.debug("set x");
				int del = updateReadDelta(data2, index);
				logger.debug("multi delta:{}", del);
				multiOpaqueRectBlt.getRectangleOrders()[i].setX(del + multiOpaqueRectBlt.getRectangleOrders()[i - 1].getX());
				logger.debug("1multix:{}", multiOpaqueRectBlt.getRectangleOrders()[i].getX());
			} else {
				multiOpaqueRectBlt.getRectangleOrders()[i].setX(multiOpaqueRectBlt.getRectangleOrders()[i - 1].getX());
				logger.debug("2multix:{}", multiOpaqueRectBlt.getRectangleOrders()[i].getX());
			}
			if ((~flags & 0x40) != 0) {
				// logger.debug("set y");
				multiOpaqueRectBlt.getRectangleOrders()[i].setY(updateReadDelta(data2, index)
						+ multiOpaqueRectBlt.getRectangleOrders()[i - 1].getY());
			} else {
				multiOpaqueRectBlt.getRectangleOrders()[i].setY(multiOpaqueRectBlt.getRectangleOrders()[i - 1].getY());
			}
			if ((~flags & 0x20) != 0) {
				// logger.debug("set w");
				multiOpaqueRectBlt.getRectangleOrders()[i].setCX(updateReadDelta(data2, index));
			} else {
				multiOpaqueRectBlt.getRectangleOrders()[i].setCX(multiOpaqueRectBlt.getRectangleOrders()[i - 1].getCX());
			}
			if ((~flags & 0x10) != 0) {
				// logger.debug("set h");
				multiOpaqueRectBlt.getRectangleOrders()[i].setCY(updateReadDelta(data2, index));
			} else {
				multiOpaqueRectBlt.getRectangleOrders()[i].setCY(multiOpaqueRectBlt.getRectangleOrders()[i - 1].getCY());
			}
			flags <<= 4;
		}

		// logger.debug("data2 position:{}", index);

	}

	private int updateReadDelta(int[] data, AtomicInteger index) {
		int b = data[index.getAndIncrement()] & 0xff;
		logger.debug("b:{}, binary:{}", new Object[] { b, Integer.toBinaryString(b) });
		int value;
		if ((b & 0x40) != 0) { // negative
			value = (b | (~0x3f & 0xff));
		} else { // positive
			value = (b & (0x3f & 0xff));
		}

		if ((b & 0x80) != 0) {
			b = data[index.getAndIncrement()] & 0xff;
			logger.debug("b:{}, binary:{}", new Object[] { b, Integer.toBinaryString(b) });
			value = ((value << 8)) | b;
			String extend15 = extend15(Integer.toBinaryString(value));
			String signExtend = signExtend(extend15);
			value = (int) Long.parseLong(signExtend, 2);
			logger.debug("extend15:{}, extend:{}, value:{}", new Object[] { extend15, signExtend, value });
		} else {
			String extend7 = extend7(Integer.toBinaryString(value));
			String signExtend = signExtend(extend7);
			value = (int) Long.parseLong(signExtend, 2);
			logger.debug("extend7:{}, extend:{}, value:{}", new Object[] { extend7, signExtend, value });
		}
		return value;
	}

	private static String extend7(String str) {
		if (str.length() >= 7) {
			return str;
		}
		int n = 7 - str.length();
		char[] sign_ext = new char[n];
		Arrays.fill(sign_ext, '0');

		return new String(sign_ext) + str;
	}

	private static String extend15(String str) {
		if (str.length() >= 15) {
			return str;
		}
		int n = 15 - str.length();
		char[] sign_ext = new char[n];
		Arrays.fill(sign_ext, '0');

		return new String(sign_ext) + str;
	}

	private static String signExtend(String str) {
		int n = 32 - str.length();
		char[] sign_ext = new char[n];
		Arrays.fill(sign_ext, str.charAt(0));

		return new String(sign_ext) + str;
	}

	/**
	 * Parse data describing a desktop save order, either saving the desktop to
	 * cache, or drawing a section to screen
	 * 
	 * @param data
	 *            Packet containing desktop save order
	 * @param desksave
	 *            DeskSaveOrder object in which to store order description
	 * @param present
	 *            Flags defining information available within the packet
	 * @param delta
	 *            True if destination coordinates are described as relative to
	 *            the source
	 * @throws RdesktopException
	 */
	private void processDeskSave(ChannelHandlerContext context, RdpPacket_Localised data, DeskSaveOrder desksave,
			int present, boolean delta) throws RdesktopException {
		// int width = 0, height = 0;

		if ((present & 0x01) != 0) {
			desksave.setOffset(data.getLittleEndian32());
		}

		if ((present & 0x02) != 0) {
			desksave.setLeft(setCoordinate(data, desksave.getLeft(), delta));
		}

		if ((present & 0x04) != 0) {
			desksave.setTop(setCoordinate(data, desksave.getTop(), delta));
		}

		if ((present & 0x08) != 0) {
			desksave.setRight(setCoordinate(data, desksave.getRight(), delta));
		}

		if ((present & 0x10) != 0) {
			desksave.setBottom(setCoordinate(data, desksave.getBottom(), delta));
		}

		if ((present & 0x20) != 0) {
			desksave.setAction(data.get8());
		}

		// width = desksave.getRight() - desksave.getLeft() + 1;
		// height = desksave.getBottom() - desksave.getTop() + 1;

		// if (desksave.getAction() == 0) {
		// int[] pixel = surface.getImage(desksave.getLeft(), desksave.getTop(),
		// width, height);
		// getCache().putDesktop(desksave.getOffset(), width, height, pixel);
		// } else {
		// int[] pixel = getCache().getDesktopInt(desksave.getOffset(), width,
		// height);
		// surface.putImage(context, desksave.getLeft(), desksave.getTop(),
		// width, height, pixel);
		// }
	}

	// colorful graph
	/**
	 * Process data describing a memory blit, and perform blit on drawing
	 * surface
	 * 
	 * @param data
	 *            Packet containing mem blit order
	 * @param memblt
	 *            MemBltOrder object in which to store description of blit
	 * @param present
	 *            Flags defining information available in packet
	 * @param delta
	 *            True if destination coordinates are described as relative to
	 *            the source
	 * @throws RdesktopException
	 */
	private void processMemBlt(ChannelHandlerContext context, RdpPacket_Localised data, MemBltOrder memblt, int present,
			boolean delta) throws RdesktopException {
		if ((present & 0x01) != 0) {
			memblt.setCacheID(data.get8());
			memblt.setColorTable(data.get8());
		}
		if ((present & 0x02) != 0)
			memblt.setX(setCoordinate(data, memblt.getX(), delta));
		if ((present & 0x04) != 0)
			memblt.setY(setCoordinate(data, memblt.getY(), delta));
		if ((present & 0x08) != 0)
			memblt.setCX(setCoordinate(data, memblt.getCX(), delta));
		if ((present & 0x10) != 0)
			memblt.setCY(setCoordinate(data, memblt.getCY(), delta));
		if ((present & 0x20) != 0)
			memblt.setOpcode(ROP2_S(data.get8()));
		if ((present & 0x40) != 0)
			memblt.setSrcX(setCoordinate(data, memblt.getSrcX(), delta));
		if ((present & 0x80) != 0)
			memblt.setSrcY(setCoordinate(data, memblt.getSrcY(), delta));
		if ((present & 0x0100) != 0)
			memblt.setCacheIDX(data.getLittleEndian16());

		// if(logger.isInfoEnabled()) logger.info("Memblt
		// opcode="+memblt.getOpcode());
		// RawBitmap rawbitmap = getCache().getRawBitmap(memblt.getCacheID(),
		// memblt.getCacheIDX());
		// PrimaryUtils.sendMemBlt(context, memblt, rawbitmap);
		// surface.drawMemBltOrder(context, memblt);

	}

	/**
	 * Parse data describing a tri blit order, and perform blit on drawing
	 * surface
	 * 
	 * @param data
	 *            Packet containing tri blit order
	 * @param triblt
	 *            TriBltOrder object in which to store blit description
	 * @param present
	 *            Flags defining information available in packet
	 * @param delta
	 *            True if destination coordinates are described as relative to
	 *            the source
	 */
	private void processTriBlt(ChannelHandlerContext context, RdpPacket_Localised data, TriBltOrder triblt, int present,
			boolean delta) {
		if ((present & 0x01) != 0) {
			triblt.setCacheID(data.get8());
			triblt.setColorTable(data.get8());
		}
		if ((present & 0x02) != 0)
			triblt.setX(setCoordinate(data, triblt.getX(), delta));
		if ((present & 0x04) != 0)
			triblt.setY(setCoordinate(data, triblt.getY(), delta));
		if ((present & 0x08) != 0)
			triblt.setCX(setCoordinate(data, triblt.getCX(), delta));
		if ((present & 0x10) != 0)
			triblt.setCY(setCoordinate(data, triblt.getCY(), delta));
		if ((present & 0x20) != 0)
			triblt.setOpcode(ROP2_S(data.get8()));
		if ((present & 0x40) != 0)
			triblt.setSrcX(setCoordinate(data, triblt.getSrcX(), delta));
		if ((present & 0x80) != 0)
			triblt.setSrcY(setCoordinate(data, triblt.getSrcY(), delta));
		if ((present & 0x0100) != 0)
			triblt.setBackgroundColor(setColor(data));
		if ((present & 0x0200) != 0)
			triblt.setForegroundColor(setColor(data));

		parseBrush(data, triblt.getBrush(), present >> 10);

		if ((present & 0x8000) != 0)
			triblt.setCacheIDX(data.getLittleEndian16());
		if ((present & 0x10000) != 0)
			triblt.setUnknown(data.getLittleEndian16());

		// surface.drawTriBltOrder(context, triblt);
	}

	/**
	 * Parse data describing a multi-line order, and draw to registered surface
	 * 
	 * @param data
	 *            Packet containing polyline order
	 * @param polyline
	 *            PolyLineOrder object in which to store order description
	 * @param present
	 *            Flags defining information available in packet
	 * @param delta
	 *            True if each set of coordinates is described relative to
	 *            previous set
	 */
	private void processPolyLine(ChannelHandlerContext context, RdpPacket_Localised data, PolyLineOrder polyline,
			int present, boolean delta) {
		if ((present & 0x01) != 0)
			polyline.setX(setCoordinate(data, polyline.getX(), delta));
		if ((present & 0x02) != 0)
			polyline.setY(setCoordinate(data, polyline.getY(), delta));
		if ((present & 0x04) != 0)
			polyline.setOpcode(data.get8());
		if ((present & 0x10) != 0)
			polyline.setForegroundColor(setColor(data));
		if ((present & 0x20) != 0)
			polyline.setLines(data.get8());
		if ((present & 0x40) != 0) {
			int datasize = data.get8();
			polyline.setDataSize(datasize);
			byte[] databytes = new byte[datasize];
			for (int i = 0; i < datasize; i++)
				databytes[i] = (byte) data.get8();
			polyline.setData(databytes);
		}
		// logger.info("polyline delta="+delta);
		// if(logger.isInfoEnabled()) logger.info("Line from
		// ("+line.getStartX()+","+line.getStartY()+") to
		// ("+line.getEndX()+","+line.getEndY()+")");
		// now draw the line
		// surface.drawPolyLineOrder(context, polyline);
	}

	/**
	 * Process a text2 order and output to drawing surface
	 * 
	 * @param data
	 *            Packet containing text2 order
	 * @param text2
	 *            Text2Order object in which to store order description
	 * @param present
	 *            Flags defining information available in packet
	 * @param delta
	 *            Unused
	 * @throws RdesktopException
	 */
	private void processText2(ChannelHandlerContext context, RdpPacket_Localised data, Text2Order text2, int present,
			boolean delta) throws RdesktopException {

		if ((present & 0x000001) != 0) {
			text2.setFont(data.get8());
		}
		if ((present & 0x000002) != 0) {
			text2.setFlags(data.get8());
		}

		if ((present & 0x000004) != 0) {
			text2.setOpcode(data.get8()); // setUnknown(data.get8());
		}

		if ((present & 0x000008) != 0) {
			text2.setMixmode(data.get8());
		}

		if ((present & 0x000010) != 0) {
			text2.setForegroundColor(setColor(data));
		}

		if ((present & 0x000020) != 0) {
			text2.setBackgroundColor(setColor(data));
		}

		if ((present & 0x000040) != 0) {
			text2.setClipLeft(data.getLittleEndian16());
		}

		if ((present & 0x000080) != 0) {
			text2.setClipTop(data.getLittleEndian16());
		}

		if ((present & 0x000100) != 0) {
			text2.setClipRight(data.getLittleEndian16());
		}

		if ((present & 0x000200) != 0) {
			text2.setClipBottom(data.getLittleEndian16());
		}

		if ((present & 0x000400) != 0) {
			text2.setBoxLeft(data.getLittleEndian16());
		}

		if ((present & 0x000800) != 0) {
			text2.setBoxTop(data.getLittleEndian16());
		}

		if ((present & 0x001000) != 0) {
			text2.setBoxRight(data.getLittleEndian16());
		}

		if ((present & 0x002000) != 0) {
			text2.setBoxBottom(data.getLittleEndian16());
		}

		/*
		 * Unknown members, seen when connecting to a session that was
		 * disconnected with mstsc and with wintach's spreadsheet test.
		 */
		if ((present & 0x004000) != 0)
			data.incrementPosition(1);

		if ((present & 0x008000) != 0)
			data.incrementPosition(1);

		if ((present & 0x010000) != 0) {
			data.incrementPosition(1); /* guessing the length here */
			logger.warn("Unknown order state member (0x010000) in text2 order.\n");
		}

		if ((present & 0x020000) != 0)
			data.incrementPosition(4);

		if ((present & 0x040000) != 0)
			data.incrementPosition(4);

		if ((present & 0x080000) != 0) {
			text2.setX(data.getLittleEndian16());
		}

		if ((present & 0x100000) != 0) {
			text2.setY(data.getLittleEndian16());
		}

		if ((present & 0x200000) != 0) {
			text2.setLength(data.get8());

			byte[] text = new byte[text2.getLength()];
			data.copyToByteArray(text, 0, data.getPosition(), text.length);
			data.incrementPosition(text.length);
			text2.setText(text);

			/*
			 * if(logger.isInfoEnabled()) logger.info("X: " + text2.getX() + "
			 * Y: " + text2.getY() + " Left Clip: " + text2.getClipLeft() + "
			 * Top Clip: " + text2.getClipTop() + " Right Clip: " +
			 * text2.getClipRight() + " Bottom Clip: " + text2.getClipBottom() +
			 * " Left Box: " + text2.getBoxLeft() + " Top Box: " +
			 * text2.getBoxTop() + " Right Box: " + text2.getBoxRight() + "
			 * Bottom Box: " + text2.getBoxBottom() + " Foreground Color: " +
			 * text2.getForegroundColor() + " Background Color: " +
			 * text2.getBackgroundColor() + " Font: " + text2.getFont() + "
			 * Flags: " + text2.getFlags() + " Mixmode:
			 * " + text2.getMixmode() + " Unknown: " + text2.getUnknown() + "
			 * Length: " + text2.getLength());
			 */
		}

		// this.drawText(context, text2, text2.getClipRight() -
		// text2.getClipLeft(),
		// text2.getClipBottom() - text2.getClipTop(), text2.getBoxRight() -
		// text2.getBoxLeft(), text2.getBoxBottom()
		// - text2.getBoxTop());

	}

	/**
	 * Parse a description for a bounding box
	 * 
	 * @param data
	 *            Packet containing order defining bounding box
	 * @param bounds
	 *            BoundsOrder object in which to store description of bounds
	 * @throws OrderException
	 */
	private void parseBounds(RdpPacket_Localised data, BoundsOrder bounds) throws OrderException {
		int present = 0;

		present = data.get8();

		if ((present & 1) != 0) {
			bounds.setLeft(setCoordinate(data, bounds.getLeft(), false));
		} else if ((present & 16) != 0) {
			bounds.setLeft(setCoordinate(data, bounds.getLeft(), true));
		}

		if ((present & 2) != 0) {
			bounds.setTop(setCoordinate(data, bounds.getTop(), false));
		} else if ((present & 32) != 0) {
			bounds.setTop(setCoordinate(data, bounds.getTop(), true));
		}

		if ((present & 4) != 0) {
			bounds.setRight(setCoordinate(data, bounds.getRight(), false));
		} else if ((present & 64) != 0) {
			bounds.setRight(setCoordinate(data, bounds.getRight(), true));
		}

		if ((present & 8) != 0) {
			bounds.setBottom(setCoordinate(data, bounds.getBottom(), false));
		} else if ((present & 128) != 0) {
			bounds.setBottom(setCoordinate(data, bounds.getBottom(), true));
		}

		if (data.getPosition() > data.getEnd()) {
			throw new OrderException("Too far!");
		}
	}

	/**
	 * Retrieve a coordinate from a packet and return as an absolute integer
	 * coordinate
	 * 
	 * @param data
	 *            Packet containing coordinate at current read position
	 * @param coordinate
	 *            Offset coordinate
	 * @param delta
	 *            True if coordinate being read should be taken as relative to
	 *            offset coordinate, false if absolute
	 * @return Integer value of coordinate stored in packet, in absolute form
	 */
	private static int setCoordinate(RdpPacket_Localised data, int coordinate, boolean delta) {
		byte change = 0;

		if (delta) {
			change = (byte) data.get8();
			coordinate += change;
			logger.debug("true delta:{}", change);
			return coordinate;
		} else {
			coordinate = data.getLittleEndian16();
			// logger.debug("coordinate:{}", coordinate);
			return coordinate;
		}
	}

	/**
	 * Read a colour value from a packet
	 * 
	 * @param data
	 *            Packet containing colour value at current read position
	 * @return Integer colour value read from packet
	 */
	private static int setColor(RdpPacket_Localised data) {
		int color = 0;
		int i = 0;

		i = data.get8(); // in_uint8(s, i);
		color = i; // *colour = i;
		i = data.get8(); // in_uint8(s, i);
		color |= i << 8; // *colour |= i << 8;
		i = data.get8(); // in_uint8(s, i);
		color |= i << 16; // *colour |= i << 16;

		// color = data.get8();
		// data.incrementPosition(2);
		return color;
	}

	/**
	 * Set current cache
	 * 
	 * @param cache
	 *            Cache object to set as current global cache
	 */
	public void registerCache(Cache cache) {
		setCache(cache);
	}

	/**
	 * Parse a pen definition
	 * 
	 * @param data
	 *            Packet containing pen description at current read position
	 * @param pen
	 *            Pen object in which to store pen description
	 * @param present
	 *            Flags defining information available within packet
	 * @return True if successful
	 */
	private static boolean parsePen(RdpPacket_Localised data, Pen pen, int present) {
		if ((present & 0x01) != 0)
			pen.setStyle(data.get8());
		if ((present & 0x02) != 0)
			pen.setWidth(data.get8());
		if ((present & 0x04) != 0)
			pen.setColor(setColor(data));

		return true; // return s_check(s);
	}

	/**
	 * Interpret an integer as a 16-bit two's complement number, based on its
	 * binary representation
	 * 
	 * @param val
	 *            Integer interpretation of binary number
	 * @return 16-bit two's complement value of input
	 */
	// private int twosComplement16(int val) {
	// return ((val & 0x8000) != 0) ? -((~val & 0xFFFF) + 1) : val;
	// }

	// /**
	// * Draw a text2 order to the drawing surface
	// *
	// * @param text2
	// * Text2Order describing text to be drawn
	// * @param clipcx
	// * Width of clipping area
	// * @param clipcy
	// * Height of clipping area
	// * @param boxcx
	// * Width of bounding box (to draw if > 1)
	// * @param boxcy
	// * Height of bounding box (to draw if boxcx > 1)
	// * @throws RdesktopException
	// */
	// private void drawText(ChannelHandlerContext context, Text2Order text2,
	// int clipcx, int clipcy, int boxcx, int boxcy)
	// throws RdesktopException {
	// byte[] text = text2.getText();
	// DataBlob entry = null;
	// Glyph glyph = null;
	// int offset = 0;
	// int ptext = 0;
	// int length = text2.getLength();
	// int x = text2.getX();
	// int y = text2.getY();
	//
	// // if (boxcx > 1) {
	// // surface.fillRectangle(context, text2.getBoxLeft(), text2.getBoxTop(),
	// // boxcx, boxcy, text2.getBackgroundColor());
	// // } else if (text2.getMixmode() == MIX_OPAQUE) {
	// // surface.fillRectangle(context, text2.getClipLeft(),
	// // text2.getClipTop(), clipcx, clipcy,
	// // text2.getBackgroundColor());
	// // }
	//
	// /*
	// * logger.debug("X: " + text2.getX() + " Y: " + text2.getY() +
	// * " Left Clip: " + text2.getClipLeft() + " Top Clip: " +
	// * text2.getClipTop() + " Right Clip: " + text2.getClipRight() +
	// * " Bottom Clip: " + text2.getClipBottom() + " Left Box:
	// * " + text2.getBoxLeft() + " Top Box: " + text2.getBoxTop() + " Right
	// * Box: " + text2.getBoxRight() + " Bottom Box:
	// * " + text2.getBoxBottom() + " Foreground Color:
	// * " + text2.getForegroundColor() + " Background Color: " +
	// * text2.getBackgroundColor() + " Font: " + text2.getFont() + " Flags: "
	// * + text2.getFlags() + " Mixmode: " + text2.getMixmode() + " Unknown: "
	// * + text2.getUnknown() + " Length: " + text2.getLength());
	// */
	// for (int i = 0; i < length;) {
	// switch (text[ptext + i] & 0x000000ff) {
	// case (0xff):
	// if (i + 2 < length) {
	// byte[] data = new byte[text[ptext + i + 2] & 0x000000ff];
	// System.arraycopy(text, ptext, data, 0, text[ptext + i + 2] & 0x000000ff);
	// DataBlob db = new DataBlob(text[ptext + i + 2] & 0x000000ff, data);
	// getCache().putText(text[ptext + i + 1] & 0x000000ff, db);
	// } else {
	// throw new RdesktopException();
	// }
	// length -= i + 3;
	// ptext = i + 3;
	// i = 0;
	// break;
	//
	// case (0xfe):
	// entry = getCache().getText(text[ptext + i + 1] & 0x000000ff);
	// if (entry != null) {
	// if ((entry.getData()[1] == 0) && ((text2.getFlags() & TEXT2_IMPLICIT_X)
	// == 0)) {
	// if ((text2.getFlags() & 0x04) != 0) {
	// y += text[ptext + i + 2] & 0x000000ff;
	// } else {
	// x += text[ptext + i + 2] & 0x000000ff;
	// }
	// }
	// }
	// if (i + 2 < length) {
	// i += 3;
	// } else {
	// i += 2;
	// }
	// length -= i;
	// ptext = i;
	// i = 0;
	// // break;
	//
	// byte[] data = entry.getData();
	// for (int j = 0; j < entry.getSize(); j++) {
	// glyph = getCache().getFont(text2.getFont(), data[j] & 0x000000ff);
	// if ((text2.getFlags() & TEXT2_IMPLICIT_X) == 0) {
	// offset = data[++j] & 0x000000ff;
	// if ((offset & 0x80) != 0) {
	// if ((text2.getFlags() & TEXT2_VERTICAL) != 0) {
	// int var = this.twosComplement16((data[j + 1] & 0xff) | ((data[j + 2] &
	// 0xff) << 8));
	// y += var;
	// j += 2;
	// } else {
	// int var = this.twosComplement16((data[j + 1] & 0xff) | ((data[j + 2] &
	// 0xff) << 8));
	// x += var;
	// j += 2;
	// }
	// } else {
	// if ((text2.getFlags() & TEXT2_VERTICAL) != 0) {
	// y += offset;
	// } else {
	// x += offset;
	// }
	// }
	// }
	// if (glyph != null) {
	// // if((text2.getFlags() & TEXT2_VERTICAL) != 0)
	// // logger.info("Drawing glyph: (" + (x +
	// // (short)glyph.getOffset()) + ", " + (y +
	// // (short)glyph.getBaseLine()) + ")" );
	// // surface.drawGlyph(context, text2.getMixmode(), x +
	// // (short) glyph.getOffset(),
	// // y + (short) glyph.getBaseLine(), glyph.getWidth(),
	// // glyph.getHeight(), glyph.getFontData(),
	// // text2.getBackgroundColor(),
	// // text2.getForegroundColor());
	//
	// if ((text2.getFlags() & TEXT2_IMPLICIT_X) != 0) {
	// x += glyph.getWidth();
	// }
	// }
	// }
	// break;
	//
	// default:
	// glyph = getCache().getFont(text2.getFont(), text[ptext + i] &
	// 0x000000ff);
	// if ((text2.getFlags() & TEXT2_IMPLICIT_X) == 0) {
	// offset = text[ptext + (++i)] & 0x000000ff;
	// if ((offset & 0x80) != 0) {
	// if ((text2.getFlags() & TEXT2_VERTICAL) != 0) {
	// // logger.info("y +=" + (text[ptext +
	// // (i+1)]&0x000000ff) + " | " + ((text[ptext +
	// // (i+2)]&0x000000ff) << 8));
	// int var = this.twosComplement16((text[ptext + i + 1] & 0x000000ff)
	// | ((text[ptext + i + 2] & 0x000000ff) << 8));
	// y += var;
	// i += 2;
	// } else {
	// int var = this.twosComplement16((text[ptext + i + 1] & 0x000000ff)
	// | ((text[ptext + i + 2] & 0x000000ff) << 8));
	// x += var;
	// i += 2;
	// }
	// } else {
	// if ((text2.getFlags() & TEXT2_VERTICAL) != 0) {
	// y += offset;
	// } else {
	// x += offset;
	// }
	// }
	// }
	// if (glyph != null) {
	// // surface.drawGlyph(context, text2.getMixmode(), x +
	// // (short) glyph.getOffset(),
	// // y + (short) glyph.getBaseLine(), glyph.getWidth(),
	// // glyph.getHeight(), glyph.getFontData(),
	// // text2.getBackgroundColor(), text2.getForegroundColor());
	//
	// if ((text2.getFlags() & TEXT2_IMPLICIT_X) != 0)
	// x += glyph.getWidth();
	// }
	// i++;
	// break;
	// }
	// }
	// }

	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}
}
