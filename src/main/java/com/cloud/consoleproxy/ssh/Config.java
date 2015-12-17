package com.cloud.consoleproxy.ssh;

import com.jcraft.jsch.JSch;

public class Config {

	public static final String VERSION = "0.1.52";

	static java.util.Hashtable<String, String> config = new java.util.Hashtable<String, String>();
	static {
		// config.put("kex", "diffie-hellman-group-exchange-sha1");
		config.put(
				"kex",
				"diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521");
		config.put("server_host_key", "ssh-rsa,ssh-dss,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521");
		// config.put("server_host_key", "ssh-dss,ssh-rsa");

		config.put("cipher.s2c",
				"aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc");
		config.put("cipher.c2s",
				"aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc");

		config.put("mac.s2c", "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96");
		config.put("mac.c2s", "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96");
		config.put("compression.s2c", "none");
		// config.put("compression.s2c", "zlib@openssh.com,zlib,none");
		config.put("compression.c2s", "none");
		// config.put("compression.c2s", "zlib@openssh.com,zlib,none");

		config.put("lang.s2c", "");
		config.put("lang.c2s", "");

		config.put("compression_level", "6");

		config.put("diffie-hellman-group-exchange-sha1", "com.cloud.consoleproxy.ssh.DHGEX");
		config.put("diffie-hellman-group1-sha1", "com.cloud.consoleproxy.ssh.DHG1");
		config.put("diffie-hellman-group14-sha1", "com.jcraft.jsch.DHG14");
		config.put("diffie-hellman-group-exchange-sha256", "com.jcraft.jsch.DHGEX256"); // avaibale
																						// since
																						// JDK1.4.2.

		config.put("ecdsa-sha2-nistp256", "com.jcraft.jsch.jce.SignatureECDSA");
		config.put("ecdsa-sha2-nistp384", "com.jcraft.jsch.jce.SignatureECDSA");
		config.put("ecdsa-sha2-nistp521", "com.jcraft.jsch.jce.SignatureECDSA");

		config.put("ecdh-sha2-nistp256", "com.jcraft.jsch.DHEC256");
		config.put("ecdh-sha2-nistp384", "com.jcraft.jsch.DHEC384");
		config.put("ecdh-sha2-nistp521", "com.jcraft.jsch.DHEC521");

		config.put("ecdh-sha2-nistp", "com.jcraft.jsch.jce.ECDHN");

		config.put("dh", "com.cloud.consoleproxy.ssh.DH");
		config.put("3des-cbc", "com.jcraft.jsch.jce.TripleDESCBC");
		config.put("blowfish-cbc", "com.jcraft.jsch.jce.BlowfishCBC");
		config.put("hmac-sha1", "com.jcraft.jsch.jce.HMACSHA1");
		config.put("hmac-sha1-96", "com.jcraft.jsch.jce.HMACSHA196");
		config.put("hmac-sha2-256", "com.jcraft.jsch.jce.HMACSHA256");

		config.put("hmac-md5", "com.jcraft.jsch.jce.HMACMD5");
		config.put("hmac-md5-96", "com.jcraft.jsch.jce.HMACMD596");
		config.put("sha-1", "com.jcraft.jsch.jce.SHA1");
		config.put("sha-256", "com.jcraft.jsch.jce.SHA256");
		config.put("sha-384", "com.jcraft.jsch.jce.SHA384");
		config.put("sha-512", "com.jcraft.jsch.jce.SHA512");
		config.put("md5", "com.jcraft.jsch.jce.MD5");
		config.put("signature.dss", "com.jcraft.jsch.jce.SignatureDSA");
		config.put("signature.rsa", "com.jcraft.jsch.jce.SignatureRSA");
		config.put("signature.ecdsa", "com.jcraft.jsch.jce.SignatureECDSA");
		config.put("keypairgen.dsa", "com.jcraft.jsch.jce.KeyPairGenDSA");
		config.put("keypairgen.rsa", "com.jcraft.jsch.jce.KeyPairGenRSA");
		config.put("keypairgen.ecdsa", "com.jcraft.jsch.jce.KeyPairGenECDSA");
		config.put("random", "com.jcraft.jsch.jce.Random");

		config.put("none", "com.jcraft.jsch.CipherNone");

		config.put("aes128-cbc", "com.jcraft.jsch.jce.AES128CBC");
		config.put("aes192-cbc", "com.jcraft.jsch.jce.AES192CBC");
		config.put("aes256-cbc", "com.jcraft.jsch.jce.AES256CBC");

		config.put("aes128-ctr", "com.jcraft.jsch.jce.AES128CTR");
		config.put("aes192-ctr", "com.jcraft.jsch.jce.AES192CTR");
		config.put("aes256-ctr", "com.jcraft.jsch.jce.AES256CTR");
		config.put("3des-ctr", "com.jcraft.jsch.jce.TripleDESCTR");
		config.put("arcfour", "com.jcraft.jsch.jce.ARCFOUR");
		config.put("arcfour128", "com.jcraft.jsch.jce.ARCFOUR128");
		config.put("arcfour256", "com.jcraft.jsch.jce.ARCFOUR256");

		config.put("userauth.none", "com.cloud.consoleproxy.ssh.UserAuthNone");
		config.put("userauth.password", "com.cloud.consoleproxy.ssh.UserAuthPassword");
		config.put("userauth.keyboard-interactive", "com.cloud.consoleproxy.ssh.UserAuthKeyboardInteractive");
		config.put("userauth.publickey", "com.cloud.consoleproxy.ssh.UserAuthPublicKey");
		config.put("userauth.gssapi-with-mic", "com.jcraft.jsch.UserAuthGSSAPIWithMIC");
		config.put("gssapi-with-mic.krb5", "com.jcraft.jsch.jgss.GSSContextKrb5");

		config.put("zlib", "com.jcraft.jsch.jcraft.Compression");
		config.put("zlib@openssh.com", "com.jcraft.jsch.jcraft.Compression");

		config.put("pbkdf", "com.jcraft.jsch.jce.PBKDF");

		config.put("StrictHostKeyChecking", "ask");
		config.put("HashKnownHosts", "no");
		// config.put("HashKnownHosts", "yes");
		config.put("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive,password");

		config.put("CheckCiphers",
				"aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc,3des-ctr,arcfour,arcfour128,arcfour256");
		config.put("CheckKexes", "diffie-hellman-group14-sha1,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521");
		config.put("CheckSignatures", "ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521");

		config.put("MaxAuthTries", "6");
		config.put("ClearAllForwardings", "no");
	}

	public static final int SSH_MSG_DISCONNECT = 1;
	public static final int SSH_MSG_IGNORE = 2;
	public static final int SSH_MSG_UNIMPLEMENTED = 3;
	public static final int SSH_MSG_DEBUG = 4;
	public static final int SSH_MSG_SERVICE_REQUEST = 5;
	public static final int SSH_MSG_SERVICE_ACCEPT = 6;
	public static final int SSH_MSG_KEXINIT = 20;
	public static final int SSH_MSG_NEWKEYS = 21;
	public static final int SSH_MSG_KEXDH_INIT = 30;
	public static final int SSH_MSG_KEXDH_REPLY = 31;
	public static final int SSH_MSG_KEX_DH_GEX_GROUP = 31;
	public static final int SSH_MSG_KEX_DH_GEX_INIT = 32;
	public static final int SSH_MSG_KEX_DH_GEX_REPLY = 33;
	public static final int SSH_MSG_KEX_DH_GEX_REQUEST = 34;
	public static final int SSH_MSG_GLOBAL_REQUEST = 80;
	public static final int SSH_MSG_REQUEST_SUCCESS = 81;
	public static final int SSH_MSG_REQUEST_FAILURE = 82;
	public static final int SSH_MSG_CHANNEL_OPEN = 90;
	public static final int SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
	public static final int SSH_MSG_CHANNEL_OPEN_FAILURE = 92;
	public static final int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;
	public static final int SSH_MSG_CHANNEL_DATA = 94;
	public static final int SSH_MSG_CHANNEL_EXTENDED_DATA = 95;
	public static final int SSH_MSG_CHANNEL_EOF = 96;
	public static final int SSH_MSG_CHANNEL_CLOSE = 97;
	public static final int SSH_MSG_CHANNEL_REQUEST = 98;
	public static final int SSH_MSG_CHANNEL_SUCCESS = 99;
	public static final int SSH_MSG_CHANNEL_FAILURE = 100;

	public static final int JSCH_INIT = 0;
	public static final int JSCH_CHECKHOST = 1;
	public static final int JSCH_NEWKEYS = 2;


	public static String getConfig(String key) {
		Object foo = null;
		if (config != null) {
			foo = config.get(key);
			if (foo instanceof String) {
				return (String) foo;
			}
		}
		foo = JSch.getConfig(key);
		if (foo instanceof String)
			return (String) foo;
		return null;
	}

}
