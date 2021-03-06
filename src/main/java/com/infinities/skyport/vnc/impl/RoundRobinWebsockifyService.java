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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.service.WebsockifyService;
import com.infinities.skyport.websockify.WebsockifyServer;

public class RoundRobinWebsockifyService implements WebsockifyService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(RoundRobinWebsockifyService.class);
	private final List<WebsockifyService> services;
	private Iterator<WebsockifyService> iterator;


	public RoundRobinWebsockifyService(Set<ServerConfiguration> configurations) {
		services = new ArrayList<WebsockifyService>();
		for (ServerConfiguration configuration : configurations) {
			logger.debug("add configuration: {}", configuration);
			WebsockifyService service = WebsockifyServiceFactory.newServer(configuration);
			services.add(service);
		}
	}

	@Override
	public synchronized void close() throws IOException {
		for (WebsockifyService service : services) {
			service.close();
		}
		logger.debug("close RemoteWebsockifyService");
	}

	@Override
	public WebsockifyProfile sockify(WebsockifyProfile profile) throws Exception {
		WebsockifyService service;

		synchronized (iterator) {
			service = iterator.next();
		}

		return service.sockify(profile);
	}

	@Override
	public WebsockifyProfile release(WebsockifyProfile profile) throws Exception {
		for (WebsockifyService service : services) {
			service.release(profile);
		}

		return profile;
	}

	@Override
	public synchronized void initialize() throws Exception {
		for (WebsockifyService service : services) {
			service.initialize();
		}

		iterator = Iterators.cycle(services);
	}

	@Override
	public synchronized void setSecurityManager(Object securityManager) {
		for (WebsockifyService service : services) {
			service.setSecurityManager(securityManager);
		}
	}

	@Override
	public synchronized void setWebsockifyServer(WebsockifyServer websockifyServer) {
		for (WebsockifyService service : services) {
			service.setWebsockifyServer(websockifyServer);
		}
	}

}
