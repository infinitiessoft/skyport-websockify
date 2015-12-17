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
