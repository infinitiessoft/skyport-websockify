package com.infinities.skyport.rdp.custom;

import java.io.IOException;
import java.math.BigInteger;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.CryptoException;

public class SecureUtils {

	private static final Logger logger = LoggerFactory.getLogger(SecureUtils.class);
	private static final int SEC_TAG_SRV_INFO = 0x0c01;
	private static final int SEC_TAG_SRV_CRYPT = 0x0c02;
	// private static final int SEC_TAG_SRV_3 = 0x0c03;
	private static final int SEC_TAG_SRV_CHANNELS = 0x0c03;

	private static final int SEC_TAG_CLI_INFO = 0xc001;
	private static final int SEC_TAG_CLI_CRYPT = 0xc002;
	private static final int SEC_TAG_CLI_CHANNELS = 0xc003;
	private static final int SEC_TAG_CLI_4 = 0xc004;

	private static final int SEC_TAG_PUBKEY = 0x0006;
	private static final int SEC_TAG_KEYSIG = 0x0008;
	private static final int SEC_RANDOM_SIZE = 32;
	private static final int SEC_RSA_MAGIC = 0x31415352; /* RSA1 */
	public static final int SEC_ENCRYPT = 0x0008;
	public static final int SEC_INFO_PKT = 0x0040;

	private static final int SEC_MODULUS_SIZE = 64;
	private static final int SEC_MAX_MODULUS_SIZE = 256;
	private static final int SEC_PADDING_SIZE = 8;
	private static final int SEC_EXPONENT_SIZE = 4;

	private static final int SEC_CLIENT_RANDOM = 0x0001;


	// private static final int SEC_LICENCE_NEG = 0x0080;

	/**
	 * Handle MCS info from server (server info, encryption info and channel
	 * information)
	 * 
	 * @param mcs_data
	 *            Data received from server
	 */
	public static void processMcsData(RdpPacket_Localised mcs_data, RDPSession session) throws RdesktopException,
			CryptoException {
		logger.debug("Secure.processMcsData");
		int tag = 0, len = 0, length = 0, nexttag = 0;

		mcs_data.incrementPosition(21); // header (T.124 stuff, probably)
		len = mcs_data.get8();

		if ((len & 0x00000080) != 0) {
			len = mcs_data.get8();
		}

		while (mcs_data.getPosition() < mcs_data.getEnd()) {
			tag = mcs_data.getLittleEndian16();
			length = mcs_data.getLittleEndian16();

			if (length <= 4)
				return;

			nexttag = mcs_data.getPosition() + length - 4;

			switch (tag) {
			case (SEC_TAG_SRV_INFO):
				processSrvInfo(mcs_data, session);
				break;
			case (SEC_TAG_SRV_CRYPT):
				processCryptInfo(mcs_data, session);
				break;
			case (SEC_TAG_SRV_CHANNELS):
				/*
				 * FIXME: We should parse this information and use it to map
				 * RDP5 channels to MCS channels
				 */
				break;

			default:
				throw new RdesktopException("Not implemented! Tag:" + tag + "not recognized!");
			}

			logger.debug("mcs_data setPosition", nexttag);
			mcs_data.setPosition(nexttag);
		}
	}

	/**
	 * Read server info from packet, specifically the RDP version of the server
	 * 
	 * @param mcs_data
	 *            Packet to read
	 */
	private static void processSrvInfo(RdpPacket_Localised mcs_data, RDPSession session) {
		session.getOptions().setServer_rdp_version(mcs_data.getLittleEndian16()); // in_uint16_le(s,
		// g_server_rdp_version);
		logger.debug(("Server RDP version is " + session.getOptions().getServer_rdp_version()));
		if (1 == session.getOptions().getServer_rdp_version())
			session.getOptions().setUse_rdp5(false);
	}

	public static void processCryptInfo(RdpPacket_Localised data, RDPSession session) throws RdesktopException,
			CryptoException {
		int rc4_key_size = 0;

		rc4_key_size = parseCryptInfo(data, session);
		if (rc4_key_size == 0) {
			return;
		}

		// this.client_random = this.generateRandom(SEC_RANDOM_SIZE);
		logger.debug("readCert = " + session.isReadCert());
		if (session.isReadCert()) { /*
									 * Which means we should use RDP5-style
									 * encryption
									 */

			// *** reverse the client random
			// this.reverse(this.client_random);

			// *** load the server public key into the stored data for
			// encryption
			/*
			 * this.exponent =
			 * this.server_public_key.getPublicExponent().toByteArray();
			 * this.modulus = this.server_public_key.getModulus().toByteArray();
			 * 
			 * System.out.println("Exponent: " +
			 * server_public_key.getPublicExponent());
			 * System.out.println("Modulus: " + server_public_key.getModulus());
			 */

			// *** perform encryption
			// this.sec_crypted_random = RSA_public_encrypt(this.client_random,
			// this.server_public_key);
			// this.RSAEncrypt(SEC_RANDOM_SIZE);

			// this.RSAEncrypt(SEC_RANDOM_SIZE);

			// *** reverse the random data back
			// this.reverse(this.sec_crypted_random);

		} else {
			generateRandom();
			RSAEncrypt(SEC_RANDOM_SIZE, session);
		}
		generate_keys(rc4_key_size, session);
	}

	/**
	 * Read encryption information from a Secure layer PDU, obtaining and
	 * storing level of encryption and any keys received
	 * 
	 * @param data
	 *            Packet to read encryption information from
	 * @return Size of RC4 key
	 * @throws RdesktopException
	 */
	public static int parseCryptInfo(RdpPacket_Localised data, RDPSession session) throws RdesktopException {
		logger.debug("Secure.parseCryptInfo");
		int encryption_level = 0, random_length = 0, RSA_info_length = 0;
		int tag = 0, length = 0;
		int next_tag = 0, end = 0;
		int rc4_key_size = 0;

		rc4_key_size = data.getLittleEndian32(); // 1 = 40-Bit 2 = 128 Bit
		encryption_level = data.getLittleEndian32(); // 1 = low, 2 = medium, 3 =
														// high
		if (encryption_level == 0) { // no encryption
			return 0;
		}
		random_length = data.getLittleEndian32();
		RSA_info_length = data.getLittleEndian32();

		if (random_length != SEC_RANDOM_SIZE) {
			throw new RdesktopException("Wrong Size of Random! Got" + random_length + "expected" + SEC_RANDOM_SIZE);
		}
		session.setServer_random(new byte[random_length]);
		data.copyToByteArray(session.getServer_random(), 0, data.getPosition(), random_length);
		data.incrementPosition(random_length);

		end = data.getPosition() + RSA_info_length;

		if (end > data.getEnd()) {
			logger.debug("Reached end of crypt info prematurely ");
			return 0;
		}

		// data.incrementPosition(12); // unknown bytes
		int flags = data.getLittleEndian32(); // in_uint32_le(s, flags); // 1 =
												// RDP4-style, 0x80000002 =
												// X.509
		logger.debug("Flags = 0x" + Integer.toHexString(flags));
		if ((flags & 1) != 0) {
			logger.debug(("We're going for the RDP4-style encryption"));
			data.incrementPosition(8); // in_uint8s(s, 8); // unknown

			while (data.getPosition() < data.getEnd()) {
				tag = data.getLittleEndian16();
				length = data.getLittleEndian16();

				next_tag = data.getPosition() + length;

				switch (tag) {

				case (SEC_TAG_PUBKEY):

					if (!parsePublicKey(data, session)) {
						return 0;
					}

					break;
				case (SEC_TAG_KEYSIG):
					// Microsoft issued a key but we don't care
					break;

				default:
					throw new RdesktopException("Unimplemented decrypt tag " + tag);
				}
				data.setPosition(next_tag);
			}

			if (data.getPosition() == data.getEnd()) {
				return rc4_key_size;
			} else {
				logger.warn("End not reached!");
				return 0;
			}

		} else {
			// data.incrementPosition(4); // number of certificates
			int num_certs = data.getLittleEndian32();
			logger.debug("num_certs: {}", num_certs);

			int cacert_len = data.getLittleEndian32();
			data.incrementPosition(cacert_len);
			int cert_len = data.getLittleEndian32();
			data.incrementPosition(cert_len);

			session.setReadCert(true);

			return rc4_key_size;
		}

	}

	/**
	 * Generate encryption keys of applicable size for connection
	 * 
	 * @param rc4_key_size
	 *            Size of keys to generate (1 if 40-bit encryption, otherwise
	 *            128-bit)
	 * @throws CryptoException
	 */
	public static void generate_keys(int rc4_key_size, RDPSession session) throws CryptoException {
		byte[] session_key = new byte[48];
		byte[] temp_hash = new byte[48];
		byte[] input = new byte[48];

		System.arraycopy(session.getClient_random(), 0, input, 0, 24);
		System.arraycopy(session.getServer_random(), 0, input, 24, 24);

		temp_hash = hash48(input, session.getClient_random(), session.getServer_random(), 65, session);
		session_key = hash48(temp_hash, session.getClient_random(), session.getServer_random(), 88, session);

		System.arraycopy(session_key, 0, session.getSec_sign_key(), 0, 16); // changed
		// from 8 -
		// rdesktop
		// 1.2.0

		session.setSec_decrypt_key(hash16(session_key, session.getClient_random(), session.getServer_random(), 16, session));
		session.setSec_encrypt_key(hash16(session_key, session.getClient_random(), session.getServer_random(), 32, session));

		if (rc4_key_size == 1) {
			logger.info("40 Bit Encryption enabled");
			make40bit(session.getSec_sign_key());
			make40bit(session.getSec_decrypt_key());
			make40bit(session.getSec_encrypt_key());
			session.setKeylength(8);
		} else {
			logger.info("128 Bit Encryption enabled");
			session.setKeylength(16);
		}

		System.arraycopy(session.getSec_decrypt_key(), 0, session.getSec_decrypt_update_key(), 0, 16); // changed
		// from
		// 8
		// -
		// rdesktop
		// 1.2.0
		System.arraycopy(session.getSec_encrypt_key(), 0, session.getSec_encrypt_update_key(), 0, 16); // changed
		// from
		// 8
		// -
		// rdesktop
		// 1.2.0

		byte[] key = new byte[session.getKeylength()];
		System.arraycopy(session.getSec_encrypt_key(), 0, key, 0, session.getKeylength());
		session.getRc4_enc().engineInitEncrypt(key);
		System.arraycopy(session.getSec_decrypt_key(), 0, key, 0, session.getKeylength());
		session.getRc4_dec().engineInitDecrypt(key);
	}

	public static void generateRandom() {
		/*
		 * try{ SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		 * random.nextBytes(this.client_random); }
		 * catch(NoSuchAlgorithmException
		 * e){logger.warn("No Such Random Algorithm");}
		 */
	}

	public static void RSAEncrypt(int length, RDPSession session) throws RdesktopException {
		byte[] inr = new byte[length];
		// int outlength = 0;
		BigInteger mod = null;
		BigInteger exp = null;
		BigInteger x = null;

		reverse(session.getExponent());
		reverse(session.getModulus());
		System.arraycopy(session.getClient_random(), 0, inr, 0, length);
		reverse(inr);

		if ((session.getModulus()[0] & 0x80) != 0) {
			byte[] temp = new byte[session.getModulus().length + 1];
			System.arraycopy(session.getModulus(), 0, temp, 1, session.getModulus().length);
			temp[0] = 0;
			mod = new BigInteger(temp);
		} else {
			mod = new BigInteger(session.getModulus());
		}
		if ((session.getExponent()[0] & 0x80) != 0) {
			byte[] temp = new byte[session.getExponent().length + 1];
			System.arraycopy(session.getExponent(), 0, temp, 1, session.getExponent().length);
			temp[0] = 0;
			exp = new BigInteger(temp);
		} else {
			exp = new BigInteger(session.getExponent());
		}
		if ((inr[0] & 0x80) != 0) {
			byte[] temp = new byte[inr.length + 1];
			System.arraycopy(inr, 0, temp, 1, inr.length);
			temp[0] = 0;
			x = new BigInteger(temp);
		} else {
			x = new BigInteger(inr);
		}

		BigInteger y = x.modPow(exp, mod);
		session.setSec_crypted_random(y.toByteArray());

		if ((session.getSec_crypted_random()[0] & 0x80) != 0) {
			throw new RdesktopException("Wrong Sign! Expected positive Integer!");
		}

		if (session.getSec_crypted_random().length > session.getServer_public_key_len()) {
			logger.warn("sec_crypted_random too big!"); /* FIXME */
		}
		reverse(session.getSec_crypted_random());

		byte[] temp = new byte[session.getServer_public_key_len()];

		if (session.getSec_crypted_random().length < session.getServer_public_key_len()) {
			System.arraycopy(session.getSec_crypted_random(), 0, temp, 0, session.getSec_crypted_random().length);
			for (int i = session.getSec_crypted_random().length; i < temp.length; i++) {
				temp[i] = 0;
			}
			session.setSec_crypted_random(temp);

		}

	}

	/**
	 * Reverse the values in the provided array
	 * 
	 * @param data
	 *            Array as passed reversed on return
	 */
	public static void reverse(byte[] data) {
		int i = 0, j = 0;
		byte temp = 0;

		for (i = 0, j = data.length - 1; i < j; i++, j--) {
			temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}
	}

	public static void reverse(byte[] data, int length) {
		int i = 0, j = 0;
		byte temp = 0;

		for (i = 0, j = length - 1; i < j; i++, j--) {
			temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}
	}

	/**
	 * Read in a public key from a provided Secure layer PDU, and store in
	 * this.exponent and this.modulus
	 * 
	 * @param data
	 *            Secure layer PDU containing key data
	 * @return True if key successfully read
	 * @throws RdesktopException
	 */
	public static boolean parsePublicKey(RdpPacket_Localised data, RDPSession session) throws RdesktopException {
		int magic = 0, modulus_length = 0;

		magic = data.getLittleEndian32();

		if (magic != SEC_RSA_MAGIC) {
			throw new RdesktopException("Wrong magic! Expected" + SEC_RSA_MAGIC + "got:" + magic);
		}

		modulus_length = data.getLittleEndian32() - SEC_PADDING_SIZE;

		if ((modulus_length < SEC_MODULUS_SIZE) || (modulus_length > SEC_MAX_MODULUS_SIZE)) {
			throw new RdesktopException("Wrong modulus size! Expected" + SEC_MODULUS_SIZE + "+" + SEC_PADDING_SIZE + "got:"
					+ modulus_length);
		}

		data.incrementPosition(8); // unknown modulus bits
		session.setExponent(new byte[SEC_EXPONENT_SIZE]);
		data.copyToByteArray(session.getExponent(), 0, data.getPosition(), SEC_EXPONENT_SIZE);
		data.incrementPosition(SEC_EXPONENT_SIZE);
		session.setModulus(new byte[modulus_length]);
		data.copyToByteArray(session.getModulus(), 0, data.getPosition(), modulus_length);
		data.incrementPosition(modulus_length);
		data.incrementPosition(SEC_PADDING_SIZE);
		session.setServer_public_key_len(modulus_length);

		if (data.getPosition() <= data.getEnd()) {
			return true;
		} else {
			return false;
		}
	}

	public static byte[] hash48(byte[] in, byte[] salt1, byte[] salt2, int salt, RDPSession session) throws CryptoException {
		byte[] shasig = new byte[20];
		byte[] pad = new byte[4];
		byte[] out = new byte[48];
		int i = 0;

		for (i = 0; i < 3; i++) {
			for (int j = 0; j <= i; j++) {
				pad[j] = (byte) (salt + i);
			}
			session.getSha1().engineUpdate(pad, 0, i + 1);
			session.getSha1().engineUpdate(in, 0, 48);
			session.getSha1().engineUpdate(salt1, 0, 32);
			session.getSha1().engineUpdate(salt2, 0, 32);
			shasig = session.getSha1().engineDigest();
			session.getSha1().engineReset();

			session.getMd5().engineUpdate(in, 0, 48);
			session.getMd5().engineUpdate(shasig, 0, 20);
			System.arraycopy(session.getMd5().engineDigest(), 0, out, i * 16, 16);
		}

		return out;
	}

	public static byte[] hash16(byte[] in, byte[] salt1, byte[] salt2, int in_position, RDPSession session)
			throws CryptoException {

		session.getMd5().engineUpdate(in, in_position, 16);
		session.getMd5().engineUpdate(salt1, 0, 32);
		session.getMd5().engineUpdate(salt2, 0, 32);
		return session.getMd5().engineDigest();
	}

	/**
	 * Generate a 40-bit key and store in the parameter key.
	 * 
	 * @param key
	 */
	public static void make40bit(byte[] key) {
		key[0] = (byte) 0xd1;
		key[1] = (byte) 0x26;
		key[2] = (byte) 0x9e;
	}

	public static void establishKey(RDPSession session, ChannelHandlerContext context) throws RdesktopException,
			IOException, CryptoException {
		int length = session.getServer_public_key_len() + SEC_PADDING_SIZE;
		int flags = SEC_CLIENT_RANDOM;
		RdpPacket_Localised buffer = init(flags, length + 4, session);

		buffer.setLittleEndian32(length);

		buffer.copyFromByteArray(session.getSec_crypted_random(), 0, buffer.getPosition(),
				session.getServer_public_key_len());
		buffer.incrementPosition(session.getServer_public_key_len());
		buffer.incrementPosition(SEC_PADDING_SIZE);
		buffer.markEnd();
		send(buffer, flags, session, context);
	}

	/**
	 * Intialise a packet at the Secure layer
	 * 
	 * @param flags
	 *            Encryption flags
	 * @param length
	 *            Length of packet
	 * @return Intialised packet
	 * @throws RdesktopException
	 */
	public static RdpPacket_Localised init(int flags, int length, RDPSession session) throws RdesktopException {
		int headerlength = 0;
		RdpPacket_Localised buffer;
		logger.debug("licenseIssued? {}, flags:{}, length:{}", new Object[] { session.isLicenceIssued(), flags, length });
		if (!session.isLicenceIssued())
			headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 4;
		else
			headerlength = ((flags & SEC_ENCRYPT) != 0) ? 12 : 0;

		buffer = MCSUtils.init(length + headerlength);
		buffer.pushLayer(RdpPacket_Localised.SECURE_HEADER, headerlength);
		// buffer.setHeader(RdpPacket_Localised.SECURE_HEADER);
		// buffer.incrementPosition(headerlength);
		// buffer.setStart(buffer.getPosition());
		return buffer;
	}

	/**
	 * Send secure data on the global channel
	 * 
	 * @param sec_data
	 *            Data to send
	 * @param flags
	 *            Encryption flags
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public static void send(RdpPacket_Localised sec_data, int flags, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {
		send_to_channel(sec_data, flags, MCSUtils.MCS_GLOBAL_CHANNEL, session, context);
	}

	/**
	 * Prepare data as a Secure PDU and pass down to the MCS layer
	 * 
	 * @param sec_data
	 *            Data to send
	 * @param flags
	 *            Encryption flags
	 * @param channel
	 *            Channel over which to send data
	 * @throws RdesktopException
	 * @throws IOException
	 * @throws CryptoException
	 */
	public static void send_to_channel(RdpPacket_Localised sec_data, int flags, int channel, RDPSession session,
			ChannelHandlerContext context) throws RdesktopException, IOException, CryptoException {
		logger.debug("sec_data:{}, flags:{}, channel:{}", new Object[] { sec_data, flags, channel });
		int datalength = 0;
		byte[] signature = null;
		byte[] data;
		byte[] buffer;

		sec_data.setPosition(sec_data.getHeader(RdpPacket_Localised.SECURE_HEADER));

		if (session.isLicenceIssued() == false || (flags & SEC_ENCRYPT) != 0) {
			sec_data.setLittleEndian32(flags);
		}
		if ((flags & SEC_ENCRYPT) != 0) {
			logger.debug("sec_encrypt");
			flags &= ~SEC_ENCRYPT;
			datalength = sec_data.getEnd() - sec_data.getPosition() - 8;
			data = new byte[datalength];
			buffer = null;
			sec_data.copyToByteArray(data, 0, sec_data.getPosition() + 8, datalength);
			signature = sign(session.getSec_sign_key(), 8, session.getKeylength(), data, datalength, session);

			buffer = encrypt(data, datalength, session);

			sec_data.copyFromByteArray(signature, 0, sec_data.getPosition(), 8);
			sec_data.copyFromByteArray(buffer, 0, sec_data.getPosition() + 8, datalength);

		}
		// McsLayer.send(sec_data);
		MCSUtils.send_to_channel(sec_data, channel, session, context);
	}

	/**
	 * Generate MD5 signature
	 * 
	 * @param session_key
	 *            Key with which to sign data
	 * @param length
	 *            Length of signature
	 * @param keylen
	 *            Length of key
	 * @param data
	 *            Data to sign
	 * @param datalength
	 *            Length of data to sign
	 * @return Signature for data
	 * @throws CryptoException
	 */
	public static byte[] sign(byte[] session_key, int length, int keylen, byte[] data, int datalength, RDPSession session)
			throws CryptoException {
		byte[] shasig = new byte[20];
		byte[] md5sig = new byte[16];
		byte[] lenhdr = new byte[4];
		byte[] signature = new byte[length];
		logger.debug("lenhdr:{}, datalength:{}", new Object[] { lenhdr, datalength });
		setLittleEndian32(lenhdr, datalength);

		session.getSha1().engineReset();
		session.getSha1().engineUpdate(session_key, 0, keylen/* length */);
		session.getSha1().engineUpdate(session.getPad54(), 0, 40);
		session.getSha1().engineUpdate(lenhdr, 0, 4);
		session.getSha1().engineUpdate(data, 0, datalength);
		shasig = session.getSha1().engineDigest();
		session.getSha1().engineReset();

		session.getMd5().engineReset();
		session.getMd5().engineUpdate(session_key, 0, keylen/* length */);
		session.getMd5().engineUpdate(session.getPad92(), 0, 48);
		session.getMd5().engineUpdate(shasig, 0, 20);
		md5sig = session.getMd5().engineDigest();
		session.getMd5().engineReset();

		System.arraycopy(md5sig, 0, signature, 0, length);
		return signature;
	}

	/**
	 * Write a 32-bit integer value to an array of bytes, length 4
	 * 
	 * @param data
	 *            Modified by method to be a 4-byte array representing the
	 *            parameter value
	 * @param value
	 *            Integer value to return as a little-endian 32-bit value
	 */
	public static void setLittleEndian32(byte[] data, int value) {

		data[3] = (byte) ((value >>> 24) & 0xff);
		data[2] = (byte) ((value >>> 16) & 0xff);
		data[1] = (byte) ((value >>> 8) & 0xff);
		data[0] = (byte) (value & 0xff);
	}

	/**
	 * Encrypt specified number of bytes from provided data using RC4 algorithm
	 * 
	 * @param data
	 *            Data to encrypt
	 * @param length
	 *            Number of bytes to encrypt (from start of array)
	 * @return Encrypted data
	 * @throws CryptoException
	 */
	public static byte[] encrypt(byte[] data, int length, RDPSession session) throws CryptoException {
		byte[] buffer = null;
		if (session.getEnc_count() == 4096) {
			logger.debug("enc_count == 4096");
			session.setSec_encrypt_key(update(session.getSec_encrypt_key(), session.getSec_encrypt_update_key(), session));
			byte[] key = new byte[session.getKeylength()];
			System.arraycopy(session.getSec_encrypt_key(), 0, key, 0, session.getKeylength());
			session.getRc4_enc().engineInitEncrypt(key);
			// logger.debug("Packet enc_count="+enc_count);
			session.setEnc_count(0);
		}
		// this.rc4.engineInitEncrypt(this.rc4_encrypt_key);
		buffer = session.getRc4_enc().crypt(data, 0, length);
		session.setEnc_count(session.getEnc_count() + 1);
		logger.debug("Enc_count:{}", session.getEnc_count());
		return buffer;
	}

	/**
	 * 
	 * @param key
	 * @param update_key
	 * @return
	 * @throws CryptoException
	 */
	public static byte[] update(byte[] key, byte[] update_key, RDPSession session) throws CryptoException {
		byte[] shasig = new byte[20];
		byte[] update = new byte[session.getKeylength()]; // changed from 8 -
															// rdesktop
		// 1.2.0
		byte[] thekey = new byte[key.length];

		session.getSha1().engineReset();
		session.getSha1().engineUpdate(update_key, 0, session.getKeylength());
		session.getSha1().engineUpdate(session.getPad54(), 0, 40);
		session.getSha1().engineUpdate(key, 0, session.getKeylength()); // changed
																		// from
																		// 8 -
		// rdesktop 1.2.0
		shasig = session.getSha1().engineDigest();
		session.getSha1().engineReset();

		session.getMd5().engineReset();
		session.getMd5().engineUpdate(update_key, 0, session.getKeylength()); // changed
																				// from
		// 8 -
		// rdesktop
		// 1.2.0
		session.getMd5().engineUpdate(session.getPad92(), 0, 48);
		session.getMd5().engineUpdate(shasig, 0, 20);
		thekey = session.getMd5().engineDigest();
		session.getMd5().engineReset();

		System.arraycopy(thekey, 0, update, 0, session.getKeylength());
		session.getRc4_update().engineInitDecrypt(update);
		// added
		thekey = session.getRc4_update().crypt(thekey, 0, session.getKeylength());

		if (session.getKeylength() == 8) {
			make40bit(thekey);
		}

		return thekey;
	}

	/**
	 * Construct MCS data, including channel, encryption and display options
	 * 
	 * @return Packet populated with MCS data
	 */
	public static RdpPacket_Localised sendMcsData(RDPSession session, String hostname) {
		logger.debug("Secure.sendMcsData");

		RdpPacket_Localised buffer = new RdpPacket_Localised(512);
		logger.debug("Options.hostname:{}", hostname);
		int hostlen = 2 * (hostname == null ? 0 : hostname.length());

		if (hostlen > 30) {
			hostlen = 30;
		}

		int length = 158;
		logger.debug("Options.use_rdp5: {}", session.getOptions().isUse_rdp5());
		if (session.getOptions().isUse_rdp5())
			length += 76 + 12 + 4;

		logger.debug("channels.num_channels(): {}", session.getChannels().num_channels());
		if (session.getOptions().isUse_rdp5() && (session.getChannels().num_channels() > 0))
			length += session.getChannels().num_channels() * 12 + 8;

		buffer.setBigEndian16(5); /* unknown */
		buffer.setBigEndian16(0x14);
		buffer.set8(0x7c);
		buffer.setBigEndian16(1);

		buffer.setBigEndian16(length | 0x8000); // remaining length

		buffer.setBigEndian16(8); // length?
		buffer.setBigEndian16(16);
		buffer.set8(0);
		buffer.setLittleEndian16(0xc001);
		buffer.set8(0);

		buffer.setLittleEndian32(0x61637544); // "Duca" ?!
		buffer.setBigEndian16(length - 14 | 0x8000); // remaining length

		// Client information
		buffer.setLittleEndian16(SEC_TAG_CLI_INFO);
		buffer.setLittleEndian16(session.getOptions().isUse_rdp5() ? 212 : 136); // length
		buffer.setLittleEndian16(session.getOptions().isUse_rdp5() ? 4 : 1);
		buffer.setLittleEndian16(8);
		logger.debug("Options.width: {}", session.getOptions().getWidth());
		buffer.setLittleEndian16(session.getOptions().getWidth());
		logger.debug("Options.height: {}", session.getOptions().getHeight());
		buffer.setLittleEndian16(session.getOptions().getHeight());
		buffer.setLittleEndian16(0xca01);
		buffer.setLittleEndian16(0xaa03);
		logger.debug("Options.keylayout: {}", session.getOptions().getKeylayout());
		buffer.setLittleEndian32(session.getOptions().getKeylayout());
		buffer.setLittleEndian32(session.getOptions().isUse_rdp5() ? 2600 : 419); // or
																					// 0ece
																					// //
		// client
		// build? we
		// are 2600
		// compatible
		// :-)

		/* Unicode name of client, padded to 32 bytes */
		buffer.outUnicodeString(hostname.toUpperCase(), hostlen);
		buffer.incrementPosition(30 - hostlen);

		buffer.setLittleEndian32(4);
		buffer.setLittleEndian32(0);
		buffer.setLittleEndian32(12);
		buffer.incrementPosition(64); /* reserved? 4 + 12 doublewords */

		buffer.setLittleEndian16(0xca01); // out_uint16_le(s, 0xca01);
		buffer.setLittleEndian16(session.getOptions().isUse_rdp5() ? 1 : 0);

		if (session.getOptions().isUse_rdp5()) {
			buffer.setLittleEndian32(0); // out_uint32(s, 0);
			logger.debug("Options.server_bpp: {}", session.getOptions().getServer_bpp());
			buffer.set8(session.getOptions().getServer_bpp()); // out_uint8(s,
																// g_server_bpp);
			buffer.setLittleEndian16(0x0700); // out_uint16_le(s, 0x0700);
			buffer.set8(0); // out_uint8(s, 0);
			buffer.setLittleEndian32(1); // out_uint32_le(s, 1);

			buffer.incrementPosition(64);

			buffer.setLittleEndian16(SEC_TAG_CLI_4); // out_uint16_le(s,
														// SEC_TAG_CLI_4);
			buffer.setLittleEndian16(12); // out_uint16_le(s, 12);
			logger.debug("Options.console_session: {}", session.getOptions().isConsole_session());
			buffer.setLittleEndian32(session.getOptions().isConsole_session() ? 0xb : 0xd); // out_uint32_le(s,
			// g_console_session
			// ?
			// 0xb
			// :
			// 9);
			buffer.setLittleEndian32(0); // out_uint32(s, 0);
		}

		// Client encryption settings //
		buffer.setLittleEndian16(SEC_TAG_CLI_CRYPT);
		buffer.setLittleEndian16(session.getOptions().isUse_rdp5() ? 12 : 8); // length

		// if(Options.use_rdp5) buffer.setLittleEndian32(Options.encryption ?
		// 0x1b : 0); // 128-bit encryption supported
		// else
		logger.debug("Options.encryption: {}", session.getOptions().isEncryption());
		buffer.setLittleEndian32(session.getOptions().isEncryption() ? (session.getOptions().isConsole_session() ? 0xb : 0x3)
				: 0);

		if (session.getOptions().isUse_rdp5())
			buffer.setLittleEndian32(0); // unknown

		if (session.getOptions().isUse_rdp5() && (session.getChannels().num_channels() > 0)) {
			logger.debug(("num_channels is " + session.getChannels().num_channels()));
			buffer.setLittleEndian16(SEC_TAG_CLI_CHANNELS); // out_uint16_le(s,
															// SEC_TAG_CLI_CHANNELS);
			buffer.setLittleEndian16(session.getChannels().num_channels() * 12 + 8); // out_uint16_le(s,
			// g_num_channels
			// * 12
			// + 8);
			// //
			// length
			buffer.setLittleEndian32(session.getChannels().num_channels()); // out_uint32_le(s,
			// g_num_channels);
			// // number of
			// virtual
			// channels
			for (int i = 0; i < session.getChannels().num_channels(); i++) {
				logger.debug(("Requesting channel " + session.getChannels().channel(i).name()));
				buffer.out_uint8p(session.getChannels().channel(i).name(), 8); // out_uint8a(s,
				// g_channels[i].name,
				// 8);
				buffer.setBigEndian32(session.getChannels().channel(i).flags()); // out_uint32_be(s,
				// g_channels[i].flags);
			}
		}
		buffer.markEnd();
		return buffer;
	}
	
	/**
	 * Receive a Secure layer PDU from the MCS layer
	 * 
	 * @return Packet representing received Secure PDU
	 * @throws Exception
	 */
	// public static RdpPacket_Localised receive(ChannelBuffer channelBuffer,
	// RDPSession session, ChannelHandlerContext ctx)
	// throws Exception {
	// int sec_flags = 0;
	// RdpPacket_Localised buffer = null;
	// logger.debug("channelBuffer readableBytes:{}",
	// channelBuffer.readableBytes());
	//
	// while (true) {
	// int[] channel = new int[1];
	// buffer = MCSUtils.receive(ctx, channel, channelBuffer, session);
	// if (buffer == null) {
	// return null;
	// }
	// buffer.setHeader(RdpPacket_Localised.SECURE_HEADER);
	// if (Constants.encryption || (!session.isLicenceIssued())) {
	//
	// sec_flags = buffer.getLittleEndian32();
	// logger.debug("sec_flags:{}", sec_flags);
	// logger.debug("sec_flags & SEC_LICENCE_NEG: {}", sec_flags &
	// SEC_LICENCE_NEG);
	// if ((sec_flags & SEC_LICENCE_NEG) != 0) {
	// session.getLicence().process(buffer, ctx);
	// logger.debug("continue loop");
	// continue;
	// }
	// logger.debug("sec_flags & SEC_ENCRYPT: {}", sec_flags & SEC_ENCRYPT);
	// if ((sec_flags & SEC_ENCRYPT) != 0) {
	// buffer.incrementPosition(8); // signature
	// byte[] data = new byte[buffer.size() - buffer.getPosition()];
	// buffer.copyToByteArray(data, 0, buffer.getPosition(), data.length);
	// byte[] packet = decrypt(data, session);
	//
	// buffer.copyFromByteArray(packet, 0, buffer.getPosition(), packet.length);
	//
	// // buffer.setStart(buffer.getPosition());
	// // return buffer;
	// }
	// }
	//
	// logger.debug("channel[0]: {}", channel[0]);
	// if (channel[0] != MCSUtils.MCS_GLOBAL_CHANNEL) {
	// session.getChannels().channel_process(buffer, channel[0], session, ctx);
	// logger.debug("continue loop2");
	// continue;
	// }
	//
	// buffer.setStart(buffer.getPosition());
	// return buffer;
	// }
	// }

	/**
	 * Decrypt provided data using RC4 algorithm
	 * 
	 * @param data
	 *            Data to decrypt
	 * @return Decrypted data
	 * @throws CryptoException
	 */
	public static byte[] decrypt(byte[] data, RDPSession session) throws CryptoException {
		byte[] buffer = null;
		if (session.getDec_count() == 4096) {
			session.setSec_decrypt_key(update(session.getSec_decrypt_key(), session.getSec_decrypt_update_key(), session));
			byte[] key = new byte[session.getKeylength()];
			System.arraycopy(session.getSec_decrypt_key(), 0, key, 0, session.getKeylength());
			session.getRc4_dec().engineInitDecrypt(key);
			// logger.debug("Packet dec_count="+dec_count);
			session.setDec_count(0);
		}
		// this.rc4.engineInitDecrypt(this.rc4_decrypt_key);

		buffer = session.getRc4_dec().crypt(data);
		session.setDec_count(session.getDec_count() + 1);
		return buffer;
	}

	/**
	 * @return MCS user ID
	 */
	public static int getUserID(RDPSession session) {
		return MCSUtils.getUserID(session);
	}
}
