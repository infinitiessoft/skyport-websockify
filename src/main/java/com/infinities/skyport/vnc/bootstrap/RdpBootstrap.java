package com.infinities.skyport.vnc.bootstrap;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
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

import com.google.common.base.Strings;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.rdp.custom.CustomVChannels;
import com.infinities.skyport.rdp.custom.ISOUtils;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.SecureUtils;
import com.infinities.skyport.rdp.handler.ISOReceiveHandler;
import com.infinities.skyport.rdp.handler.ISOReceiveMessageHandler;
import com.infinities.skyport.rdp.handler.MCSReceiveHandler;
import com.infinities.skyport.rdp.handler.MainLoopHandler;
import com.infinities.skyport.rdp.handler.RDPReceiveHandler;
import com.infinities.skyport.rdp.handler.SecureReceiveHandler;
import com.infinities.skyport.rdp.handler.SendCjrq2Handler;
import com.infinities.skyport.rdp.handler.SendCjrq3Handler;
import com.infinities.skyport.rdp.handler.SendCjrqHandler;
import com.infinities.skyport.rdp.handler.SendCredentialHandler;
import com.infinities.skyport.rdp.handler.SendEndrqHandler;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler.Type;
import com.lixia.rdp.Options;
import com.lixia.rdp.RdpPacket_Localised;

public class RdpBootstrap extends AbstractBootstrap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(RdpBootstrap.class);
	private static final int CONNECTION_REQUEST = 0xE0;
	private static final int PROTOCOL_VERSION = 0x03;
	private Channel inboundChannel;
	private WebsockifyProfile profile;
	private InetSocketAddress remoteAddress;
	private RDPSession session;
	private Options options;
	private Channel toRDPChanel = null;


	public RdpBootstrap(WebsockifyProfile profile, ChannelFactory channelFactory) {
		super(channelFactory);
		this.profile = profile;
		remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
		initClient(profile);
		setOption("tcpNoDelay", true);
	}

	private void initClient(final WebsockifyProfile profile) {
		try {
			String host = profile.getTargetHost();
			final String instanceId = profile.getTargetHost();
			final int port = profile.getTargetPort();
			options = new Options();

			// options.setWidth(800);
			// options.setHeight(600);
			session = new RDPSession(options);
			CustomVChannels channels = new CustomVChannels(options);
			logger.info("connecting to instance " + instanceId + " on host " + host);
			logger.info("Connecting to " + host + ":" + port + " ...");
			session.setChannels(channels);
		} catch (Exception e) {
			logger.info("error occurred in initializing rdp client", e);
			throw e;
		}
	}

	@Override
	public ChannelFuture connect() {
		String host = profile.getTargetHost();
		final String password = profile.getPassword();
		final int port = profile.getTargetPort();
		final String username = profile.getUsername();
		String name = null;
		String domain = null;
		if (username.contains("\\")) {
			String[] tokens = username.split("\\\\");
			name = tokens[1];
			domain = tokens[0];
		} else {
			name = username;
			domain = "Workgroup";
		}

		options.setUsername(name);
		options.setDomain(domain);
		options.setPort(port);
		options.setHostname(host);
		if (!Strings.isNullOrEmpty(password)) {
			options.setAutologin(true);
		}
		options.setPassword(password);

		SocketAddress remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
		logger.debug("inbound channel id : {}", inboundChannel.getId());
		inboundChannel.getPipeline().remove("passwordhandler");
		getPipeline().remove("challengehandler");

		RdpPacket_Localised mcs_data = SecureUtils.sendMcsData(session, profile.getHost());
		getPipeline().addFirst("mainLoopHandler", new MainLoopHandler(session));
		getPipeline().addFirst("rdpReceiveHandler", new RDPReceiveHandler(session));
		getPipeline().addFirst("secureReceiveHandler", new SecureReceiveHandler(session));
		getPipeline().addFirst("mcsReceiveHandler", new MCSReceiveHandler());
		getPipeline().addFirst("isoReceiveHandler", new ISOReceiveHandler());
		getPipeline().addFirst("isoReceiveMessageHandler", new ISOReceiveMessageHandler(session));
		getPipeline().addFirst("sendCjrq3Handler", new SendCjrq3Handler(session, mcs_data));
		getPipeline().addFirst("sendCjrq2Handler", new SendCjrq2Handler(session));
		getPipeline().addFirst("sendCjrqHandler", new SendCjrqHandler(session));
		getPipeline().addFirst("sendEndrqHandler", new SendEndrqHandler(mcs_data, session));
		getPipeline().addFirst("sendCredentialHandler", new SendCredentialHandler(mcs_data, session));

		ChannelFuture future = super.connect(remoteAddress);
		toRDPChanel = future.getChannel();
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
					/**
					 * ISO.send_connection_request Send the server a connection
					 * request, detailing client protocol version
					 */
					String uname = profile.getHost();
					if (uname.length() > 9)
						uname = uname.substring(0, 9);
					int length = 11 + (uname.length() > 0 ? ("Cookie: mstshash=".length() + uname.length() + 2) : 0) + 8;
					RdpPacket_Localised buffer = new RdpPacket_Localised(length);
					byte[] packet = new byte[length];

					buffer.set8(PROTOCOL_VERSION); // send Version Info
					buffer.set8(0); // reserved byte
					buffer.setBigEndian16(length); // Length
					buffer.set8(length - 5); // Length of Header
					buffer.set8(CONNECTION_REQUEST);
					buffer.setBigEndian16(0); // Destination reference ( 0 at CC
												// and DR)
					buffer.setBigEndian16(0); // source reference should be a
												// reasonable address we use 0
					buffer.set8(0); // service class
					if (options.getUsername().length() > 0) {
						logger.debug("Including username");
						buffer.out_uint8p("Cookie: mstshash=", "Cookie: mstshash=".length());
						buffer.out_uint8p(uname, uname.length());

						buffer.set8(0x0d); // unknown
						buffer.set8(0x0a); // unknown
					}

					/*
					 * // Authentication request?
					 * buffer.setLittleEndian16(0x01);
					 * buffer.setLittleEndian16(0x08); // Do we try to use SSL?
					 * buffer.set8(Options.use_ssl? 0x01 : 0x00);
					 * buffer.incrementPosition(3);
					 */
					buffer.copyToByteArray(packet, 0, 0, packet.length);
					ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(packet);
					future.getChannel().write(channelBuffer);
					inboundChannel.setReadable(true);

					// upstream
					// inboundChannel.

					// downstream
				} else {
					logger.error("Failed to create outbound connection to {}", target);
					logger.error("Failed cause",future.getCause());
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
		int width = 800, height = 600;
		try {
			width = Integer.parseInt(queryParams.get("width").get(0));
		} catch (Exception e) {
			width = 800;
		}
		try {
			height = Integer.parseInt(queryParams.get("height").get(0));
		} catch (Exception e) {
			height = 600;
		}

		options.setWidth(width);
		options.setHeight(height);
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

	@Override
	public Type getType() {
		return Type.Binary;
	}

	@Override
	public void close() throws IOException {
		try {
			if (toRDPChanel != null) {
				logger.debug("pipeline:{}", toRDPChanel.getPipeline());
				if (toRDPChanel.getPipeline().get("inputLocalisedHandler") != null) {
					toRDPChanel.getPipeline().remove("inputLocalisedHandler");
				}
				ISOUtils.sendMessage(ISOUtils.DISCONNECT_REQUEST, toRDPChanel);
			}
		} catch (Exception e) {

		}
		this.server.close(serverChannel);
	}

}
