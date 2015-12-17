package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

public class CustomRequestSignal extends CustomRequest {

	private String signal = "KILL";


	public void setSignal(String foo) {
		signal = foo;
	}

	public void request(CustomSession session, CustomChannelShell channel, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		super.request(session, channel, context, evt);

		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);

		packet.reset();
		buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
		buf.putInt(channel.getRecipient());
		buf.putString(Util.str2byte("signal"));
		buf.putByte((byte) (waitForReply() ? 1 : 0));
		buf.putString(Util.str2byte(signal));
		write("CustomRequestSignal", packet, context, evt);
	}
}
