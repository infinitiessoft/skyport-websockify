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

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.vnc.impl.BasicHttpAuthenticationFilter;

public class BasicHttpAuthenticationHandler extends SimpleChannelUpstreamHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(BasicHttpAuthenticationHandler.class);


	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		Object msg = e.getMessage();

		logger.debug("handling authentication");
		// An HttpRequest means either an initial websocket connection
		// or a web server request
		if (msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) e.getMessage();

			List<Entry<String, String>> entrys = req.headers().entries();
			String str = "";

			for (Entry<String, String> entry : entrys) {
				str += entry.getKey() + "=" + entry.getValue() + " ";
			}

			logger.debug("request >>> {}, {}", new Object[] { str, req.getUri() });
			boolean isLogin = new BasicHttpAuthenticationFilter().executeLogin(req);
			if (!isLogin) {
				sendAuthResponse(ctx, UNAUTHORIZED);
				return;
			}
		}

		ctx.getPipeline().remove(this);
		// And let SecondHandler handle the current event.
		// ctx.getPipeline().addLast("2nd", new SecondHandler());
		super.messageReceived(ctx, e);
	}

	private void sendAuthResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
		response.headers().set("WWW-Authenticate", "Basic realm=\"Infinities Skyport\"");
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.headers().set(CONTENT_LENGTH, 0);
		response.setContent(ChannelBuffers.copiedBuffer(new byte[] {}));
		ctx.getChannel().write(response);

	}

}
