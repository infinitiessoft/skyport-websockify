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
package com.infinities.skyport.rdp.handler;

import java.io.EOFException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.Message;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.lixia.rdp.RdesktopException;

public class RDPReceiveHandler extends OneToOneDecoder {

	private static final Logger logger = LoggerFactory.getLogger(RDPReceiveHandler.class);
	private RDPSession session;


	public RDPReceiveHandler(RDPSession session) {
		this.session = session;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (msg instanceof Message) {
			return receive((Message) msg);
		} else {
			return msg;
		}
	}

	private Message receive(Message msg) throws RdesktopException, EOFException {
		int length = 0;
		logger.debug("receive data: {}, next_packet: {}", new Object[] { session.getStream(), session.getNext_packet() });
		if ((session.getStream() == null) || (session.getNext_packet() >= session.getStream().getEnd())) {
			session.setStream(msg.getPacket());
			// logger.debug("key:{}, value: {}, stream:{}",
			// new Object[] { entry.getKey(), entry.getValue(),
			// session.getStream() });
			if (session.getStream() == null)
				return null;
			session.setNext_packet(session.getStream().getPosition());
		} else {
			session.getStream().setPosition(session.getNext_packet());
		}
		length = session.getStream().getLittleEndian16();

		/* 32k packets are really 8, keepalive fix - rdesktop 1.2.0 */
		if (length == 0x8000) {
			logger.warn("32k packet keepalive fix");
			session.setNext_packet(session.getNext_packet() + 8);
			msg.setType(0);
			// type[0] = 0;
			msg.setPacket(session.getStream());
			return msg;
		}
		msg.setType(session.getStream().getLittleEndian16() & 0xf);
		if (session.getStream().getPosition() != session.getStream().getEnd()) {
			session.getStream().incrementPosition(2);
		}

		session.setNext_packet(session.getNext_packet() + length);
		msg.setPacket(session.getStream());
		return msg;
	}

}
