package com.jcraft.jsch;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.ssh.handler.ChannelOpenReadTimeoutHandler;

public abstract class CustomChannel implements ChannelUpstreamHandler, ChannelDownstreamHandler {

	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(CustomChannel.class);
	public static final int SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
	public static final int SSH_MSG_CHANNEL_OPEN_FAILURE = 92;
	public static final int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;

	public static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
	public static final int SSH_OPEN_CONNECT_FAILED = 2;
	public static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;
	public static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

	static int index = 0;


	public static CustomChannelShell getChannel(String type) {
		return new CustomChannelShell();
	}


	int id;
	volatile int recipient = -1;
	protected byte[] type = Util.str2byte("foo");
	public volatile int lwsize_max = 0x100000;
	public volatile int lwsize = lwsize_max; // local initial window size
	public volatile int lmpsize = 0x4000; // local maximum packet size

	public volatile long rwsize = 0; // remote initial window size
	public volatile int rmpsize = 0; // remote maximum packet size

	public volatile boolean eof_local = false;
	public volatile boolean eof_remote = false;

	public volatile boolean close = false;
	private volatile boolean connected = false;
	private volatile boolean open_confirmation = false;

	volatile int exitstatus = -1;

	private volatile int reply = 0;
	volatile int connectTimeout = 0;

	private CustomSession session;

	int notifyme = 0;


	public synchronized void setRecipient(int foo) {
		this.recipient = foo;
		if (notifyme > 0)
			notifyAll();
	}

	public int getRecipient() {
		return recipient;
	}

	public void connect(ChannelHandlerContext context, ChannelEvent evt) throws JSchException {
		connect(0, context, evt);
	}

	public void connect(int connectTimeout, ChannelHandlerContext context, ChannelEvent evt) throws JSchException {
		this.connectTimeout = connectTimeout;
		try {
			sendChannelOpen(context, evt);
			// start(context);
		} catch (Exception e) {
			setConnected(false);
			disconnect(context, evt);
			if (e instanceof JSchException)
				throw (JSchException) e;
			throw new JSchException(e.toString(), e);
		}
	}

	public void setXForwarding(boolean foo) {
	}

	public void start(ChannelHandlerContext context, ChannelEvent evt) throws JSchException {
	}

	public boolean isEOF() {
		return eof_remote;
	}

	public void getData(CustomBuffer buf) {
		setRecipient(buf.getInt());
		setRemoteWindowSize(buf.getUInt());
		setRemotePacketSize(buf.getInt());
	}


	private int dataLen = 0;
	private CustomBuffer buffer = null;
	private CustomPacket packet = null;
	private boolean closed = false;
	private byte[] b = new byte[1];


	public void write(int w, ChannelHandlerContext context, ChannelEvent evt) throws java.io.IOException {
		b[0] = (byte) w;
		write(b, 0, 1, context, evt);
	}

	public void write(byte[] buf, int s, int l, ChannelHandlerContext context, ChannelEvent evt) throws java.io.IOException {
		if (packet == null) {
			initPacket();
		}

		if (closed) {
			throw new java.io.IOException("Already closed");
		}

		byte[] _buf = buffer.getBuffer();
		int _bufl = _buf.length;
		while (l > 0) {
			logger.debug("l:{}", l);
			int _l = l;
			if (l > _bufl - (14 + dataLen) - Session.buffer_margin) {
				_l = _bufl - (14 + dataLen) - Session.buffer_margin;
			}

			logger.debug("_l:{}", _l);
			if (_l <= 0) {
				logger.debug("flush");
				flush(context, evt);
				continue;
			}

			System.arraycopy(buf, s, _buf, 14 + dataLen, _l);
			dataLen += _l;
			s += _l;
			l -= _l;
		}
	}

	public void flush(ChannelHandlerContext context, ChannelEvent evt) throws java.io.IOException {
		if (closed) {
			throw new java.io.IOException("Already closed");
		}
		if (dataLen == 0)
			return;
		packet.reset();
		buffer.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
		buffer.putInt(recipient);
		buffer.putInt(dataLen);
		buffer.skip(dataLen);
		try {
			int foo = dataLen;
			dataLen = 0;
			synchronized (this) {
				if (!close)
					logger.debug("flush packet");
				getSession().write(packet, this, foo, context, evt);
			}
		} catch (Exception e) {
			close(context, evt);
			throw new java.io.IOException(e.toString());
		}

	}

	public synchronized void initPacket() throws java.io.IOException {
		logger.debug("rmpsize: {}", rmpsize);
		buffer = new CustomBuffer(rmpsize);
		packet = new CustomPacket(buffer);

		byte[] _buf = buffer.getBuffer();
		if (_buf.length - (14 + 0) - Session.buffer_margin <= 0) {
			buffer = null;
			packet = null;
			throw new IOException("failed to initialize the channel.");
		}

	}

	void setLocalWindowSizeMax(int foo) {
		this.lwsize_max = foo;
	}

	public void setLocalWindowSize(int foo) {
		this.lwsize = foo;
	}

	void setLocalPacketSize(int foo) {
		this.lmpsize = foo;
	}

	public synchronized void setRemoteWindowSize(long foo) {
		this.rwsize = foo;
	}

	public synchronized void addRemoteWindowSize(long foo) {
		logger.debug("add remote window size:{}", new Object[] { String.valueOf(foo) });
		this.rwsize += foo;
		if (notifyme > 0)
			notifyAll();
	}

	public void setRemotePacketSize(int foo) {
		this.rmpsize = foo;
	}

	public void run() {
	}

	public void write(byte[] foo, ChannelHandlerContext context) throws IOException {
		write(foo, 0, foo.length, context);
	}

	public void write(byte[] foo, int s, int l, ChannelHandlerContext context) throws IOException {
		try {
			ChannelBuffer buffer = ChannelBuffers.copiedBuffer(foo, s, l);
			context.sendUpstream(new UpstreamMessageEvent(context.getChannel(), buffer, null));
			// context.write(buffer);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void write_ext(byte[] foo, int s, int l, ChannelHandlerContext context) throws IOException {
		try {
			// ChannelBuffer buffer = ChannelBuffers.copiedBuffer(foo, s, l);
			// channel.write(buffer);
			ChannelBuffer buffer = ChannelBuffers.copiedBuffer(foo, s, l);
			context.sendUpstream(new UpstreamMessageEvent(context.getChannel(), buffer, null));
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void eof_remote() {
		eof_remote = true;
	}

	public void eof(ChannelHandlerContext context, ChannelEvent evt) {
		if (eof_local)
			return;
		eof_local = true;

		int i = getRecipient();
		if (i == -1)
			return;

		try {
			CustomBuffer buf = new CustomBuffer(100);
			CustomPacket packet = new CustomPacket(buf);
			packet.reset();
			buf.putByte((byte) Session.SSH_MSG_CHANNEL_EOF);
			buf.putInt(i);
			synchronized (this) {
				if (!close)
					getSession().write(packet, context, evt);
			}
		} catch (Exception e) {
			// System.err.println("Channel.eof");
			// e.printStackTrace();
		}
		/*
		 * if(!isConnected()){ disconnect(); }
		 */
	}

	/*
	 * http://www1.ietf.org/internet-drafts/draft-ietf-secsh-connect-24.txt
	 * 
	 * 5.3 Closing a Channel When a party will no longer send more data to a
	 * channel, it SHOULD send SSH_MSG_CHANNEL_EOF.
	 * 
	 * byte SSH_MSG_CHANNEL_EOF uint32 recipient_channel
	 * 
	 * No explicit response is sent to this message. However, the application
	 * may send EOF to whatever is at the other end of the channel. Note that
	 * the channel remains open after this message, and more data may still be
	 * sent in the other direction. This message does not consume window space
	 * and can be sent even if no window space is available.
	 * 
	 * When either party wishes to terminate the channel, it sends
	 * SSH_MSG_CHANNEL_CLOSE. Upon receiving this message, a party MUST send
	 * back a SSH_MSG_CHANNEL_CLOSE unless it has already sent this message for
	 * the channel. The channel is considered closed for a party when it has
	 * both sent and received SSH_MSG_CHANNEL_CLOSE, and the party may then
	 * reuse the channel number. A party MAY send SSH_MSG_CHANNEL_CLOSE without
	 * having sent or received SSH_MSG_CHANNEL_EOF.
	 * 
	 * byte SSH_MSG_CHANNEL_CLOSE uint32 recipient_channel
	 * 
	 * This message does not consume window space and can be sent even if no
	 * window space is available.
	 * 
	 * It is recommended that any data sent before this message is delivered to
	 * the actual destination, if possible.
	 */

	void close(ChannelHandlerContext context, ChannelEvent evt) {
		if (close)
			return;
		close = true;
		eof_local = eof_remote = true;

		int i = getRecipient();
		if (i == -1)
			return;

		try {
			CustomBuffer buf = new CustomBuffer(100);
			CustomPacket packet = new CustomPacket(buf);
			packet.reset();
			buf.putByte((byte) Session.SSH_MSG_CHANNEL_CLOSE);
			buf.putInt(i);
			synchronized (this) {
				getSession().write(packet, context, evt);
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public boolean isClosed() {
		return close;
	}

	// static void disconnect(Session session) {
	// Channel[] channels = null;
	// int count = 0;
	// synchronized (pool) {
	// channels = new Channel[pool.size()];
	// for (int i = 0; i < pool.size(); i++) {
	// try {
	// Channel c = ((Channel) (pool.elementAt(i)));
	// if (c.session == session) {
	// channels[count++] = c;
	// }
	// } catch (Exception e) {
	// }
	// }
	// }
	// for (int i = 0; i < count; i++) {
	// channels[i].disconnect();
	// }
	// }

	public void disconnect(ChannelHandlerContext context, ChannelEvent evt) {
		// System.err.println(this+":disconnect "+io+" "+connected);
		// Thread.dumpStack();

		try {

			synchronized (this) {
				if (!isConnected()) {
					return;
				}
				setConnected(false);
			}

			close(context, evt);
			eof_remote = eof_local = true;
		} finally {
			// Channel.del(this);
		}
	}

	public boolean isConnected() {
		CustomSession _session = this.session;
		if (_session != null) {
			return _session.isConnected() && connected;
		}
		return false;
	}

	public void sendSignal(String signal, ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomRequestSignal request = new CustomRequestSignal();
		request.setSignal(signal);
		request.request(getSession(), this, context, evt);
	}

	// public String toString(){
	// return "Channel: type="+new
	// String(type)+",id="+id+",recipient="+recipient+",window_size="+window_size+",packet_size="+packet_size;
	// }

	/*
	 * class OutputThread extends Thread{ Channel c; OutputThread(Channel c){
	 * this.c=c;} public void run(){c.output_thread();} }
	 */

	// class PassiveInputStream extends MyPipedInputStream {
	//
	// PipedOutputStream out;
	//
	//
	// PassiveInputStream(PipedOutputStream out, int size) throws IOException {
	// super(out, size);
	// this.out = out;
	// }
	//
	// PassiveInputStream(PipedOutputStream out) throws IOException {
	// super(out);
	// this.out = out;
	// }
	//
	// public void close() throws IOException {
	// if (out != null) {
	// this.out.close();
	// }
	// out = null;
	// }
	// }

	// class PassiveOutputStream extends PipedOutputStream {
	//
	// private MyPipedInputStream _sink = null;
	//
	//
	// PassiveOutputStream(PipedInputStream in, boolean resizable_buffer) throws
	// IOException {
	// super(in);
	// if (resizable_buffer && (in instanceof MyPipedInputStream)) {
	// this._sink = (MyPipedInputStream) in;
	// }
	// }
	//
	// public void write(int b) throws IOException {
	// if (_sink != null) {
	// _sink.checkSpace(1);
	// }
	// super.write(b);
	// }
	//
	// public void write(byte[] b, int off, int len) throws IOException {
	// if (_sink != null) {
	// _sink.checkSpace(len);
	// }
	// super.write(b, off, len);
	// }
	// }

	public void setExitStatus(int status) {
		exitstatus = status;
	}

	public int getExitStatus() {
		return exitstatus;
	}

	public void setSession(CustomSession session) {
		this.session = session;
	}

	public CustomSession getSession() throws JSchException {
		CustomSession _session = session;
		if (_session == null) {
			throw new JSchException("session is not available");
		}
		return _session;
	}

	public int getId() {
		return id;
	}

	protected void sendOpenConfirmation(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomBuffer buf = new CustomBuffer(100);
		CustomPacket packet = new CustomPacket(buf);
		packet.reset();
		buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
		buf.putInt(getRecipient());
		buf.putInt(id);
		buf.putInt(lwsize);
		buf.putInt(lmpsize);
		getSession().write(packet, context, evt);
	}

	protected void sendOpenFailure(int reasoncode, ChannelHandlerContext context, ChannelEvent evt) {
		try {
			CustomBuffer buf = new CustomBuffer(100);
			CustomPacket packet = new CustomPacket(buf);
			packet.reset();
			buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_FAILURE);
			buf.putInt(getRecipient());
			buf.putInt(reasoncode);
			buf.putString(Util.str2byte("open failed"));
			buf.putString(Util.empty);
			getSession().write(packet, context, evt);
		} catch (Exception e) {
		}
	}

	protected CustomPacket genChannelOpenPacket() {
		CustomBuffer buf = new CustomBuffer(100);
		CustomPacket packet = new CustomPacket(buf);
		// byte SSH_MSG_CHANNEL_OPEN(90)
		// string channel type //
		// uint32 sender channel // 0
		// uint32 initial window size // 0x100000(65536)
		// uint32 maxmum packet size // 0x4000(16384)
		packet.reset();
		buf.putByte((byte) 90);
		buf.putString(this.type);
		buf.putInt(this.id);
		buf.putInt(this.lwsize);
		buf.putInt(this.lmpsize);
		return packet;
	}

	protected void sendChannelOpen(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomSession _session = getSession();
		if (!_session.isConnected()) {
			throw new JSchException("session is down");
		}

		CustomPacket packet = genChannelOpenPacket();
		_session.write(packet, context, evt);

		// int retry = 2000;
		// long start = System.currentTimeMillis();
		long timeout = connectTimeout;

		// if (timeout != 0L)
		// retry = 1;
		synchronized (this) {
			ChannelOpenReadTimeoutHandler timeoutHandler = new ChannelOpenReadTimeoutHandler(this, new HashedWheelTimer(),
					(int) timeout / 1000);
			context.getPipeline().addFirst("channelOpenReadTimeoutHandler", timeoutHandler);
		}
	}

	public boolean isOpen_confirmation() {
		return open_confirmation;
	}

	public void setOpen_confirmation(boolean open_confirmation) {
		this.open_confirmation = open_confirmation;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public int getReply() {
		return reply;
	}

	public void setReply(int reply) {
		this.reply = reply;
	}
}
