package com.infinities.skyport.ssh.handler;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloud.consoleproxy.ssh.Config;
import com.infinities.skyport.ssh.handler.exception.KeyExchangeException;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.CustomUserAuth;

public class MsgServiceHandler implements ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(MsgServiceHandler.class);
	private CustomSession session;


	public MsgServiceHandler(CustomSession session) {
		this.session = session;
	}

	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.trace("handle up stream : {}, {}, {}", new Object[] { evt.getClass(), evt.toString(),
				context.getPipeline().getNames() });

		if (!(evt instanceof MessageEvent)) {
			context.sendUpstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (e.getMessage() instanceof CustomBuffer) {
			try {
				// ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
				CustomBuffer buf = (CustomBuffer) e.getMessage();
				// CustomPacket packet = new CustomPacket(buf);
				// packet.reset();
				// buf = session.read(buf, buffer);
				int command = buf.getCommand();
				logger.debug("command: {}", command);
				boolean result = (command == Config.SSH_MSG_SERVICE_ACCEPT);
				logger.info("SSH_MSG_SERVICE_ACCEPT received");
				logger.debug("user auth none result:{}", String.valueOf(result));
				if (result) {
					context.getPipeline().remove(this);
					try {
						String userAuth = session.getIdentity() != null ? Config.getConfig("userauth.publickey") : Config
								.getConfig("userauth.password");
						Class<?> c = Class.forName(userAuth);
						logger.debug("reflect UserAuth: {}", c.getName());
						CustomUserAuth ua = (CustomUserAuth) (c.newInstance());
						ua.start(session, context, evt);
					} catch (Exception ex) {
						throw new KeyExchangeException("cannot get userauth class", ex);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				session.close(context.getChannel(), ex.getMessage(), context, e.getMessage());
				throw ex;
			}
		} else {
			context.sendUpstream(evt);
		}
	}
}
