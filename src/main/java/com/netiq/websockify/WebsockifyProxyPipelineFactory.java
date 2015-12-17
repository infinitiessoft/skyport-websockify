package com.netiq.websockify;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.vnc.bootstrap.ConsoleBootstrap;
import com.infinities.skyport.websockify.SSLSetting;

public class WebsockifyProxyPipelineFactory implements ChannelPipelineFactory {

	private final ClientSocketChannelFactory cf;
	private final IProxyTargetResolver resolver;
	private final ConsoleBootstrap boostrap;
	private final SSLSetting sslSetting;
	private final ServerConfiguration configuration;
	private final org.apache.shiro.mgt.SecurityManager securityManager;


	public WebsockifyProxyPipelineFactory(ClientSocketChannelFactory cf, IProxyTargetResolver resolver,
			ConsoleBootstrap boostrap, SSLSetting sslSetting, ServerConfiguration configuration,
			org.apache.shiro.mgt.SecurityManager securityManager) {
		this.cf = cf;
		this.resolver = resolver;
		this.configuration = configuration;
		this.boostrap = boostrap;
		this.sslSetting = sslSetting;
		this.securityManager = securityManager;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline p = pipeline(); // Note the static import.

		p.addLast("unification", new PortUnificationHandler(cf, resolver, boostrap, sslSetting, configuration,
				securityManager));
		return p;

	}

}
