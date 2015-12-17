package com.jcraft.jsch;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultExceptionEvent;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.infinities.skyport.util.JsonUtil;
import com.infinities.skyport.vnc.util.SSHUtil;

class CustomChannelSession extends CustomChannel {

	private org.slf4j.Logger logger = LoggerFactory.getLogger(CustomChannelSession.class);
	private static byte[] _session = Util.str2byte("session");

	protected boolean pty = false;

	protected String ttype = "vt100";
	protected int tcol = 80;
	protected int trow = 24;
	protected int twp = 640;
	protected int thp = 480;
	protected byte[] terminal_mode = null;
	private boolean pass = true;


	public CustomChannelSession() {
		super();
		type = _session;
		// io = new IO();
	}

	/**
	 * Allocate a Pseudo-Terminal. Refer to RFC4254 6.2. Requesting a
	 * Pseudo-Terminal.
	 *
	 * @param enable
	 */
	public void setPty(boolean enable) {
		pty = enable;
	}

	/**
	 * Set the terminal mode.
	 * 
	 * @param terminal_mode
	 */
	public void setTerminalMode(byte[] terminal_mode) {
		this.terminal_mode = terminal_mode;
	}

	/**
	 * Change the window dimension interactively. Refer to RFC4254 6.7. Window
	 * Dimension Change Message.
	 *
	 * @param col
	 *            terminal width, columns
	 * @param row
	 *            terminal height, rows
	 * @param wp
	 *            terminal width, pixels
	 * @param hp
	 *            terminal height, pixels
	 */
	public void setPtySize(int col, int row, int wp, int hp, ChannelHandlerContext context, ChannelEvent evt) {
		setPtyType(this.ttype, col, row, wp, hp);
		if (!pty || !isConnected()) {
			return;
		}
		try {
			CustomRequestWindowChange request = new CustomRequestWindowChange();
			request.setSize(col, row, wp, hp);
			request.request(getSession(), this, context, evt);
		} catch (Exception e) {
			// System.err.println("ChannelSessio.setPtySize: "+e);
		}
	}

	/**
	 * Set the terminal type. This method is not effective after
	 * Channel#connect().
	 *
	 * @param ttype
	 *            terminal type(for example, "vt100")
	 * @see #setPtyType(String, int, int, int, int)
	 */
	public void setPtyType(String ttype) {
		setPtyType(ttype, 80, 24, 640, 480);
	}

	/**
	 * Set the terminal type. This method is not effective after
	 * Channel#connect().
	 *
	 * @param ttype
	 *            terminal type(for example, "vt100")
	 * @param col
	 *            terminal width, columns
	 * @param row
	 *            terminal height, rows
	 * @param wp
	 *            terminal width, pixels
	 * @param hp
	 *            terminal height, pixels
	 */
	public void setPtyType(String ttype, int col, int row, int wp, int hp) {
		this.ttype = ttype;
		this.tcol = col;
		this.trow = row;
		this.twp = wp;
		this.thp = hp;
	}

	protected void sendRequests(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomSession _session = getSession();
		CustomRequest request;

		if (pty) {
			logger.debug("send pty request");
			request = new CustomRequestPtyReq();
			((CustomRequestPtyReq) request).setTType(ttype);
			((CustomRequestPtyReq) request).setTSize(tcol, trow, twp, thp);
			if (terminal_mode != null) {
				((CustomRequestPtyReq) request).setTerminalMode(terminal_mode);
			}
			request.request(_session, this, context, evt);
		}
	}

	public void run(ChannelBuffer buffer, ChannelHandlerContext context, ChannelEvent evt) {
		// System.err.println(this+":run >");

		CustomBuffer buf = new CustomBuffer(rmpsize);
		CustomPacket packet = new CustomPacket(buf);
		int i = -1;
		try {
			if (isConnected()) {
				int expect = buf.getBuffer().length - 14 - Session.buffer_margin;
				logger.debug("expect length:{}", new Object[] { expect });
				int actual = expect > buffer.readableBytes() ? buffer.readableBytes() : expect;
				logger.debug("actual length:{}", new Object[] { actual });
				buffer.readBytes(buf.getBuffer(), 14, actual);
				i = buffer.readerIndex();
				logger.debug("reader index:{}, recipient:{}", new Object[] { i, recipient });
				// i = new IO().in.read(buf.getBuffer(), 14,
				// buf.getBuffer().length - 14 - Session.buffer_margin);
				if (i == 0)
					return;
				if (i == -1) {
					eof(context, evt);
					return;
				}
				if (close)
					return;

				// System.out.println("write: "+i);
				packet.reset();
				buf.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
				buf.putInt(recipient);
				buf.putInt(i);
				buf.skip(i);
				getSession().write(packet, this, i, context, evt);
			}
		} catch (Exception e) {
			logger.warn("run failed", e);
			// System.err.println("# ChannelExec.run");
			// e.printStackTrace();
		}
	}

	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		if (evt instanceof DefaultExceptionEvent) {
			logger.warn("catch exception", ((DefaultExceptionEvent) evt).getCause());
		}

		if (!(evt instanceof MessageEvent)) {
			context.sendUpstream(evt);
			return;
		}

		// MessageEvent e = (MessageEvent) evt;
		// ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		// logger.debug("str: {}", new Object[] { new String(buffer.array()) });
		context.sendUpstream(evt);
		// run(buffer, context.getChannel());
	}

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.debug("pass handle down stream : {}, {}, {}", new Object[] { evt.getClass(), evt.toString(),
				context.getPipeline().getNames() });

		if (evt instanceof DefaultExceptionEvent) {
			logger.warn("catch exception", ((DefaultExceptionEvent) evt).getCause());
		}

		if (!(evt instanceof MessageEvent)) {
			context.sendDownstream(evt);
			return;
		}

		if (!pass) {
			MessageEvent e = (MessageEvent) evt;
			if (e.getMessage() instanceof ChannelBuffer) {
				ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
				logger.debug("receive raw {}", new String(buffer.array()));
				// byte[] base64Decoded = BaseEncoding.base64().decode(new
				// String(buffer.array()));
				// Encode the message to base64
				String msg = new String(buffer.array());
				logger.debug("receive decoded {}", msg);
				if (!Strings.isNullOrEmpty(msg)) {
					try {
						JsonNode jsonRoot = JsonUtil.readJson(msg);
						String command = null;
						if (jsonRoot.has("command")) {
							command = jsonRoot.get("command").asText();
						}
						logger.debug("Command : {}", command);

						Integer keyCode = null;
						if (jsonRoot.has("keyCode")) {
							keyCode = jsonRoot.get("keyCode").asInt();
						}

						logger.debug("KeyCode : {}", keyCode);

						if (keyCode != null) {
							if (SSHUtil.keyMap.containsKey(keyCode)) {
								byte[] buf = SSHUtil.keyMap.get(keyCode);
								// write(buf, 0, buf.length,
								// context.getChannel());
								ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(buf);
								run(channelBuffer, context, evt);
								DownstreamMessageEvent messageEvent = new DownstreamMessageEvent(context.getChannel(),
										evt.getFuture(), channelBuffer, evt.getChannel().getRemoteAddress());
								logger.debug("write user message");
								context.sendDownstream(messageEvent);
							}
						} else {
							if (command != null) {
								ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(command.getBytes());
								run(channelBuffer, context, evt);
								DownstreamMessageEvent messageEvent = new DownstreamMessageEvent(context.getChannel(),
										evt.getFuture(), channelBuffer, evt.getChannel().getRemoteAddress());
								context.sendDownstream(messageEvent);
							}
							// CustomBuffer buf = new CustomBuffer(32768);
							// buf.getBuffer()[14] = 10;
							//
							// CustomPacket packet = new CustomPacket(buf);
							// packet.reset();
							// buf.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
							// buf.putInt(0);
							// buf.putInt(1);
							// buf.skip(1);

							// run(buffer, context, evt);

							// ChannelBuffer channelBuffer =
							// ChannelBuffers.copiedBuffer(buf.getBuffer());

							// getSession().write(packet, this, 1, context,
							// evt);
							// DownstreamMessageEvent messageEvent = new
							// DownstreamMessageEvent(context.getChannel(),
							// evt.getFuture(), channelBuffer,
							// evt.getChannel().getRemoteAddress());
							// context.sendDownstream(messageEvent);
						}
						return;
					} catch (com.fasterxml.jackson.core.JsonParseException ex) {
						logger.warn("ignore parse failed", ex);
						// return;	
					}
				}
			} else {
				logger.debug("not channelbuffer: {}", e.getMessage().getClass());
				context.sendDownstream(evt);
			}

		} else {
			context.sendDownstream(evt);
		}
	}

	public boolean isPass() {
		return pass;
	}

	public void setPass(boolean pass) {
		this.pass = pass;
	}
}
