//package com.cloud.consoleproxy.ssh;
//
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.infinities.skyport.vnc.util.SSHUtil;
//import com.jcraft.jsch.DH;
//import com.jcraft.jsch.HASH;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.SignatureDSA;
//import com.jcraft.jsch.SignatureRSA;
//
//public class DHGEX extends KeyExchange {
//
//	private static final Logger logger = LoggerFactory.getLogger(DHGEX.class);
//	private static final int SSH_MSG_KEX_DH_GEX_GROUP = 31;
//	private static final int SSH_MSG_KEX_DH_GEX_INIT = 32;
//	private static final int SSH_MSG_KEX_DH_GEX_REPLY = 33;
//	private static final int SSH_MSG_KEX_DH_GEX_REQUEST = 34;
//
//	static int min = 1024;
//
//	// static int min=512;
//	static int preferred = 1024;
//	static int max = 1024;
//
//	// static int preferred=1024;
//	// static int max=2000;
//
//	static final int RSA = 0;
//	static final int DSS = 1;
//	private int type = 0;
//
//	private int state;
//
//	// com.jcraft.jsch.DH dh;
//	DH dh;
//
//	byte[] V_S;
//	byte[] V_C;
//	byte[] I_S;
//	byte[] I_C;
//
//	private Buffer buf;
//	private Packet packet;
//
//	private byte[] p;
//	private byte[] g;
//	private byte[] e;
//
//
//	// private byte[] f;
//	
//	private SshSession session;
//
//	public void init(SshSession session, Channel channel, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws Exception {
//		this.session = session;
//		init(channel, V_S, V_C, I_S, I_C);
//	}
//
//	public void init(Channel channel, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws Exception {
//		this.channel = channel;
//		this.V_S = V_S;
//		this.V_C = V_C;
//		this.I_S = I_S;
//		this.I_C = I_C;
//
//		try {
//			Class c = Class.forName(JSch.getConfig("sha-1"));
//			sha = (HASH) (c.newInstance());
//			sha.init();
//		} catch (Exception e) {
//			System.err.println(e);
//		}
//
//		buf = new Buffer();
//		packet = new Packet(buf);
//
//		try {
//			Class c = Class.forName(JSch.getConfig("dh"));
//			dh = (com.jcraft.jsch.DH) (c.newInstance());
//			dh.init();
//		} catch (Exception e) {
//			// System.err.println(e);
//			throw e;
//		}
//
//		packet.reset();
//		buf.putByte((byte) SSH_MSG_KEX_DH_GEX_REQUEST);
//		buf.putInt(min);
//		buf.putInt(preferred);
//		buf.putInt(max);
//		session.write(packet, channel);
//
//		logger.info("SSH_MSG_KEX_DH_GEX_REQUEST({}<{}<{}) sent", new Object[] { min, preferred, max });
//		logger.info("expecting SSH_MSG_KEX_DH_GEX_GROUP");
//
//		state = SSH_MSG_KEX_DH_GEX_GROUP;
//	}
//
//	public boolean next(ChannelBuffer _buf) throws Exception {
//		int i, j;
//		switch (state) {
//		case SSH_MSG_KEX_DH_GEX_GROUP:
//			// byte SSH_MSG_KEX_DH_GEX_GROUP(31)
//			// mpint p, safe prime
//			// mpint g, generator for subgroup in GF (p)
//			_buf.readInt();
//			_buf.readByte();
//			j = _buf.readByte();
//			if (j != SSH_MSG_KEX_DH_GEX_GROUP) {
//				System.err.println("type: must be SSH_MSG_KEX_DH_GEX_GROUP " + j);
//				return false;
//			}
//
//			p = _buf.array();
////			System.out.println("P : " + )
//			g = _buf.array();
//	
//			 for(int iii=0; iii<p.length; iii++){
//			 System.err.println("0x"+Integer.toHexString(p[iii]&0xff)+","); }
//			 System.err.println(""); for(int iii=0; iii<g.length; iii++){
//			 System.err.println("0x"+Integer.toHexString(g[iii]&0xff)+","); }
//			 
//			dh.setP(p);
//			dh.setG(g);
//
//			// The client responds with:
//			// byte SSH_MSG_KEX_DH_GEX_INIT(32)
//			// mpint e <- g^x mod p
//			// x is a random number (1 < x < (p-1)/2)
//
//			e = dh.getE();
//
//			packet.reset();
//			buf.putByte((byte) SSH_MSG_KEX_DH_GEX_INIT);
//			buf.putMPInt(e);
//			channel.write(packet);
//
//			logger.info("SSH_MSG_KEX_DH_GEX_INIT sent");
//			logger.info("expecting SSH_MSG_KEX_DH_GEX_REPLY");
//
//			state = SSH_MSG_KEX_DH_GEX_REPLY;
//			return true;
//			// break;
//
//		case SSH_MSG_KEX_DH_GEX_REPLY:
//			// The server responds with:
//			// byte SSH_MSG_KEX_DH_GEX_REPLY(33)
//			// string server public host key and certificates (K_S)
//			// mpint f
//			// string signature of H
//			j = _buf.readInt();
//			j = _buf.readByte();
//			j = _buf.readByte();
//			if (j != SSH_MSG_KEX_DH_GEX_REPLY) {
//				System.err.println("type: must be SSH_MSG_KEX_DH_GEX_REPLY " + j);
//				return false;
//			}
//
//			K_S = _buf.array();
//			// K_S is server_key_blob, which includes ....
//			// string ssh-dss
//			// impint p of dsa
//			// impint q of dsa
//			// impint g of dsa
//			// impint pub_key of dsa
//			// System.err.print("K_S: "); dump(K_S, 0, K_S.length);
//
//			byte[] f = _buf.array();
//			byte[] sig_of_H = _buf.array();
//
//			dh.setF(f);
//			K = dh.getK();
//
//			// The hash H is computed as the HASH hash of the concatenation of
//			// the
//			// following:
//			// string V_C, the client's version string (CR and NL excluded)
//			// string V_S, the server's version string (CR and NL excluded)
//			// string I_C, the payload of the client's SSH_MSG_KEXINIT
//			// string I_S, the payload of the server's SSH_MSG_KEXINIT
//			// string K_S, the host key
//			// uint32 min, minimal size in bits of an acceptable group
//			// uint32 n, preferred size in bits of the group the server should
//			// send
//			// uint32 max, maximal size in bits of an acceptable group
//			// mpint p, safe prime
//			// mpint g, generator for subgroup
//			// mpint e, exchange value sent by the client
//			// mpint f, exchange value sent by the server
//			// mpint K, the shared secret
//			// This value is called the exchange hash, and it is used to
//			// authenti-
//			// cate the key exchange.
//
//			buf.reset();
//			buf.putString(V_C);
//			buf.putString(V_S);
//			buf.putString(I_C);
//			buf.putString(I_S);
//			buf.putString(K_S);
//			buf.putInt(min);
//			buf.putInt(preferred);
//			buf.putInt(max);
//			buf.putMPInt(p);
//			buf.putMPInt(g);
//			buf.putMPInt(e);
//			buf.putMPInt(f);
//			buf.putMPInt(K);
//
//			byte[] foo = new byte[buf.getLength()];
//			buf.getByte(foo);
//			sha.update(foo, 0, foo.length);
//
//			H = sha.digest();
//
//			// System.err.print("H -> "); dump(H, 0, H.length);
//
//			i = 0;
//			j = 0;
//			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//					| ((K_S[i++]) & 0x000000ff);
//			String alg = SSHUtil.byte2str(K_S, i, j);
//			i += j;
//
//			boolean result = false;
//			if (alg.equals("ssh-rsa")) {
//				byte[] tmp;
//				byte[] ee;
//				byte[] n;
//
//				type = RSA;
//
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				ee = tmp;
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				n = tmp;
//
//				// SignatureRSA sig=new SignatureRSA();
//				// sig.init();
//
//				SignatureRSA sig = null;
//				try {
//					Class c = Class.forName(JSch.getConfig("signature.rsa"));
//					sig = (SignatureRSA) (c.newInstance());
//					sig.init();
//				} catch (Exception e) {
//					System.err.println(e);
//				}
//
//				sig.setPubKey(ee, n);
//				sig.update(H);
//				result = sig.verify(sig_of_H);
//
//				logger.info("ssh_rsa_verify: signature {}", result);
//
//			} else if (alg.equals("ssh-dss")) {
//				byte[] q = null;
//				byte[] tmp;
//
//				type = DSS;
//
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				p = tmp;
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				q = tmp;
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				g = tmp;
//				j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
//						| ((K_S[i++]) & 0x000000ff);
//				tmp = new byte[j];
//				System.arraycopy(K_S, i, tmp, 0, j);
//				i += j;
//				f = tmp;
//
//				// SignatureDSA sig=new SignatureDSA();
//				// sig.init();
//
//				SignatureDSA sig = null;
//				try {
//					Class c = Class.forName(JSch.getConfig("signature.dss"));
//					sig = (SignatureDSA) (c.newInstance());
//					sig.init();
//				} catch (Exception e) {
//					System.err.println(e);
//				}
//
//				sig.setPubKey(f, p, q, g);
//				sig.update(H);
//				result = sig.verify(sig_of_H);
//
//				logger.info("ssh_dss_verify: signature {}", result);
//
//			} else {
//				System.err.println("unknown alg");
//			}
//			state = STATE_END;
//			return result;
//		}
//		return false;
//	}
//
//	public String getKeyType() {
//		if (type == DSS)
//			return "DSA";
//		return "RSA";
//	}
//
//	public int getState() {
//		return state;
//	}
//
//}