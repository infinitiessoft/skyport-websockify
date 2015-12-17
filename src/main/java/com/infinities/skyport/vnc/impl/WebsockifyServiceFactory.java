package com.infinities.skyport.vnc.impl;

import java.io.Serializable;

import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.service.WebsockifyService;

public class WebsockifyServiceFactory implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private WebsockifyServiceFactory(){
		
	}

	public static WebsockifyService newServer(ServerConfiguration configuration) {
		switch (configuration.getMode()) {
		case local:
			return new LocalWebsockifyService(configuration);
		case remote:
			return new RemoteWebsockifyService(configuration);
		default:
			throw new IllegalArgumentException("No WebsockifyService with mode: " + configuration.getMode());
		}
	}
}
