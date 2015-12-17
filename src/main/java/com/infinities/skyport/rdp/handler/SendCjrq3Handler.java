package com.infinities.skyport.rdp.handler;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.rdp.custom.CustomVChannels;
import com.infinities.skyport.rdp.custom.MCSUtils;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.SecureUtils;
import com.lixia.rdp.Constants;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

//ISO.connect
//MCS.connect.sendConnectInitial
public class SendCjrq3Handler extends FrameDecoder {

	private static final Logger logger = LoggerFactory.getLogger(SendCjrq3Handler.class);
	/* Info Packet Flags */
	private static int INFO_MOUSE = 0x00000001;
	private static int INFO_DISABLECTRLALTDEL = 0x00000002;
	private static int INFO_AUTOLOGON = 0x00000008;
	private static int INFO_UNICODE = 0x00000010;
	private static int INFO_MAXIMIZESHELL = 0x00000020;
	private static int INFO_LOGONNOTIFY = 0x00000040;
	private static int INFO_COMPRESSION = 0x00000080;
	private static int INFO_ENABLEWINDOWSKEY = 0x00000100;
	private static int INFO_REMOTECONSOLEAUDIO = 0x00002000;
	// private static int INFO_FORCE_ENCRYPTED_CS_PDU = 0x00004000;
	// private static int INFO_RAIL = 0x00008000;
	private static int INFO_LOGONERRORS = 0x00010000;
	// private static int INFO_MOUSE_HAS_WHEEL = 0x00020000;
	// private static int INFO_PASSWORD_IS_SC_PIN = 0x00040000;
	// private static int INFO_NOAUDIOPLAYBACK = 0x00080000;
	// private static int INFO_USING_SAVED_CREDS = 0x00100000;
	private static int PACKET_COMPR_TYPE_64K = 0x00000200;
	private static int RNS_INFO_AUDIOCAPTURE = 0x00200000;
	private static int INFO_NORMALLOGON = (INFO_MOUSE | INFO_DISABLECTRLALTDEL | INFO_UNICODE | INFO_MAXIMIZESHELL);
	private static int CLIENT_INFO_AF_INET = 0x0002;
	private RDPSession session;
	private CustomVChannels channels;
	private int i = 0;
	private RdpPacket_Localised mcs_data;
	private String username;
	// private InetAddress server;
	private String domain;
	private String password;
	private String command;
	private String directory;

	private int connect_flags = INFO_NORMALLOGON;


	// private Options options;

	public SendCjrq3Handler(RDPSession session, RdpPacket_Localised mcs_data) {
		this.session = session;
		this.channels = session.getChannels();
		this.mcs_data = mcs_data;
		this.username = session.getOptions().getUsername();
		// this.server = server;
		this.domain = session.getOptions().getDomain();
		this.password = session.getOptions().getPassword();
		this.command = session.getOptions().getCommand();
		this.directory = session.getOptions().getDirectory();

		if (session.getOptions().isBulk_compression()) {
			connect_flags |= INFO_COMPRESSION | PACKET_COMPR_TYPE_64K;
		}
		if (session.getOptions().isAutologin()) {
			connect_flags |= INFO_AUTOLOGON;
		}
		if (session.getOptions().isConsole_audio()) {
			connect_flags |= INFO_REMOTECONSOLEAUDIO;
		}

		connect_flags |= INFO_UNICODE;
		connect_flags |= INFO_LOGONERRORS;
		connect_flags |= INFO_LOGONNOTIFY;
		connect_flags |= INFO_ENABLEWINDOWSKEY;
		connect_flags |= RNS_INFO_AUDIOCAPTURE;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		MCSUtils.receive_cjcf(ctx, buffer, session);

		if (i < channels.num_channels()) {
			MCSUtils.send_cjrq(channels.mcs_id(i), session.getMcsUserID(), ctx);
			i++;
		} else {
			SecureUtils.processMcsData(mcs_data, session);
			if (Constants.encryption) {
				SecureUtils.establishKey(session, ctx);
			}
			rdp_send_client_info(connect_flags, domain, username, password, command, directory, ctx);
			ctx.getPipeline().remove(this);
		}
		return null;
	}

	/**
	 * Send user logon details to the server
	 * 
	 * @param flags
	 *            Set of flags defining logon type
	 * @param domain
	 *            Domain for logon
	 * @param username
	 *            Username for logon
	 * @param password
	 *            Password for logon
	 * @param command
	 *            Alternative shell for session
	 * @param directory
	 *            Starting working directory for session
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	private void rdp_send_client_info(int flags, String domain, String username, String password, String command,
			String directory, ChannelHandlerContext ctx) throws RdesktopException, IOException, CryptoException {

		int len_ip = 2 * "127.0.0.1".length();
		int len_dll = 2 * "C:\\WINNT\\System32\\mstscax.dll".length();
		int packetlen = 0;
		logger.debug("flags:{}", flags);
		int sec_flags = SecureUtils.SEC_INFO_PKT | (Constants.encryption ? SecureUtils.SEC_ENCRYPT : 0);
		logger.debug("domain:{}", domain);
		int domainlen = 2 * domain.length();
		logger.debug("username:{}", username);
		int userlen = 2 * username.length();
		logger.debug("password:{}", password);
		int passlen = 2 * password.length();
		logger.debug("command:{}", command);
		int commandlen = 2 * command.length();
		logger.debug("directory:{}", directory);
		int dirlen = 2 * directory.length();

		packetlen = 8 + (5 * 4) + domainlen + userlen + passlen + commandlen + dirlen;
		if (session.getOptions().isUse_rdp5() && 1 != session.getOptions().getServer_rdp_version()) {
			// rdp 5
			packetlen += 180 + (2 * 4) + len_ip + len_dll;
		}
		RdpPacket_Localised data = SecureUtils.init(sec_flags, packetlen, session);

		data.setLittleEndian32(0);/* codePage */
		data.setLittleEndian32(flags);/* flags */

		data.setLittleEndian16(domainlen);
		data.setLittleEndian16(userlen);
		data.setLittleEndian16(passlen);
		data.setLittleEndian16(commandlen);
		data.setLittleEndian16(dirlen);
		data.outUnicodeString(domain, domainlen);
		data.outUnicodeString(username, userlen);
		data.outUnicodeString(password, passlen);
		data.outUnicodeString(command, commandlen);
		data.outUnicodeString(directory, dirlen);

		if (session.getOptions().isUse_rdp5() && 1 != session.getOptions().getServer_rdp_version()) {
			logger.debug("Sending RDP5-style Logon packet");

			data.setLittleEndian16(CLIENT_INFO_AF_INET);
			data.setLittleEndian16(len_ip + 2);
			// // Length of client ip
			data.outUnicodeString("127.0.0.1", len_ip);
			data.setLittleEndian16(len_dll + 2);
			data.outUnicodeString("C:\\WINNT\\System32\\mstscax.dll", len_dll);

			/* clientTimeZone (172 bytes) */
			rdp_out_client_timezone_info(data);

			data.setLittleEndian32(2); // out_uint32_le(s, 2);
			data.setLittleEndian32(0); // out_uint32(s, 0);
			// data.setLittleEndian32(0xffffffc4); // out_uint32_le(s,
			// 0xffffffc4);
			// data.setLittleEndian32(0xfffffffe); // out_uint32_le(s,
			// 0xfffffffe);
			logger.debug("performanceflags:{}", session.getOptions().getRdp5_performanceflags());
			data.setLittleEndian32(session.getOptions().getRdp5_performanceflags()); // out_uint32_le(s,
			// 0x0f);
			data.setLittleEndian32(0); // out_uint32(s, 0);
		}

		data.markEnd();
		byte[] buffer = new byte[data.getEnd()];
		data.copyToByteArray(buffer, 0, 0, data.getEnd());
		SecureUtils.send(data, sec_flags, session, ctx);
	}

	private void rdp_out_client_timezone_info(RdpPacket_Localised data) {

		data.setLittleEndian16(0xffc4); // out_uint16_le(s, 0xffc4);
		data.setLittleEndian16(0xffff); // out_uint16_le(s, 0xffff);
		data.outUnicodeString("GTB, normaltid", 2 * "GTB, normaltid".length()); // rdp_out_unistr(s,
																				// "GTB, normaltid",
																				// 2
																				// *
		// strlen("GTB, normaltid"));
		data.incrementPosition(62 - 2 * "GTB, normaltid".length()); // out_uint8s(s,
		// 62 -
		// 2 *
		// strlen("GTB,
		// normaltid"));

		data.setLittleEndian32(0x0a0000); // out_uint32_le(s, 0x0a0000);
		data.setLittleEndian32(0x050000); // out_uint32_le(s, 0x050000);
		data.setLittleEndian32(3); // out_uint32_le(s, 3);
		data.setLittleEndian32(0); // out_uint32_le(s, 0);
		data.setLittleEndian32(0); // out_uint32_le(s, 0);

		data.outUnicodeString("GTB, sommartid", 2 * "GTB, sommartid".length()); // rdp_out_unistr(s,
																				// "GTB, sommartid",
																				// 2
																				// *
		// strlen("GTB, sommartid"));
		data.incrementPosition(62 - 2 * "GTB, sommartid".length()); // out_uint8s(s,
		// 62 -
		// 2 *
		// strlen("GTB,
		// sommartid"));

		data.setLittleEndian32(0x30000); // out_uint32_le(s, 0x30000);
		data.setLittleEndian32(0x050000); // out_uint32_le(s, 0x050000);
	}
}
