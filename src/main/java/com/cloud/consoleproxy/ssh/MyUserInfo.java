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
package com.cloud.consoleproxy.ssh;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.infinities.skyport.util.JsonUtil;
import com.infinities.skyport.vnc.model.SessionOutput;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class MyUserInfo implements UserInfo, UIKeyboardInteractive {

	private final static Logger logger = LoggerFactory.getLogger(MyUserInfo.class);
	private Channel inboundChannel;
	private String password;
	private long sessionId;


	public MyUserInfo(Channel inboundChannel, long sessionId) {
		this.inboundChannel = inboundChannel;
		this.sessionId = sessionId;
	}

	@Override
	public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
			boolean[] echo) {
		logger.debug("prompt keyboard interactive destination: {}, name:{}, instruction:{}", new Object[] { destination,
				name, instruction });
		return null;
	}

	@Override
	public String getPassphrase() {
		logger.debug("get passphase");
		return null;
	}

	@Override
	public String getPassword() {
		logger.debug("get password");
		return password;
	}

	@Override
	public boolean promptPassword(String message) {
		logger.debug("prompt password: {}", message);
		return true;
	}

	@Override
	public boolean promptPassphrase(String message) {
		logger.debug("prompt passphrase: {}", message);
		return false;
	}

	@Override
	public boolean promptYesNo(String message) {
		logger.debug("prompt yes and no: {}", message);
		try {
			writeOutput(message);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return false;
	}

	@Override
	public void showMessage(String message) {
		logger.debug("show message: {}", message);
	}

	private void writeOutput(String message) throws JsonProcessingException {
		if (inboundChannel != null) {
			SessionOutput sessionOutput = new SessionOutput();
			sessionOutput.setSessionId(sessionId);
			sessionOutput.setOutput(message);
			List<SessionOutput> outputList = new ArrayList<SessionOutput>();
			outputList.add(sessionOutput);

			String json = JsonUtil.getObjectMapper().writeValueAsString(outputList);
			TextWebSocketFrame frame = new TextWebSocketFrame(json);
			inboundChannel.write(frame);
		}
	}

}
