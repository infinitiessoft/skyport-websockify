package com.jcraft.jsch;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.vnc.util.SSHUtil;

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
 Copyright (c) 2002-2015 ymnk, JCraft,Inc. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright 
 notice, this list of conditions and the following disclaimer in 
 the documentation and/or other materials provided with the distribution.

 3. The names of the authors may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
 INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public abstract class CustomKeyExchange {

	private final static Logger logger = LoggerFactory.getLogger(CustomKeyExchange.class);
	public static final int PROPOSAL_KEX_ALGS = 0;
	static final int PROPOSAL_SERVER_HOST_KEY_ALGS = 1;
	public static final int PROPOSAL_ENC_ALGS_CTOS = 2;
	public static final int PROPOSAL_ENC_ALGS_STOC = 3;
	public static final int PROPOSAL_MAC_ALGS_CTOS = 4;
	public static final int PROPOSAL_MAC_ALGS_STOC = 5;
	public static final int PROPOSAL_COMP_ALGS_CTOS = 6;
	public static final int PROPOSAL_COMP_ALGS_STOC = 7;
	public static final int PROPOSAL_LANG_CTOS = 8;
	public static final int PROPOSAL_LANG_STOC = 9;
	public static final int PROPOSAL_MAX = 10;

	// static String kex_algs="diffie-hellman-group-exchange-sha1"+
	// ",diffie-hellman-group1-sha1";

	// static String kex="diffie-hellman-group-exchange-sha1";
	static String kex = "diffie-hellman-group1-sha1";
	static String server_host_key = "ssh-rsa,ssh-dss";
	static String enc_c2s = "blowfish-cbc";
	static String enc_s2c = "blowfish-cbc";
	static String mac_c2s = "hmac-md5"; // hmac-md5,hmac-sha1,hmac-ripemd160,
										// hmac-sha1-96,hmac-md5-96
	static String mac_s2c = "hmac-md5";
	// static String comp_c2s="none"; // zlib
	// static String comp_s2c="none";
	static String lang_c2s = "";
	static String lang_s2c = "";

	public static final int STATE_END = 0;

	protected HASH sha = null;
	protected byte[] K = null;
	protected byte[] H = null;
	protected byte[] K_S = null;


	public abstract void init(CustomSession session, ChannelHandlerContext context, ChannelEvent evt, byte[] V_S,
			byte[] V_C, byte[] I_S, byte[] I_C) throws Exception;

	public abstract boolean next(CustomBuffer buffer) throws Exception;

	public abstract int getState();


	protected final int RSA = 0;
	protected final int DSS = 1;
	protected final int ECDSA = 2;
	private int type = 0;
	protected String key_alg_name = "";


	public String getKeyType() {
		if (type == DSS)
			return "DSA";
		if (type == RSA)
			return "RSA";
		return "ECDSA";
	}

	public String getKeyAlgorithName() {
		return key_alg_name;
	}

	public static String[] guess(byte[] I_S, byte[] I_C) {
		String[] guess = new String[PROPOSAL_MAX];
		CustomBuffer sb = new CustomBuffer(I_S);
		sb.setOffSet(17);
		CustomBuffer cb = new CustomBuffer(I_C);
		cb.setOffSet(17);

		for (int i = 0; i < PROPOSAL_MAX; i++) {
			logger.info("kex: server: {}", SSHUtil.byte2str(sb.getString()));
		}
		for (int i = 0; i < PROPOSAL_MAX; i++) {
			logger.info("kex: client: {}", SSHUtil.byte2str(cb.getString()));
		}
		sb.setOffSet(17);
		cb.setOffSet(17);

		for (int i = 0; i < PROPOSAL_MAX; i++) {
			byte[] sp = sb.getString(); // server proposal
			byte[] cp = cb.getString(); // client proposal
			int j = 0;
			int k = 0;

			loop: while (j < cp.length) {
				while (j < cp.length && cp[j] != ',')
					j++;
				if (k == j)
					return null;
				String algorithm = SSHUtil.byte2str(cp, k, j - k);
				int l = 0;
				int m = 0;
				while (l < sp.length) {
					while (l < sp.length && sp[l] != ',')
						l++;
					if (m == l)
						return null;
					if (algorithm.equals(SSHUtil.byte2str(sp, m, l - m))) {
						guess[i] = algorithm;
						break loop;
					}
					l++;
					m = l;
				}
				j++;
				k = j;
			}
			if (j == 0) {
				guess[i] = "";
			} else if (guess[i] == null) {
				return null;
			}
		}

		logger.info("kex: server->client {} {} {}", new Object[] { guess[PROPOSAL_ENC_ALGS_STOC],
				guess[PROPOSAL_MAC_ALGS_STOC], guess[PROPOSAL_COMP_ALGS_STOC] });
		logger.info("kex: client->server {} {} {}", new Object[] { guess[PROPOSAL_ENC_ALGS_CTOS],
				guess[PROPOSAL_MAC_ALGS_CTOS], guess[PROPOSAL_COMP_ALGS_CTOS] });

		return guess;
	}

	public String getFingerPrint() {
		HASH hash = null;
		try {
			Class<?> c = Class.forName(JSch.getConfig("md5"));
			hash = (HASH) (c.newInstance());
		} catch (Exception e) {
			logger.error("getFingerPrint failed", e);
		}
		return SSHUtil.getFingerPrint(hash, getHostKey());
	}

	byte[] getK() {
		return K;
	}

	byte[] getH() {
		return H;
	}

	HASH getHash() {
		return sha;
	}

	public byte[] getHostKey() {
		return K_S;
	}

	/*
	 * It seems JCE included in Oracle's Java7u6(and later) has suddenly changed
	 * its behavior. The secrete generated by KeyAgreement#generateSecret() may
	 * start with 0, even if it is a positive value.
	 */
	protected byte[] normalize(byte[] secret) {
		if (secret.length > 1 && secret[0] == 0 && (secret[1] & 0x80) == 0) {
			byte[] tmp = new byte[secret.length - 1];
			System.arraycopy(secret, 1, tmp, 0, tmp.length);
			return normalize(tmp);
		} else {
			return secret;
		}
	}

	protected boolean verify(String alg, byte[] K_S, int index, byte[] sig_of_H) throws Exception {
		int i, j;

		i = index;
		boolean result = false;

		if (alg.equals("ssh-rsa")) {
			byte[] tmp;
			byte[] ee;
			byte[] n;

			type = RSA;
			key_alg_name = alg;

			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			ee = tmp;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			n = tmp;

			SignatureRSA sig = null;
			try {
				Class<?> c = Class.forName(JSch.getConfig("signature.rsa"));
				sig = (SignatureRSA) (c.newInstance());
				sig.init();
			} catch (Exception e) {
				logger.error("sign failed", e);
			}
			sig.setPubKey(ee, n);
			sig.update(H);
			result = sig.verify(sig_of_H);
			logger.info("ssh_rsa_verify: signature {}", result);
		} else if (alg.equals("ssh-dss")) {
			byte[] q = null;
			byte[] tmp;
			byte[] p;
			byte[] g;
			byte[] f;

			type = DSS;
			key_alg_name = alg;

			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			p = tmp;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			q = tmp;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			g = tmp;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			f = tmp;

			SignatureDSA sig = null;
			try {
				Class<?> c = Class.forName(JSch.getConfig("signature.dss"));
				sig = (SignatureDSA) (c.newInstance());
				sig.init();
			} catch (Exception e) {
				logger.error("sign failed", e);
			}
			sig.setPubKey(f, p, q, g);
			sig.update(H);
			result = sig.verify(sig_of_H);

			logger.info("ssh_dss_verify: signature {}", result);
		} else if (alg.equals("ecdsa-sha2-nistp256") || alg.equals("ecdsa-sha2-nistp384")
				|| alg.equals("ecdsa-sha2-nistp521")) {
			byte[] tmp;
			byte[] r;
			byte[] s;

			// RFC 5656,
			type = ECDSA;
			key_alg_name = alg;

			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			tmp = new byte[j];
			System.arraycopy(K_S, i, tmp, 0, j);
			i += j;
			j = ((K_S[i++] << 24) & 0xff000000) | ((K_S[i++] << 16) & 0x00ff0000) | ((K_S[i++] << 8) & 0x0000ff00)
					| ((K_S[i++]) & 0x000000ff);
			i++;
			tmp = new byte[(j - 1) / 2];
			System.arraycopy(K_S, i, tmp, 0, tmp.length);
			i += (j - 1) / 2;
			r = tmp;
			tmp = new byte[(j - 1) / 2];
			System.arraycopy(K_S, i, tmp, 0, tmp.length);
			i += (j - 1) / 2;
			s = tmp;

			SignatureECDSA sig = null;
			try {
				Class<?> c = Class.forName(JSch.getConfig("signature.ecdsa"));
				sig = (SignatureECDSA) (c.newInstance());
				sig.init();
			} catch (Exception e) {
				logger.error("sign failed", e);
			}

			sig.setPubKey(r, s);

			sig.update(H);

			result = sig.verify(sig_of_H);
		} else {
			logger.error("unknown algo");
		}

		return result;
	}

}
