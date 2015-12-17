package com.infinities.skyport.vnc.bootstrap;

import java.io.IOException;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;

import com.infinities.skyport.websockify.WebsockifyServer;

public abstract class AbstractBootstrap extends ClientBootstrap implements ConsoleBootstrap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected WebsockifyServer server;
	protected Channel serverChannel;


	public AbstractBootstrap(ChannelFactory channelFactory) {
		super(channelFactory);
	}

	@Override
	public void setWebsockifyServer(WebsockifyServer server) {
		this.server = server;
	}

	@Override
	public void setServerChannel(Channel serverChannel) {
		this.serverChannel = serverChannel;
	}

	@Override
	public void close() throws IOException {
		this.server.close(serverChannel);
	}

}
