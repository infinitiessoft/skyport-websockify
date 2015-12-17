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
package com.infinities.skyport.vnc.bootstrap;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;
import com.infinities.skyport.model.console.ConsoleType;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.ssh.handler.BufferHandler;
import com.infinities.skyport.ssh.handler.JsonHandler;
import com.infinities.skyport.ssh.handler.MsgServiceHandler;
import com.infinities.skyport.ssh.handler.ReceiveKexHandler;
import com.infinities.skyport.ssh.handler.SendKexHandler;
import com.infinities.skyport.ssh.handler.VerifyKexHandler;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler.Type;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.JSchException;

public class SshBootstrap extends AbstractBootstrap {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(SshBootstrap.class);
	private Channel inboundChannel;
	private WebsockifyProfile profile;
	private InetSocketAddress remoteAddress;
	// private SshHandler sshHandler;
	private static final String version = "JSCH-0.1.52";
	private byte[] V_C = str2byte("SSH-2.0-" + version); // client version


	public SshBootstrap(WebsockifyProfile profile, ChannelFactory channelFactory) {
		super(channelFactory);
		this.profile = profile;
		remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
	}

	@Override
	public ChannelFuture connect() {
		SocketAddress remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
		logger.debug("inbound channel id : {}", inboundChannel.getId());
		logger.debug("inbound channel address : {}", inboundChannel.getLocalAddress());
		inboundChannel.getPipeline().remove("passwordhandler");
		getPipeline().remove("challengehandler");
		CustomSession session = null;
		try {
			session = new CustomSession(profile.getUsername(), profile.getTargetHost(), profile.getTargetPort(),
					profile.getPassword());
			if (ConsoleType.VPSSH.equals(profile.getConsoleType())) {
				session.addIdentity("remote", BaseEncoding.base64().decode(profile.getKey()), null, profile.getPassword());
			}
		} catch (JSchException e) {
			throw new BootstrapInitialException(e);
		}

		getPipeline().addFirst("jsonHandler", new JsonHandler());
		// getPipeline().addFirst("interactiveHandler", new
		// InteractiveHandler(session));
		getPipeline().addFirst("msgServiceHandler", new MsgServiceHandler(session));
		getPipeline().addFirst("verifyKexHandler", new VerifyKexHandler(session));
		getPipeline().addFirst("receiveKexHandler", new ReceiveKexHandler(session));
		getPipeline().addFirst("bufferHandler", new BufferHandler(session));
		getPipeline().addFirst("sendKexHandler", new SendKexHandler(session, inboundChannel));

		ChannelFuture future = super.connect(remoteAddress);
		future.addListener(createChannelFutureListener(remoteAddress, future.getChannel()));

		return future;
	}

	private ChannelFutureListener createChannelFutureListener(final SocketAddress target, final Channel outboundChannel) {
		checkNotNull(inboundChannel);

		return new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("connection to {} succeeded", target);
					byte[] foo = new byte[V_C.length + 1];
					System.arraycopy(V_C, 0, foo, 0, V_C.length);
					foo[foo.length - 1] = (byte) '\n';
					ChannelBuffer buffer = ChannelBuffers.copiedBuffer(foo);
					future.getChannel().write(buffer);

					inboundChannel.setReadable(true);
				} else {
					logger.error("Failed to create outbound connection to {}", target);
					// Close the connection if the connection attempt has
					// failed.
					inboundChannel.close();
				}

			}
		};
	}

	@Override
	public ChannelFuture connect(Channel inboundChannel, Object trafficLock, Map<String, List<String>> queryParams) {
		this.inboundChannel = inboundChannel;
		return connect();
	}

	@Override
	public InetSocketAddress getTarget() {
		return remoteAddress;
	}

	@Override
	public WebsockifyProfile getProfile() {
		return profile;
	}

	public static byte[] str2byte(String str) {
		return str2byte(str, "UTF-8");
	}

	private static byte[] str2byte(String str, String encoding) {
		if (str == null)
			return null;
		try {
			return str.getBytes(encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	@Override
	public Type getType() {
		return Type.Base64Text;
	}

}
