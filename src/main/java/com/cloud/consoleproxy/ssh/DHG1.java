package com.cloud.consoleproxy.ssh;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.vnc.util.SSHUtil;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomKeyExchange;
import com.jcraft.jsch.CustomPacket;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.DH;
import com.jcraft.jsch.HASH;

public class DHG1 extends CustomKeyExchange {

	private final static Logger logger = LoggerFactory.getLogger(DHG1.class);
	static final byte[] g = { 2 };
	static final byte[] p = { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2, (byte) 0x21, (byte) 0x68,
			(byte) 0xC2, (byte) 0x34, (byte) 0xC4, (byte) 0xC6, (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC,
			(byte) 0x1C, (byte) 0xD1, (byte) 0x29, (byte) 0x02, (byte) 0x4E, (byte) 0x08, (byte) 0x8A, (byte) 0x67,
			(byte) 0xCC, (byte) 0x74, (byte) 0x02, (byte) 0x0B, (byte) 0xBE, (byte) 0xA6, (byte) 0x3B, (byte) 0x13,
			(byte) 0x9B, (byte) 0x22, (byte) 0x51, (byte) 0x4A, (byte) 0x08, (byte) 0x79, (byte) 0x8E, (byte) 0x34,
			(byte) 0x04, (byte) 0xDD, (byte) 0xEF, (byte) 0x95, (byte) 0x19, (byte) 0xB3, (byte) 0xCD, (byte) 0x3A,
			(byte) 0x43, (byte) 0x1B, (byte) 0x30, (byte) 0x2B, (byte) 0x0A, (byte) 0x6D, (byte) 0xF2, (byte) 0x5F,
			(byte) 0x14, (byte) 0x37, (byte) 0x4F, (byte) 0xE1, (byte) 0x35, (byte) 0x6D, (byte) 0x6D, (byte) 0x51,
			(byte) 0xC2, (byte) 0x45, (byte) 0xE4, (byte) 0x85, (byte) 0xB5, (byte) 0x76, (byte) 0x62, (byte) 0x5E,
			(byte) 0x7E, (byte) 0xC6, (byte) 0xF4, (byte) 0x4C, (byte) 0x42, (byte) 0xE9, (byte) 0xA6, (byte) 0x37,
			(byte) 0xED, (byte) 0x6B, (byte) 0x0B, (byte) 0xFF, (byte) 0x5C, (byte) 0xB6, (byte) 0xF4, (byte) 0x06,
			(byte) 0xB7, (byte) 0xED, (byte) 0xEE, (byte) 0x38, (byte) 0x6B, (byte) 0xFB, (byte) 0x5A, (byte) 0x89,
			(byte) 0x9F, (byte) 0xA5, (byte) 0xAE, (byte) 0x9F, (byte) 0x24, (byte) 0x11, (byte) 0x7C, (byte) 0x4B,
			(byte) 0x1F, (byte) 0xE6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xEC, (byte) 0xE6,
			(byte) 0x53, (byte) 0x81, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF };

	private static final int SSH_MSG_KEXDH_INIT = 30;
	private static final int SSH_MSG_KEXDH_REPLY = 31;

	// static final int RSA = 0;
	// static final int DSS = 1;
	// private int type = 0;

	private int state;

	DH dh;
	// HASH sha;

	// byte[] K;
	// byte[] H;

	byte[] V_S;
	byte[] V_C;
	byte[] I_S;
	byte[] I_C;

	// byte[] K_S;

	byte[] e;

	private CustomBuffer buf;
	private CustomPacket packet;

	private CustomSession session;


	@Override
	public void init(CustomSession session, ChannelHandlerContext context, ChannelEvent evt, byte[] V_S, byte[] V_C,
			byte[] I_S, byte[] I_C) throws Exception {
		this.session = session;
		init(context, evt, V_S, V_C, I_S, I_C);
	}

	public void init(ChannelHandlerContext context, ChannelEvent evt, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C)
			throws Exception {
		// this.channel = channel;
		this.V_S = V_S;
		this.V_C = V_C;
		this.I_S = I_S;
		this.I_C = I_C;

		// sha=new SHA1();
		// sha.init();
		try {
			Class<?> c = Class.forName(Config.getConfig("sha-1"));
			sha = (HASH) (c.newInstance());
			sha.init();
		} catch (Exception e) {
			logger.error("init failed", e);
		}

		buf = new CustomBuffer();
		packet = new CustomPacket(buf);

		try {
			Class<?> c = Class.forName(Config.getConfig("dh"));
			dh = (DH) (c.newInstance());
			dh.init();
		} catch (Exception e) {
			// System.err.println(e);
			throw e;
		}

		dh.setP(p);
		dh.setG(g);

		// The client responds with:
		// byte SSH_MSG_KEXDH_INIT(30)
		// mpint e <- g^x mod p
		// x is a random number (1 < x < (p-1)/2)

		e = dh.getE();

		packet.reset();
		buf.putByte((byte) SSH_MSG_KEXDH_INIT);
		buf.putMPInt(e);
		session.write(packet, context, evt);

		logger.info("SSH_MSG_KEXDH_INIT sent");
		logger.info("expecting SSH_MSG_KEXDH_REPLY");

		state = SSH_MSG_KEXDH_REPLY;
	}

	@Override
	public boolean next(CustomBuffer _buf) throws Exception {
		int i, j;

		switch (state) {
		case SSH_MSG_KEXDH_REPLY:
			// The server responds with:
			// byte SSH_MSG_KEXDH_REPLY(31)
			// string server public host key and certificates (K_S)
			// mpint f
			// string signature of H
			j = _buf.getInt();
			j = _buf.getByte();
			j = _buf.getByte();
			if (j != 31) {
				logger.warn("type: must be 31: {}", j);
				return false;
			}

			K_S = _buf.getString();

			byte[] f = _buf.getMPInt();
			byte[] sig_of_H = _buf.getString();

			dh.setF(f);

			dh.checkRange();

			K = normalize(dh.getK());

			// The hash H is computed as the HASH hash of the concatenation of
			// the
			// following:
			// string V_C, the client's version string (CR and NL excluded)
			// string V_S, the server's version string (CR and NL excluded)
			// string I_C, the payload of the client's SSH_MSG_KEXINIT
			// string I_S, the payload of the server's SSH_MSG_KEXINIT
			// string K_S, the host key
			// mpint e, exchange value sent by the client
			// mpint f, exchange value sent by the server
			// mpint K, the shared secret
			// This value is called the exchange hash, and it is used to
			// authenti-
			// cate the key exchange.
			buf.reset();
			buf.putString(V_C);
			buf.putString(V_S);
			buf.putString(I_C);
			buf.putString(I_S);
			buf.putString(K_S);
			buf.putMPInt(e);
			buf.putMPInt(f);
			buf.putMPInt(K);
			byte[] foo = new byte[buf.getLength()];
			buf.getByte(foo);
			sha.update(foo, 0, foo.length);
			H = sha.digest();
			// System.err.print("H -> "); //dump(H, 0, H.length);

			i = 0;
			j = 0;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			String alg = SSHUtil.byte2str(K_S, i, j);
			i += j;

			boolean result = verify(alg, K_S, i, sig_of_H);

			state = STATE_END;
			return result;
		}
		return false;
	}

	// public boolean next(Buffer _buf) throws Exception {
	// int i, j;
	// switch (state) {
	// case SSH_MSG_KEXDH_REPLY:
	// j = _buf.readInt();
	// j = _buf.readByte();
	// j = _buf.readByte();
	// if (j != 31) {
	// System.err.println("type: must be 31 " + j);
	// return false;
	// }
	//
	// K_S = SSHUtil.readString(_buf);
	// byte[] f = getMPInt(_buf);
	// byte[] sig_of_H = SSHUtil.readString(_buf);
	//
	// dh.setF(f);
	// dh.checkRange();
	//
	// K = normalize(dh.getK());
	//
	// // The hash H is computed as the HASH hash of the concatenation of
	// // the
	// // following:
	// // string V_C, the client's version string (CR and NL excluded)
	// // string V_S, the server's version string (CR and NL excluded)
	// // string I_C, the payload of the client's SSH_MSG_KEXINIT
	// // string I_S, the payload of the server's SSH_MSG_KEXINIT
	// // string K_S, the host key
	// // mpint e, exchange value sent by the client
	// // mpint f, exchange value sent by the server
	// // mpint K, the shared secret
	// // This value is called the exchange hash, and it is used to
	// // authenti-
	// // cate the key exchange.
	// buf.reset();
	// buf.putString(V_C);
	// buf.putString(V_S);
	// buf.putString(I_C);
	// buf.putString(I_S);
	// buf.putString(K_S);
	// buf.putMPInt(e);
	// buf.putMPInt(f);
	// buf.putMPInt(K);
	// byte[] foo = new byte[buf.getLength()];
	// buf.getByte(foo);
	// sha.update(foo, 0, foo.length);
	// H = sha.digest();
	//
	// i = 0;
	// j = 0;
	// j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) |
	// ((K_S[i++] << 8) & 0x0000ff00)
	// | ((K_S[i++]) & 0x000000ff);
	// String alg = SSHUtil.byte2str(K_S, i, j);
	// i += j;
	//
	// boolean result = verify(alg, K_S, i, sig_of_H);
	// state = STATE_END;
	// return result;
	// }
	// return false;
	// }

	public byte[] getMPInt(ChannelBuffer buf) {
		int i = buf.readInt();
		if (i < 0 || // bigger than 0x7fffffff
				i > 8 * 1024) {
			// TODO: an exception should be thrown.
			i = 8 * 1024; // the session will be broken, but working around
							// OOME.
		}
		// byte[] foo = new byte[i];
		// getByte(buf, foo, 0, i);
		return buf.readBytes(i).array();
	}

	void getByte(ChannelBuffer buf, byte[] foo, int start, int len) {
		int s = buf.arrayOffset();
		System.arraycopy(buf.array(), s, foo, start, len);
		s += len;
	}

	public int getInt(ChannelBuffer buf) {
		int foo = getShort(buf);
		foo = ((foo << 16) & 0xffff0000) | (getShort(buf) & 0xffff);
		return foo;
	}

	int getShort(ChannelBuffer buf) {
		int foo = getByte(buf);
		foo = ((foo << 8) & 0xff00) | (getByte(buf) & 0xff);
		return foo;
	}

	public int getByte(ChannelBuffer buf) {
		int s = buf.arrayOffset();
		return (buf.array()[s++] & 0xff);
	}

	@Override
	public int getState() {
		return state;
	}

}
