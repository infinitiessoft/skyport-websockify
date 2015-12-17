package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;

public abstract class CustomUserAuth implements ChannelUpstreamHandler, ChannelDownstreamHandler {

	public static final int OK = 0;
	public static final int FAIL = 1;
	public static final int CONTINUE = 2;

	public static final int SSH_MSG_USERAUTH_REQUEST = 50;
	public static final int SSH_MSG_USERAUTH_FAILURE = 51;
	public static final int SSH_MSG_USERAUTH_SUCCESS = 52;
	public static final int SSH_MSG_USERAUTH_BANNER = 53;
	public static final int SSH_MSG_USERAUTH_INFO_REQUEST = 60;
	public static final int SSH_MSG_USERAUTH_INFO_RESPONSE = 61;
	public static final int SSH_MSG_USERAUTH_PK_OK = 60;


	public abstract void start(CustomSession session, ChannelHandlerContext context, ChannelEvent evt) throws Exception;

}
