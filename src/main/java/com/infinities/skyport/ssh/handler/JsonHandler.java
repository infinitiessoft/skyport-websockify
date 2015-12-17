package com.infinities.skyport.ssh.handler;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.util.JsonUtil;
import com.infinities.skyport.vnc.model.SessionOutput;

public class JsonHandler extends FrameDecoder {

	private static final Logger logger = LoggerFactory.getLogger(JsonHandler.class);

	public JsonHandler() {

	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		Integer channelId = channel.getId();
		Long sessionId = new Long(channelId);
		SessionOutput sessionOutput = new SessionOutput();
		sessionOutput.setSessionId(sessionId);
		String message = new String(buffer.readBytes(buffer.readableBytes()).array());
		sessionOutput.setOutput(message);
		List<SessionOutput> outputList = new ArrayList<SessionOutput>();
		outputList.add(sessionOutput);

		String json = JsonUtil.getObjectMapper().writeValueAsString(outputList);
		logger.trace("send json:{}", json);
		ChannelBuffer jsonBuffer = ChannelBuffers.copiedBuffer(json.getBytes());
		return jsonBuffer;
	}

}
