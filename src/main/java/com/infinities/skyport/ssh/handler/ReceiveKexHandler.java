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
import com.jcraft.jsch.CustomKeyExchange;
import com.jcraft.jsch.CustomSession;

public class ReceiveKexHandler implements ChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(ReceiveKexHandler.class);
	private CustomSession session;


	public ReceiveKexHandler(CustomSession session) {
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
			// ChannelBuffer buffer =
			try {
				CustomBuffer buf = (CustomBuffer) e.getMessage();
				// buf = session.read(buf, buffer);
				logger.debug("command received: {}", buf.getCommand());
				// Encode the message to base64
				if (buf.getCommand() != Config.SSH_MSG_KEXINIT) {
					session.setIn_kex(false);
					throw new KeyExchangeException("invalid protocol: " + buf.getCommand());
				}
				logger.info("SSH_MSG_KEXINIT received");
				context.getPipeline().remove(this);
				CustomKeyExchange kex = session.receiveKexinit(buf, context, evt);
				session.setKex(kex);
			} catch (Exception ex) {
				session.close(context.getChannel(), ex.getMessage(), context, e.getMessage());
				throw ex;
			}
		} else {
			context.sendUpstream(evt);
		}
	}
}
