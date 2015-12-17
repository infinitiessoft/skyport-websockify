package com.infinities.skyport.vnc.bootstrap;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler.Type;

public class VncBootstrap extends AbstractBootstrap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(VncBootstrap.class);
	private Channel inboundChannel;
	private WebsockifyProfile profile;
	private InetSocketAddress remoteAddress;


	public VncBootstrap(WebsockifyProfile profile, ChannelFactory channelFactory) {
		super(channelFactory);
		this.profile = profile;
		remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
	}

	@Override
	public ChannelFuture connect() {
		SocketAddress remoteAddress = new InetSocketAddress(profile.getTargetHost(), profile.getTargetPort());
		ChannelFuture future = super.connect(remoteAddress);
		future.addListener(createChannelFutureListener(remoteAddress));

		return future;
	}

	private ChannelFutureListener createChannelFutureListener(final SocketAddress target) {
		checkNotNull(inboundChannel);

		return new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					// Connection attempt succeeded:
					// Begin to accept incoming traffic.
					logger.info("Created outbound connection to {}", target);
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

	public Channel getInboundChannel() {
		return inboundChannel;
	}

	public void setInboundChannel(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
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

	@Override
	public Type getType() {
		return Type.Base64Text;
	}
}
