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
package com.infinities.skyport.vnc.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.console.ConsoleType;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.service.WebsockifyService;
import com.infinities.skyport.vnc.util.AvailablePortFinder;
import com.infinities.skyport.websockify.SSLSetting;
import com.infinities.skyport.websockify.WebsockifyServer;
import com.netiq.websockify.PortUnificationHandler;
import com.netiq.websockify.WebsockifySslContext;

public class LocalWebsockifyService implements WebsockifyService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(LocalWebsockifyService.class);
	private WebsockifyServer skyportWebsockifyServer;
	private Map<String, Channel> channelMap;
	// private static String SEPERATOR = ":";
	private final int directProxyTimeout = 5000;
	private org.apache.shiro.mgt.SecurityManager securityManager;
	private AvailablePortFinder portFinder;
	private SSLSetting sslSetting;
	private final ServerConfiguration configuration;


	public LocalWebsockifyService(ServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public synchronized WebsockifyProfile sockify(WebsockifyProfile profile) {
		checkNotNull(profile.getTargetHost());
		checkNotNull(profile.getTargetPort());
		checkNotNull(profile.getToken());

		PortUnificationHandler.setConnectionToFirstMessageTimeout(directProxyTimeout);
		int port = portFinder.getNextAvailable();
		profile.setSourcePort(port);

		logger.debug("sockify sourcePort:{}, targetHost:{}, targetPort:{}, host:{}, username:{}",
				new Object[] { profile.getSourcePort(), profile.getTargetHost(), profile.getTargetPort(), profile.getHost(),
						profile.getUsername() });

		logger.debug("profile:{}", profile);

		Channel channel = skyportWebsockifyServer.connect(profile, sslSetting, configuration, securityManager);

		Channel oldChannel = channelMap.put(profile.getToken(), channel);
		skyportWebsockifyServer.close(oldChannel);

		profile.setHost(configuration.getIp());
		profile.setPath(configPath(profile.getConsoleType()));
		profile.setPort(Integer.parseInt(configuration.getPort()));
		profile.setRequireSSL(configuration.isEnableSSL());

		return profile;

	}

	private String configPath(ConsoleType consoleType) {
		switch (consoleType) {
		case RDP:
			return configuration.getRdpPath();
		case SPICE:
		case VNC:
		case XVPVNC:
			return configuration.getPath();
		case SSH:
		case VPSSH:
			return configuration.getSshPath();
		default:
			throw new IllegalArgumentException("unimplemented console type:" + consoleType);

		}
	}

	public WebsockifyServer getSkyportWebsockifyServer() {
		return skyportWebsockifyServer;
	}

	@Override
	public void setWebsockifyServer(WebsockifyServer skyportWebsockifyServer) {
		this.skyportWebsockifyServer = skyportWebsockifyServer;
	}

	@Override
	public void close() {
		if (channelMap != null) {
			final Collection<Channel> set = channelMap.values();
			for (Channel channel : set) {
				skyportWebsockifyServer.close(channel);
			}
		}
	}

	@Override
	public WebsockifyProfile release(WebsockifyProfile profile) {
		checkNotNull(profile.getToken());

		logger.debug("release token:{}", new Object[] { profile.getToken() });

		Channel channel = channelMap.remove(profile.getToken());
		if (channel != null) {
			skyportWebsockifyServer.close(channel);
		}
		return profile;
	}

	// public org.apache.shiro.mgt.SecurityManager getSecurityManager() {
	// return securityManager;
	// }

	@Override
	public void setSecurityManager(Object securityManager) {
		this.securityManager = (SecurityManager) securityManager;
	}

	@Override
	public void initialize() {
		sslSetting = SSLSetting.OFF;
		boolean requireSSL = configuration.isRequireSSL();
		boolean enableSSL = configuration.isEnableSSL();
		String keystore = configuration.getKeystoreFile();
		String keystoreType = configuration.getKeystoreType();
		String keystorePassword = configuration.getKeystorePass();
		String keystoreKeyPassword = configuration.getKeystoreKeyPass();

		if (requireSSL)
			sslSetting = SSLSetting.REQUIRED;
		else if (enableSSL)
			sslSetting = SSLSetting.ON;

		if (sslSetting != SSLSetting.OFF) {
			if (keystore == null || keystore.isEmpty()) {
				throw new IllegalArgumentException("No keystore specified.");
			}

			if (keystorePassword == null || keystorePassword.isEmpty()) {
				throw new IllegalArgumentException("No keystore password specified.");
			}

			if (keystoreKeyPassword == null || keystoreKeyPassword.isEmpty()) {
				keystoreKeyPassword = keystorePassword;
			}

			try {
				WebsockifySslContext.validateKeystore(keystoreType, keystore, keystorePassword, keystoreKeyPassword);
			} catch (Exception e) {
				throw new IllegalStateException("Error validating keystore", e);
			}
		}

		if (sslSetting != SSLSetting.OFF)
			logger.debug("SSL is {}", (sslSetting == SSLSetting.REQUIRED ? "required." : "enabled."));

		channelMap = new ConcurrentHashMap<String, Channel>();
		SecurityUtils.setSecurityManager(securityManager);
		portFinder = new AvailablePortFinder(configuration.getMinPort(), configuration.getMaxPort());
	}

}
