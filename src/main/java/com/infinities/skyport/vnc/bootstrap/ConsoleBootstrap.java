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
