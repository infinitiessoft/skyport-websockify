package com.infinities.skyport.vnc.bootstrap;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.websockify.WebsockifyServer;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler;

public interface ConsoleBootstrap extends Serializable {

	ChannelFuture connect(Channel inboundChannel, Object trafficLock, Map<String, List<String>> queryParams);

	ChannelPipeline getPipeline();

	InetSocketAddress getTarget();

	WebsockifyProfile getProfile();

	void setWebsockifyServer(WebsockifyServer server);

	void setServerChannel(Channel serverChannel);

	void close() throws Exception;

	OutboundWebsocketHandler.Type getType();

}
