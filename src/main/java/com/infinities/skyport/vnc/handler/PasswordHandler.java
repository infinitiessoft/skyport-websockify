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
package com.infinities.skyport.vnc.handler;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.infinties.skyport.vnc.crypto.DesCipher;

public class PasswordHandler extends SimpleChannelUpstreamHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(PasswordHandler.class);
	private ByteBuffer challenge;
	private String token;
	private String password;


	public PasswordHandler(ByteBuffer challenge, String token, String password) {
		this.challenge = challenge;
		this.token = token;
		this.password = password;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		logger.debug("PasswordHandler pass {}, {}", new Object[] { e.getClass(), e.toString() });
		Object msg = e.getMessage();
		logger.debug("msg class:{}", e.getMessage().getClass());

		if (msg instanceof TextWebSocketFrame) {
			handleWebSocketFrame(ctx, (TextWebSocketFrame) msg, e);
		}
		// } else if (msg instanceof DefaultHttpRequest) {
		// handleHttpRequest(ctx, (DefaultHttpRequest) msg, e);
		// }

		super.messageReceived(ctx, e);
	}

	// private void handleHttpRequest(ChannelHandlerContext ctx,
	// DefaultHttpRequest msg, MessageEvent e)
	// throws URISyntaxException, UnsupportedEncodingException, AuthException {
	// URI uri = new URI(msg.getUri());
	// Map<String, List<String>> queries = splitQuery(uri.getQuery());
	// if (!Strings.isNullOrEmpty(token) && queries.containsKey("token") &&
	// token.equals(queries.get("token").get(0))) {
	// ctx.getPipeline().remove(this);
	// } else if (!Strings.isNullOrEmpty(token)
	// && (!queries.containsKey("token") ||
	// !token.equals(queries.get("token").get(0)))) {
	// throw new AuthException("invalid token");
	// }
	//
	// }

	public static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		if (!Strings.isNullOrEmpty(query)) {
			final String[] pairs = query.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder
						.decode(pair.substring(idx + 1), "UTF-8") : null;
				query_pairs.get(key).add(value);
			}
		}
		return query_pairs;
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame, final MessageEvent e) {
		ChannelBuffer msg = ChannelBuffers.copiedBuffer(frame.getBinaryData());
		ChannelBuffer decodedMsg = Base64.decode(msg);

		if (decodedMsg.array().length == 18) {
			logger.debug("challenge size:{} , {}", new Object[] { challenge.array().length, new String(challenge.array()) });
			byte[] trimByte = Arrays.copyOf(decodedMsg.array(), 16);
			byte[] decrypted = new DesCipher().decrypt(trimByte, token);
			if (Arrays.equals(challenge.array(), decrypted)) {
				byte[] encrypted = new DesCipher().encrypt(challenge.array(), password);
				decodedMsg = ChannelBuffers.copiedBuffer(encrypted);
				decodedMsg = Base64.encode(decodedMsg);
				logger.debug("token match, replace password :{}", new String(encrypted));
				frame.setBinaryData(decodedMsg);
				ctx.getPipeline().remove(this);
			}
		}

	}

}
