package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.LoggerFactory;

public class CustomChannelShell extends CustomChannelSession {

	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(CustomChannelShell.class);


	public CustomChannelShell() {
		super();
		pty = true;
	}

	@Override
	public void start(ChannelHandlerContext context, ChannelEvent evt) throws JSchException {
		context.getPipeline().addAfter("interactiveHandler", "customChannelShell", this);
		logger.debug("pipeline: {}", context.getPipeline());
		try {
			logger.debug("send pty request");
			sendRequests(context, evt);
		} catch (Exception e) {
			if (e instanceof JSchException)
				throw (JSchException) e;
			if (e instanceof Throwable)
				throw new JSchException("ChannelShell", (Throwable) e);
			throw new JSchException("ChannelShell");
		}
	}

	public void sendRequest(ChannelHandlerContext context, ChannelEvent evt) throws JSchException {
		CustomSession _session = getSession();

		logger.debug("pipeline: {}", context.getPipeline());
		try {
			CustomRequestShell request = new CustomRequestShell();
			request.request(_session, this, context, evt);
			this.setPass(false);
			logger.debug("send start shell request");
		} catch (Exception e) {
			if (e instanceof JSchException)
				throw (JSchException) e;
			if (e instanceof Throwable)
				throw new JSchException("ChannelShell", (Throwable) e);
			throw new JSchException("ChannelShell");
		}
	}

	public void init() {

	}

}
