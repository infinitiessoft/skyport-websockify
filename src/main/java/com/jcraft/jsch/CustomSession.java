package com.jcraft.jsch;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.consoleproxy.ssh.Config;
import com.infinities.skyport.vnc.util.SSHUtil;

public class CustomSession {

	private static final byte[] keepalivemsg = SSHUtil.str2byte("keepalive@jcraft.com");
	public static final int buffer_margin = 32 + // maximum padding length
	20 + // maximum mac length
	32; // margin for deflater; deflater may inflate data
	public static final int PACKET_MAX_SIZE = 256 * 1024;
	public static final int SSH_MSG_DISCONNECT = 1;
	public static final int SSH_MSG_IGNORE = 2;
	public static final int SSH_MSG_UNIMPLEMENTED = 3;
	public static final int SSH_MSG_DEBUG = 4;
	public static final int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;
	public static final Logger logger = LoggerFactory.getLogger(CustomSession.class);

	private Random random;

	private boolean in_kex = false;

	private long kex_start_time = 0L;

	private Cipher s2ccipher;
	private Cipher c2scipher;
	private MAC s2cmac;
	private MAC c2smac;

	private byte[] s2cmac_result1;
	private byte[] s2cmac_result2;

	private Compression deflater;
	private Compression inflater;

	private int s2ccipher_size = 8;
	private int c2scipher_size = 8;

	private int seqi = 0;
	private int seqo = 0;
	private int timeout = 0;

	private String[] guess = null;

	private byte[] V_S; // server version
	private byte[] V_C = SSHUtil.str2byte("SSH-2.0-JSCH-" + Config.VERSION); // client
	private byte[] I_C; // the payload of the client's SSH_MSG_KEXINIT
	private byte[] I_S; // the payload of the server's SSH_MSG_KEXINIT

	private byte[] session_id;

	private byte[] IVc2s;
	private byte[] IVs2c;
	private byte[] Ec2s;
	private byte[] Es2c;
	private byte[] MACc2s;
	private byte[] MACs2c;

	private Object lock = new Object();

	private boolean isAuthed = false;

	private String host = "127.0.0.1";
	private int port = 22;

	private String username = null;
	private byte[] password = null;

	private UserInfo userinfo;

	private CustomKeyExchange kex = null;
	private CustomUserAuth userAuth;
	private int[] uncompress_len = new int[1];
	private volatile boolean isConnected = false;
	private GlobalRequestReply grr = new GlobalRequestReply();
	private CustomChannelShell channel;

	private int max_auth_tries = 6;
	private int auth_failures = 0;

	private Identity identity;


	public CustomSession(String username, String host, int port, String password) throws JSchException {
		// super(new JSch(), username, host, port);
		this.setUsername(username);
		logger.debug("username: {}", username);
		this.host = host;
		this.port = port;
		this.password = password.getBytes();

		try {
			Class<?> c = Class.forName(Config.getConfig("random"));
			random = (Random) (c.newInstance());
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e);
		}
		CustomPacket.setRandom(random);

	}

	// readerIndex = s, capacity or writableBytes = index, readableBytes =
	// length
	public CustomBuffer read(CustomBuffer buf, ChannelBuffer buffer) throws Exception {
		int j = 0;
		while (true) {
			buf.reset();
			buffer.readBytes(buf.getBuffer(), buf.getIndex(), s2ccipher_size);
			// logger.debug("buffer:{}", new String(buf.getBuffer()));s
			buf.setIndex(buf.getIndex() + s2ccipher_size);
			if (s2ccipher != null) {
				s2ccipher.update(buf.getBuffer(), 0, s2ccipher_size, buf.getBuffer(), 0);
			}
			j = ((buf.getBuffer()[0] << 24) & 0xff000000) | ((buf.getBuffer()[1] << 16) & 0x00ff0000)
					| ((buf.getBuffer()[2] << 8) & 0x0000ff00) | ((buf.getBuffer()[3]) & 0x000000ff);
			// RFC 4253 6.1. Maximum Packet Length
			logger.debug("j: {}", j);
			if (j < 5 || j > PACKET_MAX_SIZE) {
				start_discard(buffer, buf, s2ccipher, s2cmac, j, PACKET_MAX_SIZE);
			}
			int need = j + 4 - s2ccipher_size;
			// if(need<0){
			// throw new IOException("invalid data");
			// }
			logger.debug("need: {}", need);
			if ((buf.getIndex() + need) > buf.getBuffer().length) {
				byte[] foo = new byte[buf.getIndex() + need];
				System.arraycopy(buf.getBuffer(), 0, foo, 0, buf.getIndex());
				buf.setBuffer(foo);
			}

			if ((need % s2ccipher_size) != 0) {
				String message = "Bad packet length " + need;
				logger.error(message);
				start_discard(buffer, buf, s2ccipher, s2cmac, j, PACKET_MAX_SIZE - s2ccipher_size);
			}

			if (need > 0) {
				System.err.println("buffer size:" + buf.getBuffer().length + ", index:" + buf.getIndex() + " need:" + need
						+ ", " + buffer.readableBytes());

				buffer.readBytes(buf.getBuffer(), buf.getIndex(), need);
				buf.setIndex(buf.getIndex() + need);
				if (s2ccipher != null) {
					s2ccipher.update(buf.getBuffer(), s2ccipher_size, need, buf.getBuffer(), s2ccipher_size);
				}
			}

			if (s2cmac != null) {
				logger.debug("seqi: {}", seqi);
				s2cmac.update(seqi);
				s2cmac.update(buf.getBuffer(), 0, buf.getIndex());
				logger.debug("s2cmac_result1: {}, s2cmac_result2:{}", new Object[] { s2cmac_result1.length,
						s2cmac_result2.length });
				s2cmac.doFinal(s2cmac_result1, 0);
				buffer.readBytes(s2cmac_result2, 0, s2cmac_result2.length);
				if (!java.util.Arrays.equals(s2cmac_result1, s2cmac_result2)) {
					if (need > PACKET_MAX_SIZE) {
						throw new IOException("MAC Error");
					}
					start_discard(buffer, buf, s2ccipher, s2cmac, j, PACKET_MAX_SIZE - need);
					continue;
				}
			}

			seqi++;

			if (inflater != null) {
				// inflater.uncompress(buf);
				int pad = buf.getBuffer()[4];
				uncompress_len[0] = buf.getIndex() - 5 - pad;
				byte[] foo = inflater.uncompress(buf.getBuffer(), 5, uncompress_len);
				if (foo != null) {
					buf.setBuffer(foo);
					buf.setIndex(5 + uncompress_len[0]);
				} else {
					logger.error("fail in inflater");
					break;
				}
			}

			int type = buf.getCommand() & 0xff;
			logger.debug("read type: {}", new Object[] { type });
			if (type == SSH_MSG_DISCONNECT) {
				buf.rewind();
				buf.getInt();
				buf.getShort();
				int reason_code = buf.getInt();
				byte[] description = buf.getString();
				byte[] language_tag = buf.getString();
				throw new JSchException("SSH_MSG_DISCONNECT: " + reason_code + " " + SSHUtil.byte2str(description) + " "
						+ SSHUtil.byte2str(language_tag));
				// break;
			} else if (type == SSH_MSG_IGNORE) {
			} else if (type == SSH_MSG_UNIMPLEMENTED) {
				buf.rewind();
				buf.getInt();
				buf.getShort();
				int reason_id = buf.getInt();
				logger.info("Received SSH_MSG_UNIMPLEMENTED for {}", reason_id);
			} else if (type == SSH_MSG_DEBUG) {
				buf.rewind();
				buf.getInt();
				buf.getShort();
				/*
				 * byte always_display=(byte)buf.getByte(); byte[]
				 * message=buf.getString(); byte[] language_tag=buf.getString();
				 * System.err.println("SSH_MSG_DEBUG:"+
				 * " "+Util.byte2str(message)+ " "+Util.byte2str(language_tag));
				 */
			} else if (type == SSH_MSG_CHANNEL_WINDOW_ADJUST) {
				buf.rewind();
				buf.getInt();
				buf.getShort();
				buf.getInt();
				if (channel == null) {
				} else {
					long size = buf.getUInt();
					logger.debug("int:{}", new Object[] { size });
					channel.addRemoteWindowSize(size);
				}

			} else if (type == CustomUserAuth.SSH_MSG_USERAUTH_SUCCESS) {
				isAuthed = true;
				if (inflater == null && deflater == null) {
					String method;
					method = guess[CustomKeyExchange.PROPOSAL_COMP_ALGS_CTOS];
					initDeflater(method);

					method = guess[CustomKeyExchange.PROPOSAL_COMP_ALGS_STOC];
					initInflater(method);
				}
				break;
			} else {
				break;
			}
		}
		buf.rewind();
		return buf;
	}

	public boolean isAuthed() {
		return isAuthed;
	}

	public void setAuthed(boolean isAuthed) {
		this.isAuthed = isAuthed;
	}

	public void start_discard(ChannelBuffer buffer, CustomBuffer buf, Cipher cipher, MAC mac, int packet_length, int discard)
			throws JSchException, IOException {
		MAC discard_mac = null;

		if (!cipher.isCBC()) {
			throw new JSchException("Packet corrupt");
		}

		if (packet_length != PACKET_MAX_SIZE && mac != null) {
			discard_mac = mac;
		}

		discard -= buf.getIndex();

		while (discard > 0) {
			buf.reset();
			int len = discard > buf.getBuffer().length ? buf.getBuffer().length : discard;
			buffer.readBytes(buf.getBuffer(), 0, len);
			if (discard_mac != null) {
				discard_mac.update(buf.getBuffer(), 0, len);
			}
			discard -= len;
		}

		if (discard_mac != null) {
			discard_mac.doFinal(buf.getBuffer(), 0);
		}

		throw new JSchException("Packet corrupt");
	}

	public CustomKeyExchange receiveKexinit(CustomBuffer buf, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		int j = buf.getInt();
		if (j != buf.getLength()) { // packet was compressed and
			buf.getByte(); // j is the size of deflated packet.
			I_S = new byte[buf.getIndex() - 5];
		} else {
			I_S = new byte[j - 1 - buf.getByte()];
		}
		System.arraycopy(buf.getBuffer(), buf.getS(), I_S, 0, I_S.length);

		if (!in_kex) { // We are in rekeying activated by the remote!
			sendKexinit(context, evt);
		}

		guess = CustomKeyExchange.guess(I_S, I_C);
		if (guess == null) {
			throw new JSchException("Algorithm negotiation fail");
		}

		if (!isAuthed
				&& (guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_CTOS].equals("none") || (guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_STOC]
						.equals("none")))) {
			throw new JSchException("NONE Cipher should not be chosen before authentification is successed.");
		}

		CustomKeyExchange kex = null;
		try {
			Class<?> c = Class.forName(Config.getConfig(guess[CustomKeyExchange.PROPOSAL_KEX_ALGS]));
			kex = (CustomKeyExchange) (c.newInstance());
		} catch (Exception e) {
			throw new JSchException(e.toString(), e);
		}

		kex.init(this, context, evt, V_S, V_C, I_S, I_C);
		return kex;
	}

	// readerIndex = s, capacity or writableBytes = index, readableBytes =
	// length
	public CustomKeyExchange receiveKexinit(ChannelBuffer buffer, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		int j = buffer.readInt();
		logger.debug("read integer: {}", j);
		if (j != buffer.readableBytes()) { // packet was compressed and
			buffer.readByte(); // j is the size of deflated packet.
			I_S = new byte[buffer.writableBytes() - 5];
		} else {
			I_S = new byte[j - 1 - buffer.readByte()];
		}
		logger.debug(
				"I_S size: {}, offset: {},{},{},{},{},{}",
				new Object[] { I_S.length, buffer.capacity(), buffer.readableBytes(), buffer.writableBytes(),
						buffer.writerIndex(), buffer.readerIndex(), buffer.array().length });
		// }
		System.arraycopy(buffer.array(), buffer.readerIndex(), I_S, 0, I_S.length);
		System.out.println("Test : " + new String(buffer.array()));
		if (!in_kex) { // We are in rekeying activated by the remote!
			sendKexinit(context, evt);
		}

		guess = CustomKeyExchange.guess(I_S, I_C);
		if (guess == null) {
			throw new JSchException("Algorithm negotiation fail");
		}

		if (!isAuthed
				&& (guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_CTOS].equals("none") || (guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_STOC]
						.equals("none")))) {
			throw new JSchException("NONE Cipher should not be chosen before authentification is successed.");
		}
		CustomKeyExchange kex = null;
		try {
			logger.debug("keyexchange: {}", Config.getConfig(guess[CustomKeyExchange.PROPOSAL_KEX_ALGS]));
			Class<?> c = Class.forName(Config.getConfig(guess[CustomKeyExchange.PROPOSAL_KEX_ALGS]));
			kex = (CustomKeyExchange) (c.newInstance());
		} catch (Exception e) {
			throw new JSchException(e.toString(), e);
		}

		kex.init(this, context, evt, V_S, V_C, I_S, I_C);
		return kex;
	}

	public void sendKexinit(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		if (in_kex)
			return;

		String cipherc2s = Config.getConfig("cipher.c2s");
		String ciphers2c = JSch.getConfig("cipher.s2c");

		String[] notAvailable = checkCiphers(JSch.getConfig("CheckCiphers"));
		if (notAvailable != null && notAvailable.length > 0) {
			cipherc2s = SSHUtil.diffString(cipherc2s, notAvailable);
			ciphers2c = SSHUtil.diffString(ciphers2c, notAvailable);
			if (cipherc2s == null || ciphers2c == null) {
				throw new JSchException("There are not any available ciphers.");
			}
		}

		String kex = JSch.getConfig("kex");
		String[] not_available_kexes = checkKexes(JSch.getConfig("CheckKexes"));

		if (not_available_kexes != null && not_available_kexes.length > 0) {
			kex = SSHUtil.diffString(kex, not_available_kexes);

			if (kex == null) {
				throw new JSchException("There are not any available kexes.");
			}
		}

		String server_host_key = Config.getConfig("server_host_key");
		String[] not_available_shks = checkSignatures(Config.getConfig("CheckSignatures"));
		if (not_available_shks != null && not_available_shks.length > 0) {
			server_host_key = SSHUtil.diffString(server_host_key, not_available_shks);
			if (server_host_key == null) {
				throw new JSchException("There are not any available sig algorithm.");
			}
		}

		in_kex = true;
		kex_start_time = System.currentTimeMillis();
		// byte SSH_MSG_KEXINIT(20)
		// byte[16] cookie (random bytes)
		// string kex_algorithms
		// string server_host_key_algorithms
		// string encryption_algorithms_client_to_server
		// string encryption_algorithms_server_to_client
		// string mac_algorithms_client_to_server
		// string mac_algorithms_server_to_client
		// string compression_algorithms_client_to_server
		// string compression_algorithms_server_to_client
		// string languages_client_to_server
		// string languages_server_to_client
		CustomBuffer buf = new CustomBuffer(); // send_kexinit may be invoked
		CustomPacket packet = new CustomPacket(buf); // by user thread.
		packet.reset();
		buf.putByte((byte) Config.SSH_MSG_KEXINIT);
		synchronized (random) {
			random.fill(buf.getBuffer(), buf.getIndex(), 16);
			buf.skip(16);
		}

		buf.putString(SSHUtil.str2byte(kex));
		buf.putString(SSHUtil.str2byte(server_host_key));
		buf.putString(SSHUtil.str2byte(cipherc2s));
		buf.putString(SSHUtil.str2byte(ciphers2c));
		buf.putString(SSHUtil.str2byte(Config.getConfig("mac.c2s")));
		buf.putString(SSHUtil.str2byte(Config.getConfig("mac.s2c")));
		buf.putString(SSHUtil.str2byte(Config.getConfig("compression.c2s")));
		buf.putString(SSHUtil.str2byte(Config.getConfig("compression.s2c")));
		buf.putString(SSHUtil.str2byte(Config.getConfig("lang.c2s")));
		buf.putString(SSHUtil.str2byte(Config.getConfig("lang.s2c")));
		buf.putByte((byte) 0);
		buf.putInt(0);

		buf.setOffSet(5);
		I_C = new byte[buf.getLength()];
		buf.getByte(I_C);
		write(packet, context, evt);
		logger.info("SSH_MSG_KEXINIT sent");

	}

	private String[] checkCiphers(String ciphers) {
		if (ciphers == null || ciphers.length() == 0)
			return null;
		logger.info("CheckCiphers: {}", ciphers);

		java.util.Vector<String> result = new java.util.Vector<String>();
		String[] _ciphers = SSHUtil.split(ciphers, ",");
		for (int i = 0; i < _ciphers.length; i++) {
			if (!checkCipher(JSch.getConfig(_ciphers[i]))) {
				result.addElement(_ciphers[i]);
			}
		}
		if (result.size() == 0)
			return null;
		String[] foo = new String[result.size()];
		System.arraycopy(result.toArray(), 0, foo, 0, result.size());

		for (int i = 0; i < foo.length; i++) {
			logger.info("{} is not available.", foo[i]);
		}

		return foo;
	}

	private String[] checkKexes(String kexes) {
		if (kexes == null || kexes.length() == 0)
			return null;
		logger.info("CheckKexes: {}", kexes);

		java.util.Vector<String> result = new java.util.Vector<String>();
		String[] _kexes = SSHUtil.split(kexes, ",");
		for (int i = 0; i < _kexes.length; i++) {

			if (!checkKex(this, JSch.getConfig(_kexes[i]))) {
				result.addElement(_kexes[i]);
			}
		}
		if (result.size() == 0)
			return null;
		String[] foo = new String[result.size()];
		System.arraycopy(result.toArray(), 0, foo, 0, result.size());

		return foo;
	}

	static boolean checkKex(CustomSession s, String kex) {
		try {
			Class<?> c = Class.forName(kex);
			CustomKeyExchange _c = (CustomKeyExchange) (c.newInstance());
			_c.init(s, null, null, null, null, null, null);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static boolean checkCipher(String cipher) {
		try {
			Class<?> c = Class.forName(cipher);
			Cipher _c = (Cipher) (c.newInstance());
			_c.init(Cipher.ENCRYPT_MODE, new byte[_c.getBlockSize()], new byte[_c.getIVSize()]);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String[] checkSignatures(String sigs) {
		if (sigs == null || sigs.length() == 0)
			return null;
		logger.info("CheckSignatures: {}", sigs);

		java.util.Vector<String> result = new java.util.Vector<String>();
		String[] _sigs = SSHUtil.split(sigs, ",");
		for (int i = 0; i < _sigs.length; i++) {
			try {
				Class<?> c = Class.forName(JSch.getConfig(_sigs[i]));
				final Signature sig = (Signature) (c.newInstance());
				sig.init();
			} catch (Exception e) {
				result.addElement(_sigs[i]);
			}
		}
		if (result.size() == 0)
			return null;
		String[] foo = new String[result.size()];
		System.arraycopy(result.toArray(), 0, foo, 0, result.size());

		return foo;
	}

	public void write(CustomPacket packet, CustomChannel c, int length, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		long t = getTimeout();
		while (true) {
			logger.debug("in_kex:{}", new Object[] { String.valueOf(in_kex) });
			if (in_kex) {
				if (t > 0L && (System.currentTimeMillis() - kex_start_time) > t) {
					throw new JSchException("timeout in wating for rekeying process.");
				}
				try {
					Thread.sleep(10);
				} catch (java.lang.InterruptedException e) {
				}
				;
				continue;
			}
			synchronized (c) {
				logger.debug("rwsize:{}, length:{}", new Object[] { c.rwsize, length });
				if (c.rwsize < length) {
					try {
						c.notifyme++;
						c.wait(100);
					} catch (java.lang.InterruptedException e) {
					} finally {
						c.notifyme--;
					}
				}
				logger.debug("rwsize:{}, length:{}", new Object[] { c.rwsize, length });
				if (c.rwsize >= length) {
					c.rwsize -= length;
					break;
				}

			}
			if (c.close || !c.isConnected()) {
				throw new IOException("channel is broken");
			}

			boolean sendit = false;
			int s = 0;
			byte command = 0;
			int recipient = -1;
			synchronized (c) {
				logger.debug("rwsize:{}", new Object[] { c.rwsize });
				if (c.rwsize > 0) {
					long len = c.rwsize;
					logger.debug("len:{}, length:{}", new Object[] { len, length });
					if (len > length) {
						len = length;
					}
					if (len != length) {
						s = packet.shift((int) len, (c2scipher != null ? c2scipher_size : 8),
								(c2smac != null ? c2smac.getBlockSize() : 0));
					}
					command = packet.buffer.getCommand();
					recipient = c.getRecipient();
					length -= len;
					c.rwsize -= len;
					sendit = true;
					logger.debug("command:{}, recipient:{}, length:{}, rwsize:{}", new Object[] { command, recipient,
							length, c.rwsize });
				}
			}
			if (sendit) {
				logger.debug("write and length:{}", new Object[] { length });
				_write(packet, context, evt);
				if (length == 0) {
					return;
				}
				packet.unshift(command, recipient, s, length);
			}

			synchronized (c) {
				if (in_kex) {
					continue;
				}
				if (c.rwsize >= length) {
					c.rwsize -= length;
					break;
				}

				// try{
				// System.out.println("1wait: "+c.rwsize);
				// c.notifyme++;
				// c.wait(100);
				// }
				// catch(java.lang.InterruptedException e){
				// }
				// finally{
				// c.notifyme--;
				// }
			}
		}
		_write(packet, context, evt);
	}

	public void write(CustomPacket packet, ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.debug("in_kex= {} {}", new Object[] { in_kex, packet.buffer.getCommand() });
		long t = getTimeout();
		while (in_kex) {
			if (t > 0L && (System.currentTimeMillis() - kex_start_time) > t) {
				throw new JSchException("timeout in wating for rekeying process.");
			}
			byte command = packet.getBuffer().getCommand();
			if (command == Config.SSH_MSG_KEXINIT || command == Config.SSH_MSG_NEWKEYS
					|| command == Config.SSH_MSG_KEXDH_INIT || command == Config.SSH_MSG_KEXDH_REPLY
					|| command == Config.SSH_MSG_KEX_DH_GEX_GROUP || command == Config.SSH_MSG_KEX_DH_GEX_INIT
					|| command == Config.SSH_MSG_KEX_DH_GEX_REPLY || command == Config.SSH_MSG_KEX_DH_GEX_REQUEST
					|| command == Config.SSH_MSG_DISCONNECT) {
				break;
			}
			try {
				Thread.sleep(10);
			} catch (java.lang.InterruptedException e) {
			}
			;
		}
		_write(packet, context, evt);
	}

	public long getKex_start_time() {
		return kex_start_time;
	}

	public void setKex_start_time(long kex_start_time) {
		this.kex_start_time = kex_start_time;
	}

	private void _write(CustomPacket packet, ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		synchronized (lock) {
			encode(packet);
			ChannelBuffer buffer = ChannelBuffers.copiedBuffer(packet.getBuffer().getBuffer(), 0, packet.getBuffer()
					.getIndex());
			DownstreamMessageEvent messageEvent = new DownstreamMessageEvent(context.getChannel(), evt.getFuture(), buffer,
					evt.getChannel().getRemoteAddress());
			context.sendDownstream(messageEvent);
			// channel.write(buffer);
			seqo++;
		}
	}

	public void encode(CustomPacket packet) throws Exception {
		// System.err.println("encode: " + packet.getBuffer().getCommand());
		// System.err.println("        " + packet.getBuffer().getIndex());
		// if(packet.buffer.getCommand()==96){
		// Thread.dumpStack();
		// }

		if (c2scipher != null) {
			// System.err.println("c2scipher");
			// packet.padding(c2scipher.getIVSize());
			packet.padding(c2scipher_size);
			int pad = packet.getBuffer().getBuffer()[4];
			synchronized (random) {
				random.fill(packet.getBuffer().getBuffer(), packet.getBuffer().getIndex() - pad, pad);
			}
		} else {
			// System.err.println("padding");
			packet.padding(8);
		}

		if (c2smac != null) {
			// System.err.println("cs2mac");
			c2smac.update(seqo);
			c2smac.update(packet.getBuffer().getBuffer(), 0, packet.getBuffer().getIndex());
			c2smac.doFinal(packet.getBuffer().getBuffer(), packet.getBuffer().getIndex());
		}
		if (c2scipher != null) {
			// System.err.println("c2scipher");
			byte[] buf = packet.getBuffer().getBuffer();
			c2scipher.update(buf, 0, packet.getBuffer().getIndex(), buf, 0);
		}
		if (c2smac != null) {
			// System.err.println("c2smac");
			packet.getBuffer().skip(c2smac.getBlockSize());
		}

		// System.err.println("after encode: " +
		// packet.getBuffer().getCommand());
		// System.err.println("              " + packet.getBuffer().getIndex());
	}

	public int getTimeout() {
		return timeout;
	}

	// public void checkHost(String chost, int port, CustomKeyExchange kex)
	// throws JSchException {
	// String shkc = Config.getConfig("StrictHostKeyChecking");
	// if (hostKeyAlias != null) {
	// chost = hostKeyAlias;
	// }
	//
	// byte[] K_S = kex.getHostKey();
	// String key_type = kex.getKeyType();
	// String key_fprint = kex.getFingerPrint();
	//
	// if (hostKeyAlias == null && port != 22) {
	// chost = ("[" + chost + "]:" + port);
	// }
	//
	// HostKeyRepository hkr = getHostKeyRepository();
	//
	// String hkh = Config.getConfig("HashKnownHosts");
	// hostkey = new HostKey(chost, K_S);
	//
	// int i = 0;
	// synchronized (hkr) {
	// i = hkr.check(chost, K_S);
	// }
	//
	// boolean insert = false;
	// if ((shkc.equals("ask") || shkc.equals("yes")) && i ==
	// HostKeyRepository.CHANGED) {
	// String file = null;
	// synchronized (hkr) {
	// file = hkr.getKnownHostsRepositoryID();
	// }
	// if (file == null) {
	// file = "known_hosts";
	// }
	//
	// boolean b = false;
	//
	// if (userinfo != null) {
	// String message = "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!\n"
	// + "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!\n"
	// +
	// "Someone could be eavesdropping on you right now (man-in-the-middle attack)!\n"
	// + "It is also possible that the " + key_type +
	// " host key has just been changed.\n"
	// + "The fingerprint for the " + key_type +
	// " key sent by the remote host is\n" + key_fprint + ".\n"
	// + "Please contact your system administrator.\n" +
	// "Add correct host key in " + file
	// + " to get rid of this message.";
	// if (shkc.equals("ask")) {
	// b = userinfo.promptYesNo(message +
	// "\nDo you want to delete the old key and insert the new key?");
	// } else { // shkc.equals("yes")
	// userinfo.showMessage(message);
	// }
	// }
	//
	// if (!b) {
	// throw new JSchException("HostKey has been changed: " + chost);
	// }
	//
	// synchronized (hkr) {
	// hkr.remove(chost, kex.getKeyAlgorithName(), null);
	// insert = true;
	// }
	// }
	//
	// if ((shkc.equals("ask") || shkc.equals("yes")) && (i !=
	// HostKeyRepository.OK) && !insert) {
	//
	// if (shkc.equals("yes")) {
	// throw new JSchException("reject HostKey: " + host);
	// }
	//
	// if (userinfo != null) {
	// userinfo.promptYesNo("The authenticity of host '" + host +
	// "' can't be established.\n" + key_type
	// + " key fingerprint is " + key_fprint + ".\n" +
	// "Are you sure you want to continue connecting?");
	// insert = true;
	// } else {
	// if (i == HostKeyRepository.NOT_INCLUDED)
	// throw new JSchException("UnknownHostKey: " + host + ". " + key_type +
	// " key fingerprint is "
	// + key_fprint);
	// else
	// throw new JSchException("HostKey has been changed: " + host);
	// }
	// }
	//
	// if (shkc.equals("no") && HostKeyRepository.NOT_INCLUDED == i) {
	// insert = true;
	// }
	//
	// if (i == HostKeyRepository.OK) {
	// HostKey[] keys = hkr.getHostKey(chost, kex.getKeyAlgorithName());
	// String _key = SSHUtil.byte2str(SSHUtil.toBase64(K_S, 0, K_S.length));
	// for (int j = 0; j < keys.length; j++) {
	// if (keys[i].getKey().equals(_key) &&
	// keys[j].getMarker().equals("@revoked")) {
	// if (userinfo != null) {
	// userinfo.showMessage("The " + key_type + " host key for " + host +
	// " is marked as revoked.\n"
	// + "This could mean that a stolen key is being used to " +
	// "impersonate this host.");
	// }
	//
	// logger.debug("Host '" + host + "' has provided revoked key.");
	// throw new JSchException("revoked HostKey: " + host);
	// }
	// }
	// }
	//
	// if (i == HostKeyRepository.OK) {
	// logger.debug("Host '" + host + "' is known and matches the " + key_type +
	// " host key");
	// }
	//
	// if (insert) {
	// logger.warn("Permanently added '" + host +
	// "' ({}) to the list of known hosts.", key_type);
	// }
	//
	// if (insert) {
	// System.err.println(" insert true");
	// synchronized (hkr) {
	// hkr.add(hostkey, userinfo);
	// }
	// }
	// }
	//
	// public HostKeyRepository getHostKeyRepository() {
	// if (hostkeyRepository == null)
	// return jsch.getHostKeyRepository();
	// return hostkeyRepository;
	// }

	public void sendKeepAliveMsg(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);
		packet.reset();
		buf.putByte((byte) Config.SSH_MSG_GLOBAL_REQUEST);
		buf.putString(keepalivemsg);
		buf.putByte((byte) 1);
		write(packet, context, evt);
	}

	public void send_newkeys(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomBuffer buf = new CustomBuffer(); // send_kexinit may be invoked
		CustomPacket packet = new CustomPacket(buf); // by user thread.
		packet.reset();
		buf.putByte((byte) Config.SSH_MSG_NEWKEYS);
		write(packet, context, evt);
		logger.debug("SSH_MSG_NEWKEYS sent");
	}

	public void receive_newkeys(CustomKeyExchange kex) throws Exception {
		updateKeys(kex);
		in_kex = false;
	}

	private void updateKeys(CustomKeyExchange kex) throws Exception {
		byte[] K = kex.getK();
		byte[] H = kex.getH();
		HASH hash = kex.getHash();

		if (session_id == null) {
			session_id = new byte[H.length];
			System.arraycopy(H, 0, session_id, 0, H.length);
		}

		/*
		 * Initial IV client to server: HASH (K || H || "A" || session_id)
		 * Initial IV server to client: HASH (K || H || "B" || session_id)
		 * Encryption key client to server: HASH (K || H || "C" || session_id)
		 * Encryption key server to client: HASH (K || H || "D" || session_id)
		 * Integrity key client to server: HASH (K || H || "E" || session_id)
		 * Integrity key server to client: HASH (K || H || "F" || session_id)
		 */
		CustomBuffer buf = new CustomBuffer();
		buf.reset();
		buf.putMPInt(K);
		buf.putByte(H);
		buf.putByte((byte) 0x41);
		buf.putByte(session_id);
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		IVc2s = hash.digest();

		int j = buf.getIndex() - session_id.length - 1;

		buf.getBuffer()[j]++;
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		IVs2c = hash.digest();

		buf.getBuffer()[j]++;
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		Ec2s = hash.digest();

		buf.getBuffer()[j]++;
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		Es2c = hash.digest();

		buf.getBuffer()[j]++;
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		MACc2s = hash.digest();

		buf.getBuffer()[j]++;
		hash.update(buf.getBuffer(), 0, buf.getIndex());
		MACs2c = hash.digest();

		try {
			Class<?> c;
			String method;

			method = guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_STOC];

			c = Class.forName(Config.getConfig(method));
			s2ccipher = (Cipher) (c.newInstance());
			while (s2ccipher.getBlockSize() > Es2c.length) {
				buf.reset();
				buf.putMPInt(K);
				buf.putByte(H);
				buf.putByte(Es2c);
				hash.update(buf.getBuffer(), 0, buf.getIndex());
				byte[] foo = hash.digest();
				byte[] bar = new byte[Es2c.length + foo.length];
				System.arraycopy(Es2c, 0, bar, 0, Es2c.length);
				System.arraycopy(foo, 0, bar, Es2c.length, foo.length);
				Es2c = bar;
			}
			s2ccipher.init(Cipher.DECRYPT_MODE, Es2c, IVs2c);
			s2ccipher_size = s2ccipher.getIVSize();
			method = guess[CustomKeyExchange.PROPOSAL_MAC_ALGS_STOC];
			c = Class.forName(Config.getConfig(method));
			s2cmac = (MAC) (c.newInstance());
			MACs2c = expandKey(buf, K, H, MACs2c, hash, s2cmac.getBlockSize());
			s2cmac.init(MACs2c);
			// mac_buf=new byte[s2cmac.getBlockSize()];
			s2cmac_result1 = new byte[s2cmac.getBlockSize()];
			s2cmac_result2 = new byte[s2cmac.getBlockSize()];

			method = guess[CustomKeyExchange.PROPOSAL_ENC_ALGS_CTOS];
			c = Class.forName(Config.getConfig(method));
			c2scipher = (Cipher) (c.newInstance());
			while (c2scipher.getBlockSize() > Ec2s.length) {
				buf.reset();
				buf.putMPInt(K);
				buf.putByte(H);
				buf.putByte(Ec2s);
				hash.update(buf.getBuffer(), 0, buf.getIndex());
				byte[] foo = hash.digest();
				byte[] bar = new byte[Ec2s.length + foo.length];
				System.arraycopy(Ec2s, 0, bar, 0, Ec2s.length);
				System.arraycopy(foo, 0, bar, Ec2s.length, foo.length);
				Ec2s = bar;
			}
			c2scipher.init(Cipher.ENCRYPT_MODE, Ec2s, IVc2s);
			c2scipher_size = c2scipher.getIVSize();

			method = guess[CustomKeyExchange.PROPOSAL_MAC_ALGS_CTOS];
			c = Class.forName(Config.getConfig(method));
			c2smac = (MAC) (c.newInstance());
			MACc2s = expandKey(buf, K, H, MACc2s, hash, c2smac.getBlockSize());
			c2smac.init(MACc2s);

			method = guess[CustomKeyExchange.PROPOSAL_COMP_ALGS_CTOS];
			initDeflater(method);

			method = guess[CustomKeyExchange.PROPOSAL_COMP_ALGS_STOC];
			initInflater(method);
		} catch (Exception e) {
			if (e instanceof JSchException)
				throw e;
			throw new JSchException(e.toString(), e);
			// System.err.println("updatekeys: "+e);
		}
	}

	private byte[] expandKey(CustomBuffer buf, byte[] K, byte[] H, byte[] key, HASH hash, int required_length)
			throws Exception {
		byte[] result = key;
		int size = hash.getBlockSize();
		while (result.length < required_length) {
			buf.reset();
			buf.putMPInt(K);
			buf.putByte(H);
			buf.putByte(result);
			hash.update(buf.getBuffer(), 0, buf.getIndex());
			byte[] tmp = new byte[result.length + size];
			System.arraycopy(result, 0, tmp, 0, result.length);
			System.arraycopy(hash.digest(), 0, tmp, result.length, size);
			SSHUtil.bzero(result);
			result = tmp;
		}
		return result;
	}

	public void initInflater(String method) throws JSchException {
		if (method.equals("none")) {
			inflater = null;
			return;
		}
		String foo = Config.getConfig(method);
		if (foo != null) {
			if (method.equals("zlib") || (isAuthed && method.equals("zlib@openssh.com"))) {
				try {
					Class<?> c = Class.forName(foo);
					inflater = (Compression) (c.newInstance());
					inflater.init(Compression.INFLATER, 0);
				} catch (Exception ee) {
					throw new JSchException(ee.toString(), ee);
					// System.err.println(foo+" isn't accessible.");
				}
			}
		}
	}

	public void initDeflater(String method) throws JSchException {
		if (method.equals("none")) {
			deflater = null;
			return;
		}
		String foo = Config.getConfig(method);
		if (foo != null) {
			if (method.equals("zlib") || (isAuthed && method.equals("zlib@openssh.com"))) {
				try {
					Class<?> c = Class.forName(foo);
					deflater = (Compression) (c.newInstance());
					int level = 6;
					try {
						level = Integer.parseInt(Config.getConfig("compression_level"));
					} catch (Exception ee) {
					}
					deflater.init(Compression.DEFLATER, level);
				} catch (NoClassDefFoundError ee) {
					throw new JSchException(ee.toString(), ee);
				} catch (Exception ee) {
					throw new JSchException(ee.toString(), ee);
					// System.err.println(foo+" isn't accessible.");
				}
			}
		}
	}

	public UserInfo getUserinfo() {
		return userinfo;
	}

	public void setUserinfo(UserInfo userinfo) {
		this.userinfo = userinfo;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public byte[] getV_S() {
		return V_S;
	}

	public void setV_S(byte[] v_S) {
		V_S = v_S;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public boolean isIn_kex() {
		return in_kex;
	}

	public void setIn_kex(boolean in_kex) {
		this.in_kex = in_kex;
	}

	public CustomKeyExchange getKex() {
		return kex;
	}

	public void setKex(CustomKeyExchange kex) {
		this.kex = kex;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public CustomUserAuth getUserAuth() {
		return userAuth;
	}

	public void setUserAuth(CustomUserAuth userAuth) {
		this.userAuth = userAuth;
	}

	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = password;
	}

	public byte[] getI_C() {
		return I_C;
	}

	public void setI_C(byte[] i_C) {
		I_C = i_C;
	}

	public String[] getGuess() {
		return guess;
	}

	public void setGuess(String[] guess) {
		this.guess = guess;
	}

	public byte[] getV_C() {
		return V_C;
	}

	public void setV_C(byte[] v_C) {
		V_C = v_C;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	// private int _setPortForwardingR(String bind_address, int rport, Channel
	// channel) throws JSchException {
	// synchronized (getGrr()) {
	// CustomBuffer buf = new CustomBuffer(100); // ??
	// CustomPacket packet = new CustomPacket(buf);
	//
	// String address_to_bind =
	// CustomChannelForwardedTCPIP.normalize(bind_address);
	//
	// getGrr().setThread(Thread.currentThread());
	// getGrr().setPort(rport);
	//
	// try {
	// // byte SSH_MSG_GLOBAL_REQUEST 80
	// // string "tcpip-forward"
	// // boolean want_reply
	// // string address_to_bind
	// // uint32 port number to bind
	// packet.reset();
	// buf.putByte((byte) Config.SSH_MSG_GLOBAL_REQUEST);
	// buf.putString(SSHUtil.str2byte("tcpip-forward"));
	// buf.putByte((byte) 1);
	// buf.putString(SSHUtil.str2byte(address_to_bind));
	// buf.putInt(rport);
	// write(packet, channel);
	// } catch (Exception e) {
	// getGrr().setThread(null);
	// if (e instanceof Throwable)
	// throw new JSchException(e.toString(), (Throwable) e);
	// throw new JSchException(e.toString());
	// }
	//
	// int count = 0;
	// int reply = getGrr().getReply();
	// while (count < 10 && reply == -1) {
	// try {
	// Thread.sleep(1000);
	// } catch (Exception e) {
	// }
	// count++;
	// reply = getGrr().getReply();
	// }
	// getGrr().setThread(null);
	// if (reply != 1) {
	// throw new JSchException("remote port forwarding failed for listen port "
	// + rport);
	// }
	// rport = getGrr().getPort();
	// }
	// return rport;
	// }
	//
	// public void setPortForwardingR(String bind_address, int rport, String
	// host, int lport, SocketFactory sf, Channel channel)
	// throws JSchException {
	// int allocated = _setPortForwardingR(bind_address, rport, channel);
	// CustomChannelForwardedTCPIP.addPort(this, bind_address, rport, allocated,
	// host, lport, sf);
	// }
	//
	// public void setPortForwardingR(int rport, String daemon) throws
	// JSchException {
	// setPortForwardingR(null, rport, daemon, null);
	// }
	//
	// public void setPortForwardingR(int rport, String daemon, Object[] arg)
	// throws JSchException {
	// setPortForwardingR(null, rport, daemon, arg);
	// }
	//
	// public void setPortForwardingR(String bind_address, int rport, String
	// daemon, Object[] arg, Channel channel)
	// throws JSchException {
	// int allocated = _setPortForwardingR(bind_address, rport, channel);
	// CustomChannelForwardedTCPIP.addPort(this, bind_address, rport, allocated,
	// daemon, arg);
	// }
	//
	// public String[] getPortForwardingR() throws JSchException {
	// return CustomChannelForwardedTCPIP.getPortForwarding(this);
	// }

	public GlobalRequestReply getGrr() {
		return grr;
	}

	public void setGrr(GlobalRequestReply grr) {
		this.grr = grr;
	}


	// public void requestPortForwarding() throws JSchException {
	// if (getConfig("ClearAllForwardings").equals("yes"))
	// return;
	//
	// ConfigRepository configRepository = jsch.getConfigRepository();
	// if (configRepository == null) {
	// return;
	// }
	//
	// ConfigRepository.Config config = configRepository.getConfig(org_host);
	//
	// String[] values = config.getValues("LocalForward");
	// if (values != null) {
	// for (int i = 0; i < values.length; i++) {
	// setPortForwardingL(values[i]);
	// }
	// }
	//
	// values = config.getValues("RemoteForward");
	// if (values != null) {
	// for (int i = 0; i < values.length; i++) {
	// setPortForwardingR(values[i]);
	// }
	// }
	// }

	public class GlobalRequestReply {

		private Thread thread = null;
		private int reply = -1;
		private int port = 0;


		public void setThread(Thread thread) {
			this.thread = thread;
			this.reply = -1;
		}

		public Thread getThread() {
			return thread;
		}

		public void setReply(int reply) {
			this.reply = reply;
		}

		public int getReply() {
			return this.reply;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}
	}


	public CustomChannelShell openChannel() throws JSchException {
		if (!isConnected) {
			throw new JSchException("session is down");
		}
		try {
			CustomChannelShell channel = new CustomChannelShell();
			addChannel(channel);
			channel.init();
			return channel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	void addChannel(CustomChannel channel) {
		channel.setSession(this);
	}

	public CustomChannelShell getChannel() {
		return channel;
	}

	public void setChannel(CustomChannelShell channel) {
		this.channel = channel;
	}

	public int getMax_auth_tries() {
		return max_auth_tries;
	}

	public void setMax_auth_tries(int max_auth_tries) {
		this.max_auth_tries = max_auth_tries;
	}

	public int getAuth_failures() {
		return auth_failures;
	}

	public void setAuth_failures(int auth_failures) {
		this.auth_failures = auth_failures;
	}

	public int getC2scipher_size() {
		return c2scipher_size;
	}

	public void setC2scipher_size(int c2scipher_size) {
		this.c2scipher_size = c2scipher_size;
	}

	public int getS2ccipher_size() {
		return s2ccipher_size;
	}

	public void setS2ccipher_size(int s2ccipher_size) {
		this.s2ccipher_size = s2ccipher_size;
	}

	public Cipher getS2ccipher() {
		return s2ccipher;
	}

	public void setS2ccipher(Cipher s2ccipher) {
		this.s2ccipher = s2ccipher;
	}

	public Cipher getC2scipher() {
		return c2scipher;
	}

	public void setC2scipher(Cipher c2scipher) {
		this.c2scipher = c2scipher;
	}

	public MAC getC2smac() {
		return c2smac;
	}

	public void setC2smac(MAC c2smac) {
		this.c2smac = c2smac;
	}

	public MAC getS2cmac() {
		return s2cmac;
	}

	public void setS2cmac(MAC s2cmac) {
		this.s2cmac = s2cmac;
	}

	public int getSeqi() {
		return seqi;
	}

	public void setSeqi(int seqi) {
		this.seqi = seqi;
	}

	public byte[] getS2cmac_result1() {
		return s2cmac_result1;
	}

	public void setS2cmac_result1(byte[] s2cmac_result1) {
		this.s2cmac_result1 = s2cmac_result1;
	}

	public byte[] getS2cmac_result2() {
		return s2cmac_result2;
	}

	public void setS2cmac_result2(byte[] s2cmac_result2) {
		this.s2cmac_result2 = s2cmac_result2;
	}

	public Compression getInflater() {
		return inflater;
	}

	public void setInflater(Compression inflater) {
		this.inflater = inflater;
	}

	public int[] getUncompress_len() {
		return uncompress_len;
	}

	public void setUncompress_len(int[] uncompress_len) {
		this.uncompress_len = uncompress_len;
	}

	public Compression getDeflater() {
		return deflater;
	}

	public void setDeflater(Compression deflater) {
		this.deflater = deflater;
	}

	public byte[] getSessionId() {
		return session_id;
	}

	public void close(Channel channel, String message, ChannelHandlerContext ctx, Object buffer) {
		setIn_kex(false);
		try {
			if (isConnected()) {
				CustomBuffer buf = new CustomBuffer();
				CustomPacket packet = new CustomPacket(buf);
				packet.reset();
				buf.checkFreeSize(1 + 4 * 3 + message.length() + 2 + CustomSession.buffer_margin);
				buf.putByte((byte) Config.SSH_MSG_DISCONNECT);
				buf.putInt(3);
				buf.putString(SSHUtil.str2byte(message));
				buf.putString(SSHUtil.str2byte("en"));
				DownstreamMessageEvent messageEvent = new DownstreamMessageEvent(channel, null, buffer,
						channel.getRemoteAddress());
				write(packet, ctx, messageEvent);
			}
		} catch (Exception ee) {
		}
		try {
			CustomChannelShell ch = getChannel();
			if (ch != null && ch.isClosed()) {
				channel.close();
			}
		} catch (Exception ee) {
		}
		setConnected(false);

	}

	public void addIdentity(String name, byte[] prvkey, byte[] pubkey, String passphrase) throws JSchException {
		byte[] _passphrase = null;
		if (passphrase != null) {
			_passphrase = Util.str2byte(passphrase);
		}
		Identity identity = CustomIdentityFile.newInstance(name, prvkey, pubkey);
		identity.setPassphrase(_passphrase);
		if (_passphrase != null) {
			Util.bzero(_passphrase);
		}
		this.identity = identity;
	}

	public Identity getIdentity() {
		return identity;
	}

}
