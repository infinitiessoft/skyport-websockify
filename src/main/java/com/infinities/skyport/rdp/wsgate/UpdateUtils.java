package com.infinities.skyport.rdp.wsgate;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.RDPSession;
import com.lixia.rdp.orders.BoundsOrder;

public class UpdateUtils {

	private static final Logger logger = LoggerFactory.getLogger(UpdateUtils.class);
	// private static final int WSOP_SC_BEGINPAINT = 0;
	// private static final int WSOP_SC_ENDPAINT = 1;
	public static final int WSOP_SC_BITMAP = 2;
	private static final int WSOP_SC_SETBOUNDS = 4;
	private static final int WSOP_RGB = 15;
	private static final int WSOP_RGBBULK = 16;


	public static void sendBeginPaint(ChannelHandlerContext context) {
		logger.debug("BP");
		// int op = WSOP_SC_BEGINPAINT;
		// send(context, op);
	}

	public static byte[] int32ToByteArray(int a) {
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);
		ret[1] = (byte) ((a >> 8) & 0xFF);
		ret[2] = (byte) ((a >> 16) & 0xFF);
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	public static byte[] int32ToByteArray(long a) {
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);
		ret[1] = (byte) ((a >> 8) & 0xFF);
		ret[2] = (byte) ((a >> 16) & 0xFF);
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	// public static byte int8ToByteArray(int a) {
	// return a & 0xFF;
	// // byte[] ret = new byte[4];
	// // ret[0] = (byte) (a & 0xFF);
	// // ret[1] = (byte) ((a >> 8) & 0xFF);
	// // ret[2] = (byte) ((a >> 16) & 0xFF);
	// // ret[3] = (byte) ((a >> 24) & 0xFF);
	// // return ret;
	// }

	// private static void send(ChannelHandlerContext context, int buf) {
	//
	// // ByteBuffer bb = ByteBuffer.allocate(4);//.wrap(intToByteArray(buf));
	// ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
	// buffer.writeBytes(int32ToByteArray(buf));
	// // bb.putInt(buf);
	//
	// // bb.put(intToByteArray(buf));
	// // send(context, buffer);
	// }

	public static int byteArrayToInt(byte[] b) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	public static void send(ChannelHandlerContext context, ChannelBuffer buffer) {
		// logger.debug("send buffer size: {}", buffer.readableBytes());
		UpstreamMessageEvent upstreamEvent =
				new UpstreamMessageEvent(context.getChannel(), buffer, context.getChannel().getRemoteAddress());
		context.sendUpstream(upstreamEvent);
	}

	public static void sendBitmapUpdates(ChannelHandlerContext context, ChannelBuffer buffer) {
		// send(context, buffer);
	}

	public static void sendEndPaint(ChannelHandlerContext context) {
		logger.debug("EP");
		// int op = WSOP_SC_ENDPAINT;
		// send(context, op);
	}

	public static void sendSynchronize(ChannelHandlerContext context) {
		logger.debug("Synchronize");
	}

	public static void sendDesktopResize(RDPSession session, ChannelHandlerContext context) {
		logger.debug("DesktopResize");
		// String sendMsg = "R:";
		// sendMsg += session.getOptions().getWidth();
		// sendMsg += "x";
		// sendMsg += session.getOptions().getHeight();
		// send(context, sendMsg);
	}

	public static void sendPalette() {
		logger.debug("Palette");
	}

	public static void sendSetBounds(ChannelHandlerContext context, BoundsOrder bounds) {
		int op = WSOP_SC_SETBOUNDS;
		int l = 0, t = 0, r = 0, b = 0;
		if (bounds != null) {
			l = bounds.getLeft();
			t = bounds.getTop();
			r = bounds.getRight() + 1;
			b = bounds.getBottom() + 1;
		}

		logger.debug("set bounds l: {}, t: {}, r: {}, b: {}", new Object[] { l, t, r, b });
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(int32ToByteArray(op));
		buffer.writeBytes(int32ToByteArray(l));
		buffer.writeBytes(int32ToByteArray(t));
		buffer.writeBytes(int32ToByteArray(r));
		buffer.writeBytes(int32ToByteArray(b));
		// send(context, buffer);
	}

	public static void sendRGB(ChannelHandlerContext context, int x, int y, int color) {
		logger.debug("send rdp");
		int op = WSOP_RGB;
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(int32ToByteArray(op));
		buffer.writeBytes(int32ToByteArray(x));
		buffer.writeBytes(int32ToByteArray(y));
		buffer.writeBytes(int32ToByteArray(color));
		// send(context, buffer);
	}

	public static void
			sendRGBBulk(ChannelHandlerContext context, int x, int y, int cx, int cy, int[] data, int offset, int w) {
		logger.debug("send rdpbulk");
		int op = WSOP_RGBBULK;
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(int32ToByteArray(op));
		buffer.writeBytes(int32ToByteArray(x));
		buffer.writeBytes(int32ToByteArray(y));
		buffer.writeBytes(int32ToByteArray(cx));
		buffer.writeBytes(int32ToByteArray(cy));
		buffer.writeBytes(int32ToByteArray(offset));
		buffer.writeBytes(int32ToByteArray(w));
		int size = data.length * 4;
		buffer.writeBytes(int32ToByteArray(size));
		buffer.writeBytes(int32ToByteArray(data));
		// send(context, buffer);
	}

	private static byte[] int32ToByteArray(int[] data) {
		byte[] ret = new byte[data.length * 4];
		int retIndex = 0;
		for (int i = 0; i < data.length; i++) {
			byte[] array = int32ToByteArray(data[i]);
			ret[retIndex++] = array[0];
			ret[retIndex++] = array[1];
			ret[retIndex++] = array[2];
			ret[retIndex++] = array[3];
		}
		return ret;
	}

}
