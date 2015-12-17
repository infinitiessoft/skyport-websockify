package com.infinities.skyport.vnc.bootstrap;

import java.io.Serializable;

import org.jboss.netty.channel.ChannelFactory;

import com.infinities.skyport.model.console.WebsockifyProfile;

public class BootstrapBuilder implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	private BootstrapBuilder() {

	}

	public static ConsoleBootstrap createInstance(WebsockifyProfile profile, ChannelFactory channelFactory) {
		switch (profile.getConsoleType()) {
		case XVPVNC:
			return new XvpVncBootstrap(profile, channelFactory);
		case RDP:
			return new RdpBootstrap(profile, channelFactory);
//		case SPICE:
		case VNC:
			return new VncBootstrap(profile, channelFactory);
		case SSH:
		case VPSSH:
			return new SshBootstrap(profile, channelFactory);
		default:
			return new VncBootstrap(profile, channelFactory);
		}
	}
}
