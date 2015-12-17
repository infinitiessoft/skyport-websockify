/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.ssh.handler;

import java.io.IOException;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.consoleproxy.ssh.Config;
import com.infinities.skyport.vnc.util.SSHUtil;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomChannel;
import com.jcraft.jsch.CustomChannelShell;
import com.jcraft.jsch.CustomKeyExchange;
import com.jcraft.jsch.CustomPacket;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.JSchException;

public class InteractiveHandler implements ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(InteractiveHandler.class);
	// private int serverAliveCountMax = 1;
	private boolean x11_forwarding = false;
	private boolean agent_forwarding = false;
	private CustomSession session;
	private CustomKeyExchange kex;


	// private CustomBuffer buf;
	// private CustomPacket packet;
	// private int j;
	// private int need;

	// private Channel inboundChannel;

	public InteractiveHandler(CustomSession session) {
		this.session = session;
		// buf = new CustomBuffer();
		// packet = new CustomPacket(buf);
		// this.inboundChannel = inboundChannel;
	}

	public void execute(ChannelHandlerContext context, org.jboss.netty.channel.Channel ch, MessageEvent evt, CustomBuffer buf)
			throws Exception {
		byte[] foo;
		int i = 0;
		int[] start = new int[1];
		int[] length = new int[1];
		CustomPacket packet = new CustomPacket(buf);
		// int stimeout = 0;
		int msgType = buf.getCommand() & 0xff;
		if (kex != null && kex.getState() == msgType) {
			session.setKex_start_time(System.currentTimeMillis());
			boolean result = kex.next(buf);
			if (!result) {
				throw new JSchException("verify: " + result);
			}
			return;
		}

		CustomChannelShell channel = session.getChannel();
		logger.debug("msgType: {} ", new Object[] { msgType });
		switch (msgType) {
		case Config.SSH_MSG_KEXINIT:
			// System.err.println("KEXINIT");
			kex = session.receiveKexinit(buf, context, evt);
			break;

		case Config.SSH_MSG_NEWKEYS:
			// System.err.println("NEWKEYS");
			session.send_newkeys(context, evt);
			session.receive_newkeys(kex);
			kex = null;
			break;

		case Config.SSH_MSG_CHANNEL_DATA:
			buf.getInt();
			buf.getByte();
			buf.getByte();
			i = buf.getInt();
			foo = buf.getString(start, length);
			if (channel == null) {
				break;
			}

			if (length[0] == 0) {
				break;
			}

			try {
				logger.debug("channel write data");
				channel.write(foo, start[0], length[0], context);
			} catch (Exception ex) {
				ex.printStackTrace();
				// System.err.println(e);
				try {
					channel.disconnect(context, evt);
				} catch (Exception ee) {
					ee.printStackTrace();
				}
				break;
			}
			int len = length[0];
			channel.setLocalWindowSize(channel.lwsize - len);
			if (channel.lwsize < channel.lwsize_max / 2) {
				packet.reset();
				buf.putByte((byte) Config.SSH_MSG_CHANNEL_WINDOW_ADJUST);
				buf.putInt(channel.getRecipient());
				buf.putInt(channel.lwsize_max - channel.lwsize);
				synchronized (channel) {
					if (!channel.close)
						session.write(packet, context, evt);
				}
				channel.setLocalWindowSize(channel.lwsize_max);
			}
			break;

		case Config.SSH_MSG_CHANNEL_EXTENDED_DATA:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			buf.getInt(); // data_type_code == 1
			foo = buf.getString(start, length);
			// System.err.println("stderr: "+new
			// String(foo,start[0],length[0]));
			if (channel == null) {
				break;
			}

			if (length[0] == 0) {
				break;
			}

			channel.write_ext(foo, start[0], length[0], context);

			len = length[0];
			channel.setLocalWindowSize(channel.lwsize - len);
			if (channel.lwsize < channel.lwsize_max / 2) {
				packet.reset();
				buf.putByte((byte) Config.SSH_MSG_CHANNEL_WINDOW_ADJUST);
				buf.putInt(channel.getRecipient());
				buf.putInt(channel.lwsize_max - channel.lwsize);
				synchronized (channel) {
					if (!channel.close)
						session.write(packet, context, evt);
				}
				channel.setLocalWindowSize(channel.lwsize_max);
			}
			break;

		case Config.SSH_MSG_CHANNEL_WINDOW_ADJUST:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel == null) {
				break;
			}
			channel.addRemoteWindowSize(buf.getUInt());
			break;

		case Config.SSH_MSG_CHANNEL_EOF:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel != null) {
				// channel.eof_remote=true;
				// channel.eof();
				channel.eof_remote();
			}
			/*
			 * packet.reset(); buf.putByte((byte)SSH_MSG_CHANNEL_EOF);
			 * buf.putInt(channel.getRecipient()); write(packet);
			 */
			break;
		case Config.SSH_MSG_CHANNEL_CLOSE:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel != null) {
				// channel.close();
				channel.disconnect(context, evt);
			}
			/*
			 * if(Channel.pool.size()==0){ thread=null; }
			 */
			break;
		case Config.SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			int r = buf.getInt();
			long rws = buf.getUInt();
			int rps = buf.getInt();
			if (channel != null) {
				logger.debug("set remote windows size: {}", String.valueOf(rws));
				channel.setRemoteWindowSize(rws);
				logger.debug("set remote packet size: {}", String.valueOf(rps));
				channel.setRemotePacketSize(rps);
				channel.setOpen_confirmation(true);
				logger.debug("set channel recipient: {}", String.valueOf(r));
				channel.setRecipient(r);
				channel.start(context, evt);
			}
			break;
		case Config.SSH_MSG_CHANNEL_OPEN_FAILURE:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel != null) {
				int reason_code = buf.getInt();
				// foo=buf.getString(); // additional textual
				// information
				// foo=buf.getString(); // language tag
				channel.setExitStatus(reason_code);
				channel.close = true;
				channel.eof_remote = true;
				channel.setRecipient(0);
			}
			break;
		case Config.SSH_MSG_CHANNEL_REQUEST:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			foo = buf.getString();
			boolean reply = (buf.getByte() != 0);
			if (channel != null) {
				byte reply_type = (byte) Config.SSH_MSG_CHANNEL_FAILURE;
				if ((SSHUtil.byte2str(foo)).equals("exit-status")) {
					i = buf.getInt(); // exit-status
					channel.setExitStatus(i);
					reply_type = (byte) Config.SSH_MSG_CHANNEL_SUCCESS;
				}
				if (reply) {
					packet.reset();
					buf.putByte(reply_type);
					buf.putInt(channel.getRecipient());
					session.write(packet, context, evt);
				}
			} else {
			}
			break;
		case Config.SSH_MSG_CHANNEL_OPEN:
			buf.getInt();
			buf.getShort();
			foo = buf.getString();
			String ctyp = SSHUtil.byte2str(foo);
			if (!"forwarded-tcpip".equals(ctyp) && !("x11".equals(ctyp) && x11_forwarding)
					&& !("auth-agent@openssh.com".equals(ctyp) && agent_forwarding)) {
				// System.err.println("Session.run: CHANNEL OPEN "+ctyp);
				// throw new
				// IOException("Session.run: CHANNEL OPEN "+ctyp);
				packet.reset();
				buf.putByte((byte) Config.SSH_MSG_CHANNEL_OPEN_FAILURE);
				buf.putInt(buf.getInt());
				buf.putInt(CustomChannel.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED);
				buf.putString(SSHUtil.empty);
				buf.putString(SSHUtil.empty);
				session.write(packet, context, evt);
			} else {
				channel = CustomChannel.getChannel(ctyp);
				channel.setSession(session);
				session.setChannel(channel);
				channel.getData(buf);
				channel.init();
				context.getPipeline().addAfter("interactiveHandler", "shellChannel", channel);
				channel.start(context, evt);
			}
			break;
		case Config.SSH_MSG_CHANNEL_SUCCESS:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel == null) {
				break;
			}
			if (channel.getReply() == 1) {
				// context.getPipeline().addBefore("handler",
				// "jsonHandler", new JsonHandler());
			} else {
				logger.debug("SSH_MSG_CHANNEL_SUCCESS set reply 1");
				channel.setReply(1);
				channel.sendRequest(context, evt);
			}
			break;
		case Config.SSH_MSG_CHANNEL_FAILURE:
			buf.getInt();
			buf.getShort();
			i = buf.getInt();
			if (channel == null) {
				break;
			}
			channel.setReply(0);
			break;
		case Config.SSH_MSG_GLOBAL_REQUEST:
			buf.getInt();
			buf.getShort();
			foo = buf.getString(); // request name
			reply = (buf.getByte() != 0);
			if (reply) {
				packet.reset();
				buf.putByte((byte) Config.SSH_MSG_REQUEST_FAILURE);
				session.write(packet, context, evt);
			}
			break;
		case Config.SSH_MSG_REQUEST_FAILURE:
		case Config.SSH_MSG_REQUEST_SUCCESS:
			Thread t = session.getGrr().getThread();
			if (t != null) {
				session.getGrr().setReply(msgType == Config.SSH_MSG_REQUEST_SUCCESS ? 1 : 0);
				if (msgType == Config.SSH_MSG_REQUEST_SUCCESS && session.getGrr().getPort() == 0) {
					buf.getInt();
					buf.getShort();
					session.getGrr().setPort(buf.getInt());
				}
				t.interrupt();
			}
			break;
		default:
			throw new IOException("Unknown SSH message type " + msgType);
		}

		if (channel != null && channel.isClosed()) {
			session.close(ch, "channel closed", context, evt.getMessage());
			// context.getChannel().close();
		}
	}

	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		if (!(evt instanceof MessageEvent)) {
			context.sendUpstream(evt);
			return;
		}
		MessageEvent e = (MessageEvent) evt;
		if (e.getMessage() instanceof CustomBuffer) {
			if (session.isConnected() && session.isAuthed()) {
				CustomBuffer buf = (CustomBuffer) e.getMessage();
				try {
					execute(context, context.getChannel(), e, buf);
				} catch (Exception ex) {
					session.close(context.getChannel(), ex.getMessage(), context, e.getMessage());
					throw ex;
					// logger.warn("Caught an exception, leaving main loop",
					// ex);
				}
			}
		}
	}
}
