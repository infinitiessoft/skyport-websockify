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
package com.cloud.consoleproxy.ssh;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.infinities.skyport.ssh.handler.InteractiveHandler;
import com.infinities.skyport.ssh.handler.exception.AuthException;
import com.infinities.skyport.util.JsonUtil;
import com.infinities.skyport.vnc.util.SSHUtil;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomChannelShell;
import com.jcraft.jsch.CustomPacket;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.CustomUserAuth;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.UserInfo;

public class UserAuthPublicKey extends CustomUserAuth {

	private static final Logger logger = LoggerFactory.getLogger(UserAuthPublicKey.class);
	protected UserInfo userinfo;
	protected String username;
	private CustomSession session;
	private byte[] _username;
	private String _passphrase = "";
	private byte[] passphrase;


	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.trace("handle up stream : {}, {}, {}", new Object[] { evt.getClass(), evt.toString(),
				context.getPipeline().getNames() });

		if (!(evt instanceof MessageEvent)) {
			context.sendUpstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (e.getMessage() instanceof CustomBuffer) {
			if (session.getAuth_failures() >= session.getMax_auth_tries()) {
				throw new AuthException("Login trials exceeds " + session.getMax_auth_tries());
			}

			CustomBuffer buf = (CustomBuffer) e.getMessage();
			int command = buf.getCommand() & 0xff;
			try {
				logger.debug("password auth get command: {}", command);
				if (command == SSH_MSG_USERAUTH_SUCCESS) {
					logger.debug("auth success");
					// context.getPipeline().addFirst("userAuthPasswordHandler",
					// this);
					context.getPipeline().addAfter("bufferHandler", "interactiveHandler", new InteractiveHandler(session));
					context.getPipeline().remove(this);
					// session.requestPortForwarding();
					session.setAuthed(true);
					logger.debug("open shell channel");
					CustomChannelShell channel = session.openChannel();
					addChannel(channel);
					channel.init();
					channel.connect(5 * 1000, context, evt);
					return;
				} else if (command == SSH_MSG_USERAUTH_PK_OK) {
					Identity identity = session.getIdentity();
					if ((identity.isEncrypted() && passphrase == null)) {
						if (userinfo == null) {
							throw new AuthException("USERAUTH fail");
						}
						promptPassphrase(context, evt);
						return;
					} else {
						sendKey(context, evt, identity);
						return;
					}
				} else if (command == SSH_MSG_USERAUTH_BANNER) {
					buf.getInt();
					buf.getByte();
					buf.getByte();
					byte[] _message = buf.getString();
					// byte[] lang=
					buf.getString();
					String message = SSHUtil.byte2str(_message);
					if (userinfo != null) {
						userinfo.showMessage(message);
					}
					return;
				} else if (command == SSH_MSG_USERAUTH_FAILURE) {
					buf.getInt();
					buf.getByte();
					buf.getByte();
					byte[] foo = buf.getString();
					int partial_success = buf.getByte();
					if (partial_success != 0) {
						throw new AuthException(SSHUtil.byte2str(foo));
					}

					session.setAuth_failures(session.getAuth_failures() + 1);

					if (passphrase == null) {
						if (userinfo == null) {
							throw new AuthException("USERAUTH fail");
						}

						promptPassphrase(context, evt);
						return;
					}
					if (passphrase != null) {
						SSHUtil.bzero(passphrase);
						passphrase = null;
						_passphrase = "";
					}
					return;
				} else {
					throw new AuthException("USERAUTH fail (" + command + ")");
				}

				// if (password != null) {
				// SSHUtil.bzero(password);
				// password = null;
				// }
			} catch (Exception ex) {
				session.close(context.getChannel(), ex.getMessage(), context, e.getMessage());
				throw ex;
			}
		} else {
			context.sendUpstream(evt);
		}
	}

	private void promptPassphrase(ChannelHandlerContext context, ChannelEvent evt) {
		Identity identity = session.getIdentity();
		String prompt = "Passphrase for " + identity.getName() + ": \n";
		ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(prompt.getBytes());
		context.sendUpstream(new UpstreamMessageEvent(context.getChannel(), channelBuffer, context.getChannel()
				.getRemoteAddress()));
		logger.debug("prompt passphrase: {}", prompt);
		// userinfo.promptPassword("Password for " + dest);
		logger.debug("wait for user enter passphrase");
	}

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.debug("pass handle down stream : {}, {}, {}", new Object[] { evt.getClass(), evt.toString(),
				context.getPipeline().getNames() });

		if (!(evt instanceof MessageEvent)) {
			context.sendDownstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (e.getMessage() instanceof ChannelBuffer) {
			ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
			logger.debug("receive raw {}", new String(buffer.array()));
			// byte[] base64Decoded = BaseEncoding.base64().decode(new
			// String(buffer.array()));
			// Encode the message to base64
			String msg = new String(buffer.array());
			logger.debug("receive decoded {}", msg);
			try {
				if (Strings.isNullOrEmpty(msg.trim())) {
					return;
				}
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
						if (keyCode != 13) {
							_passphrase += new String(buf);
						} else {
							passphrase = SSHUtil.str2byte(_passphrase);
							Identity identity = session.getIdentity();
							identity.setPassphrase(passphrase);
							sendKey(context, evt, identity);
							if (passphrase != null) {
								SSHUtil.bzero(passphrase);
								passphrase = null;
								_passphrase = "";
							}
						}
					}
				} else {
					if (command != null) {
						_passphrase += command;
					}
				}
				return;
			} catch (Exception ex) {
				session.close(context.getChannel(), "auth failed", context, buffer);
				throw ex;
			}
		} else {
			logger.debug("not channelbuffer: {}", e.getMessage().getClass());
			context.sendDownstream(evt);
		}

	}

	private void sendKey(ChannelHandlerContext context, ChannelEvent evt, Identity identity) throws Exception {
		CustomBuffer _buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(_buf);
		packet.reset();
		_buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
		_buf.putString(_username);
		_buf.putString(SSHUtil.str2byte("ssh-connection"));
		_buf.putString(SSHUtil.str2byte("publickey"));
		_buf.putByte((byte) 1);
		_buf.putString(SSHUtil.str2byte(identity.getAlgName()));
		_buf.putString(identity.getPublicKeyBlob());

		byte[] sid = session.getSessionId();
		int sidlen = sid.length;
		byte[] tmp = new byte[4 + sidlen + _buf.getIndex() - 5];
		tmp[0] = (byte) (sidlen >>> 24);
		tmp[1] = (byte) (sidlen >>> 16);
		tmp[2] = (byte) (sidlen >>> 8);
		tmp[3] = (byte) (sidlen);
		System.arraycopy(sid, 0, tmp, 4, sidlen);
		System.arraycopy(_buf.getBuffer(), 5, tmp, 4 + sidlen, _buf.getIndex() - 5);
		byte[] signature = identity.getSignature(tmp);
		_buf.putString(signature);
		session.write(packet, context, evt);
	}

	@Override
	public void start(CustomSession session, ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		this.userinfo = session.getUserinfo();
		this.username = session.getUsername();
		this.session = session;
		Identity identity = session.getIdentity();
		// byte[] passphrase = null;

		// int command;

		_username = SSHUtil.str2byte(username);
		context.getPipeline().addAfter("bufferHandler", "userAuthPublicKeyHandler", this);
		logger.debug("pipeline: {}", context.getPipeline());
		if (session.getAuth_failures() >= session.getMax_auth_tries()) {
			throw new AuthException("Login trials exceeds " + session.getMax_auth_tries());
		}

		if (identity == null || identity.getPublicKeyBlob() == null) {
			throw new AuthException("USERAUTH fail");
		}

		// send
		// byte SSH_MSG_USERAUTH_REQUEST(50)
		// string user name
		// string service name ("ssh-connection")
		// string "password"
		// boolen FALSE
		// string plaintext password (ISO-10646 UTF-8)
		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);
		packet.reset();
		buf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
		buf.putString(_username);
		buf.putString(SSHUtil.str2byte("ssh-connection"));
		buf.putString(SSHUtil.str2byte("publickey"));
		buf.putByte((byte) 0);
		buf.putString(SSHUtil.str2byte(identity.getAlgName()));
		buf.putString(identity.getPublicKeyBlob());
		session.write(packet, context, evt);

	}

	private void addChannel(CustomChannelShell channel) {
		channel.setSession(session);
		session.setChannel(channel);
	}
}
