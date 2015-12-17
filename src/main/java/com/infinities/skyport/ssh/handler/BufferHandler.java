package com.infinities.skyport.ssh.handler;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.ssh.handler.BufferHandler.DecodingState;
import com.infinities.skyport.vnc.util.SSHUtil;
import com.jcraft.jsch.Cipher;
import com.jcraft.jsch.CustomBuffer;
import com.jcraft.jsch.CustomKeyExchange;
import com.jcraft.jsch.CustomPacket;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.CustomUserAuth;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.MAC;

public class BufferHandler extends ReplayingDecoder<DecodingState> {

	private final static Logger logger = LoggerFactory.getLogger(BufferHandler.class);
	private CustomBuffer buf = new CustomBuffer();
	private CustomPacket packet = new CustomPacket(buf);
	private int j = 0;
	private int need = 0;
	private int type = 0;


	public enum DecodingState {
		State1, State2, State3;
	}


	private CustomSession session;


	public BufferHandler(CustomSession session) {
		super(DecodingState.State1);
		this.session = session;
		packet.reset();
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, DecodingState state)
			throws Exception {

		loop: while (true) {
			switch (state) {
			case State1:
				buf.reset();
				buffer.readBytes(buf.getBuffer(), buf.getIndex(), session.getS2ccipher_size());
				// logger.debug("buffer:{}", new String(buf.getBuffer()));s
				buf.setIndex(buf.getIndex() + session.getS2ccipher_size());
				if (session.getS2ccipher() != null) {
					session.getS2ccipher().update(buf.getBuffer(), 0, session.getS2ccipher_size(), buf.getBuffer(), 0);
				}
				j = ((buf.getBuffer()[0] << 24) & 0xff000000) | ((buf.getBuffer()[1] << 16) & 0x00ff0000)
						| ((buf.getBuffer()[2] << 8) & 0x0000ff00) | ((buf.getBuffer()[3]) & 0x000000ff);
				// RFC 4253 6.1. Maximum Packet Length
				logger.debug("j: {}", j);
				if (j < 5 || j > CustomSession.PACKET_MAX_SIZE) {
					start_discard(buffer, buf, session.getS2ccipher(), session.getS2cmac(), j, CustomSession.PACKET_MAX_SIZE);
				}
				need = j + 4 - session.getS2ccipher_size();
				logger.debug("need: {}", need);
				if ((buf.getIndex() + need) > buf.getBuffer().length) {
					byte[] foo = new byte[buf.getIndex() + need];
					System.arraycopy(buf.getBuffer(), 0, foo, 0, buf.getIndex());
					buf.setBuffer(foo);
				}

				if ((need % session.getS2ccipher_size()) != 0) {
					String message = "Bad packet length " + need;
					logger.error(message);
					start_discard(buffer, buf, session.getS2ccipher(), session.getS2cmac(), j, CustomSession.PACKET_MAX_SIZE
							- session.getS2ccipher_size());
				}
				checkpoint(DecodingState.State2);
			case State2:
				if (need > 0) {
					logger.debug("buffer size:{}, index:{} need:{}, {}" + buffer.readableBytes(),
							new Object[] { buf.getBuffer().length, buf.getIndex(), need, buffer.readableBytes() });

					buffer.readBytes(buf.getBuffer(), buf.getIndex(), need);
					buf.setIndex(buf.getIndex() + need);
					if (session.getS2ccipher() != null) {
						session.getS2ccipher().update(buf.getBuffer(), session.getS2ccipher_size(), need, buf.getBuffer(),
								session.getS2ccipher_size());
					}
				}
				if (session.getS2cmac() != null) {
					logger.debug("seqi: {}", session.getSeqi());
					session.getS2cmac().update(session.getSeqi());
					session.getS2cmac().update(buf.getBuffer(), 0, buf.getIndex());
					// logger.debug("s2cmac_result1: {}, s2cmac_result2:{}", new
					// Object[] { s2cmac_result1.length,
					// s2cmac_result2.length });
					session.getS2cmac().doFinal(session.getS2cmac_result1(), 0);
				}
				checkpoint(DecodingState.State3);
			case State3:
				if (session.getS2cmac() != null) {
					buffer.readBytes(session.getS2cmac_result2(), 0, session.getS2cmac_result2().length);
					if (!java.util.Arrays.equals(session.getS2cmac_result1(), session.getS2cmac_result2())) {
						if (need > CustomSession.PACKET_MAX_SIZE) {
							throw new IOException("MAC Error");
						}
						start_discard(buffer, buf, session.getS2ccipher(), session.getS2cmac(), j,
								CustomSession.PACKET_MAX_SIZE - need);
						checkpoint(DecodingState.State1);
						continue;
					}
				}
				session.setSeqi(session.getSeqi() + 1);
				if (session.getInflater() != null) {
					// inflater.uncompress(buf);
					int pad = buf.getBuffer()[4];
					session.getUncompress_len()[0] = buf.getIndex() - 5 - pad;
					byte[] foo = session.getInflater().uncompress(buf.getBuffer(), 5, session.getUncompress_len());
					if (foo != null) {
						buf.setBuffer(foo);
						buf.setIndex(5 + session.getUncompress_len()[0]);
					} else {
						logger.error("fail in inflater");
						checkpoint(DecodingState.State1);
						break loop;
					}
				}
				type = buf.getCommand() & 0xff;
				logger.debug("read type: {}", new Object[] { type });
				if (type == CustomSession.SSH_MSG_DISCONNECT) {
					buf.rewind();
					buf.getInt();
					buf.getShort();
					int reason_code = buf.getInt();
					byte[] description = buf.getString();
					byte[] language_tag = buf.getString();
					throw new JSchException("SSH_MSG_DISCONNECT: " + reason_code + " " + SSHUtil.byte2str(description) + " "
							+ SSHUtil.byte2str(language_tag));
					// break;
				} else if (type == CustomSession.SSH_MSG_IGNORE) {
				} else if (type == CustomSession.SSH_MSG_UNIMPLEMENTED) {
					buf.rewind();
					buf.getInt();
					buf.getShort();
					int reason_id = buf.getInt();
					logger.info("Received SSH_MSG_UNIMPLEMENTED for {}", reason_id);
				} else if (type == CustomSession.SSH_MSG_DEBUG) {
					buf.rewind();
					buf.getInt();
					buf.getShort();
					/*
					 * byte always_display=(byte)buf.getByte(); byte[]
					 * message=buf.getString(); byte[]
					 * language_tag=buf.getString();
					 * System.err.println("SSH_MSG_DEBUG:"+
					 * " "+Util.byte2str(message)+
					 * " "+Util.byte2str(language_tag));
					 */
				} else if (type == CustomSession.SSH_MSG_CHANNEL_WINDOW_ADJUST) {
					buf.rewind();
					buf.getInt();
					buf.getShort();
					buf.getInt();
					if (channel == null) {
					} else {
						long size = buf.getUInt();
						logger.debug("int:{}", new Object[] { size });
						session.getChannel().addRemoteWindowSize(size);
					}

				} else if (type == CustomUserAuth.SSH_MSG_USERAUTH_SUCCESS) {
					session.setAuthed(true);
					if (session.getInflater() == null && session.getDeflater() == null) {
						String method;
						method = session.getGuess()[CustomKeyExchange.PROPOSAL_COMP_ALGS_CTOS];
						session.initDeflater(method);

						method = session.getGuess()[CustomKeyExchange.PROPOSAL_COMP_ALGS_STOC];
						session.initInflater(method);
					}
					checkpoint(DecodingState.State1);
					break loop;
				} else {
					checkpoint(DecodingState.State1);
					break loop;
				}
				// checkpoint(DecodingState.State1);
				break;
			default:
				logger.error("unimplemented state:{}", state);
				throw new RuntimeException("unimplemented state:" + state);
			}
		}

		buf.rewind();
		return buf;
	}

	public void start_discard(ChannelBuffer buffer, CustomBuffer buf, Cipher cipher, MAC mac, int packet_length, int discard)
			throws JSchException, IOException {
		MAC discard_mac = null;

		if (!cipher.isCBC()) {
			throw new JSchException("Packet corrupt");
		}

		if (packet_length != CustomSession.PACKET_MAX_SIZE && mac != null) {
			discard_mac = mac;
		}

		discard -= buf.getIndex();

		while (discard > 0) {
			buf.reset();
			int len = discard > buf.getBuffer().length ? buf.getBuffer().length : discard;
			buffer.readBytes(buf.getBuffer(), 0, len);
			if (discard_mac != null) {
				discard_mac.update(buf.getBuffer(), 0, len);
			}
			discard -= len;
		}

		if (discard_mac != null) {
			discard_mac.doFinal(buf.getBuffer(), 0);
		}

		throw new JSchException("Packet corrupt");
	}

}
