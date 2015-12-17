package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

public class CustomRequestShell extends CustomRequest {

	public void request(CustomSession session, CustomChannelShell channel, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		super.request(session, channel, context, evt);

		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);

		// send
		// byte SSH_MSG_CHANNEL_REQUEST(98)
		// uint32 recipient channel
		// string request type // "shell"
		// boolean want reply // 0
		packet.reset();
		buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
		buf.putInt(channel.getRecipient());
		buf.putString(Util.str2byte("shell"));
		// this.setReply(false);
		buf.putByte((byte) (waitForReply() ? 1 : 0));
		write("CustomRequestShell", packet, context, evt);
	}
}
