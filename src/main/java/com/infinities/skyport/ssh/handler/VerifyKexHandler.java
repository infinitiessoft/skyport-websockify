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

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.consoleproxy.ssh.Config;
import com.infinities.skyport.ssh.handler.exception.KeyExchangeException;
import com.infinities.skyport.vnc.util.SSHUtil;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomKeyExchange;
import com.jcraft.jsch.CustomPacket;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.JSchException;

public class VerifyKexHandler implements ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(VerifyKexHandler.class);
	private CustomSession session;
	private int state = 0;


	public VerifyKexHandler(CustomSession session) {
		this.session = session;
	}

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
			CustomBuffer buf = (CustomBuffer) e.getMessage();
			switch (state) {
			case 0:
				logger.debug("command received: {}", buf.getCommand());
				logger.debug("Key State: {}", session.getKex().getState());
				CustomKeyExchange kex = session.getKex();
				if (kex.getState() == buf.getCommand()) {
					session.setKex_start_time(System.currentTimeMillis());
					boolean result = session.getKex().next(buf);
					if (!result) {
						session.setIn_kex(false);
						throw new KeyExchangeException("verify: " + result);
					}
				} else {
					session.setIn_kex(false);
					throw new KeyExchangeException("invalid protocol(kex): " + buf.getCommand());
				}

				logger.debug("kex state: {}", String.valueOf(session.getKex().getState()));
				if (kex.getState() != CustomKeyExchange.STATE_END) {
					logger.debug("kex != end; again");
					return;
				}
				state = 1;
				break;
			case 1:
				try {
					logger.debug("remove VerifyKexHandler");
					context.getPipeline().remove(this);
					session.send_newkeys(context, evt);
					logger.debug("receive SSH_MSG_NEWKEYS");
					if (buf.getCommand() == Config.SSH_MSG_NEWKEYS) {
						logger.debug("SSH_MSG_NEWKEYS received");
						session.receive_newkeys(session.getKex());
					} else {
						session.setIn_kex(false);
						throw new JSchException("invalid protocol(newkyes): " + buf.getCommand());
					}
					sendAuthRequest(context, evt);
				} catch (Exception ex) {
					session.close(context.getChannel(), ex.getMessage(), context, e.getMessage());
					throw ex;
				}
			}
		} else {
			context.sendUpstream(evt);
		}
	}

	// UserAuthNone
	private void sendAuthRequest(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);
		packet.reset();
		buf.putByte((byte) Config.SSH_MSG_SERVICE_REQUEST);
		buf.putString(SSHUtil.str2byte("ssh-userauth"));
		session.write(packet, context, evt);
		logger.info("SSH_MSG_SERVICE_REQUEST sent");
	}

}
