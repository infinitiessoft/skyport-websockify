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
