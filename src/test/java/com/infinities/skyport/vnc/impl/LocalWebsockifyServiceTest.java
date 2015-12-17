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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.netty.channel.Channel;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.infinities.skyport.model.console.ConsoleType;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.model.console.ServerConfiguration.Mode;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.websockify.SSLSetting;
import com.infinities.skyport.websockify.WebsockifyServer;

public class LocalWebsockifyServiceTest {

	protected Mockery context = new JUnit4Mockery() {

		{
			setThreadingPolicy(new Synchroniser());
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private LocalWebsockifyService service;
	private WebsockifyServer server;
	private ServerConfiguration config;
	private Channel channel;


	@Before
	public void setUp() throws Exception {
		server = context.mock(WebsockifyServer.class);
		channel = context.mock(Channel.class);
		config = new ServerConfiguration();
		config.setIp("127.0.0.1");
		config.setEnableSSL(true);
		config.setMaxPort(11100);
		config.setMinPort(11000);
		config.setMode(Mode.local);
		config.setPath("vnc.html");
		config.setPort("1234");
		config.setKeystoreFile("config/skyport_keystore.p12");
		config.setKeystorePass("changeit");
		config.setKeystoreType("PKCS12");
		service = new LocalWebsockifyService(config);
		service.initialize();
		service.setWebsockifyServer(server);
	}

	@After
	public void tearDown() throws Exception {
		context.checking(new Expectations() {

			{
				oneOf(server).close(channel);
			}
		});
		service.close();
	}

	@Test
	public void testSockify() {
		final WebsockifyProfile profile = new WebsockifyProfile();
		profile.setConsoleType(ConsoleType.VNC);
		profile.setInternalSSL(false);
		profile.setPassword("password");
		profile.setToken("token");
		profile.setTargetHost("0.0.0.0");
		profile.setTargetPort(1);
		context.checking(new Expectations() {

			{
				oneOf(server).connect(profile, SSLSetting.REQUIRED, config, null);
				will(getAction());

				oneOf(server).close(null);
			}
		});

		WebsockifyProfile ret = service.sockify(profile);
		assertEquals(ConsoleType.VNC, ret.getConsoleType());
		assertFalse(ret.isInternalSSL());
		assertEquals("password", ret.getPassword());
		assertEquals("token", ret.getToken());
		assertEquals("0.0.0.0", ret.getTargetHost());
		assertEquals(1, ret.getTargetPort());
		assertEquals("127.0.0.1", ret.getHost());
		assertEquals(config.getPath(), ret.getPath());
		assertEquals(11000, ret.getSourcePort());
		assertEquals(config.getPort(), String.valueOf(ret.getPort()));
		assertTrue(ret.isRequireSSL());

	}

	private CustomAction getAction() {
		return new CustomAction("add disk async") {

			@Override
			public Object invoke(Invocation invocation) throws Throwable {
				WebsockifyProfile profile = (WebsockifyProfile) invocation.getParameter(0);
				ServerConfiguration configuration = (ServerConfiguration) invocation.getParameter(2);
				assertEquals(ConsoleType.VNC, profile.getConsoleType());
				assertFalse(profile.isInternalSSL());
				assertEquals("password", profile.getPassword());
				assertEquals("token", profile.getToken());
				assertEquals("0.0.0.0", profile.getTargetHost());
				assertEquals(1, profile.getTargetPort());
				assertFalse(profile.isRequireSSL());
				assertEquals(config, configuration);
				return channel;
			}
		};
	}

}
