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
package com.infinities.skyport.vnc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.infinities.skyport.vnc.model.SessionOutput;
import com.infinities.skyport.vnc.model.UserSessionsOutput;

public class SessionOutputUtil {

	private static Map<Long, UserSessionsOutput> userSessionsOutputMap = new ConcurrentHashMap<Long, UserSessionsOutput>();


	public static void addOutput(Long sessionId, SessionOutput sessionOutput) {

		UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
		if (userSessionsOutput == null) {
			userSessionsOutputMap.put(sessionId, new UserSessionsOutput(sessionId, new StringBuilder()));
			userSessionsOutput = userSessionsOutputMap.get(sessionId);
		}

	}

	public static void addToOutput(Long sessionId, char value[], int offset, int count) {

		UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
		if (userSessionsOutput != null) {
			userSessionsOutput.getOutput().append(value, offset, count);
		}
	}

	public static void removeOutput(Long sessionId) {

		UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
		if (userSessionsOutput != null) {
			userSessionsOutputMap.remove(sessionId);
		}
	}

	public static List<SessionOutput> getOutput(Long sessionId) {
		List<SessionOutput> outputList = new ArrayList<SessionOutput>();

		UserSessionsOutput userSessionsOutput = userSessionsOutputMap.get(sessionId);
		if (userSessionsOutput != null) {

			try {
				StringBuilder sb = userSessionsOutput.getOutput();
				if (sb != null) {
					SessionOutput sessionOutput = new SessionOutput();
					sessionOutput.setSessionId(sessionId);

					sessionOutput.setOutput(sb.toString());

					if (StringUtils.isNotEmpty(sessionOutput.getOutput())) {
						outputList.add(sessionOutput);

						userSessionsOutputMap.put(sessionId, new UserSessionsOutput(sessionId, new StringBuilder()));
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return outputList;
	}

}
