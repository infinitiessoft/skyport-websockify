package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

public class CustomRequestWindowChange extends CustomRequest {

	int width_columns = 80;
	int height_rows = 24;
	int width_pixels = 640;
	int height_pixels = 480;


	void setSize(int col, int row, int wp, int hp) {
		this.width_columns = col;
		this.height_rows = row;
		this.width_pixels = wp;
		this.height_pixels = hp;
	}

	@Override
	public void request(CustomSession session, CustomChannel channel, ChannelHandlerContext context, ChannelEvent evt)
			throws Exception {
		super.request(session, channel, context, evt);

		CustomBuffer buf = new CustomBuffer();
		CustomPacket packet = new CustomPacket(buf);

		// byte SSH_MSG_CHANNEL_REQUEST
		// uint32 recipient_channel
		// string "window-change"
		// boolean FALSE
		// uint32 terminal width, columns
		// uint32 terminal height, rows
		// uint32 terminal width, pixels
		// uint32 terminal height, pixels
		packet.reset();
		buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
		buf.putInt(channel.getRecipient());
		buf.putString(Util.str2byte("window-change"));
		buf.putByte((byte) (waitForReply() ? 1 : 0));
		buf.putInt(width_columns);
		buf.putInt(height_rows);
		buf.putInt(width_pixels);
		buf.putInt(height_pixels);
		write("CustomRequestWindowChange", packet, context, evt);
	}
}
