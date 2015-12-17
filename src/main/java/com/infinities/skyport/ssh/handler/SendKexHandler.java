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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.consoleproxy.ssh.MyUserInfo;
import com.infinities.skyport.ssh.handler.exception.KeyExchangeException;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.UserInfo;

public class SendKexHandler implements ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(SendKexHandler.class);
	private Channel inboundChannel;
	private CustomSession session;


	public SendKexHandler(CustomSession session, Channel inboundChannel) {
		this.session = session;
		this.inboundChannel = inboundChannel;
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
		if (e.getMessage() instanceof ChannelBuffer) {
			ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
			session.setConnected(true);

			byte[] _buf = buffer.array();
			logger.debug("command received: {}", _buf[5]);
			String serverVersion = new String(_buf);
			logger.debug("SSH Server version: {}", serverVersion);
			if (serverVersion.length() > 3 && serverVersion.startsWith("SSH-")) {
				if (serverVersion.length() < 7 || serverVersion.charAt(4) == '1' && serverVersion.charAt(6) != '9') {
					throw new KeyExchangeException("invalid server's version string");
				}

				int i = 0;
				i = _buf.length;
				if (_buf[i - 1] == 10) {
					i--;
					if (i > 0 && _buf[i - 1] == 13) {
						i--;
					}
				}
				logger.trace("i : {}", i);
				byte[] V_S = new byte[i];
				System.arraycopy(_buf, 0, V_S, 0, i);
				Integer sessionId = evt.getChannel().getId();
				session.setV_S(V_S);
				UserInfo userinfo = new MyUserInfo(inboundChannel, sessionId);
				session.setUserinfo(userinfo);
				context.getPipeline().remove(this);
				session.sendKexinit(context, evt);
			} else {
				throw new KeyExchangeException("invalid server's version string");
			}
		} else {
			context.sendUpstream(evt);
		}
	}

}
