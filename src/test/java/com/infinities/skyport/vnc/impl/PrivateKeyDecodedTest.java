package com.infinities.skyport.vnc.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Test;

import com.jcraft.jsch.CustomKeyPair;
import com.jcraft.jsch.CustomSession;
import com.jcraft.jsch.JSchException;

public class PrivateKeyDecodedTest {

	@Test
	public void testDecoded() throws NoSuchAlgorithmException, JSchException, InvalidKeySpecException, IOException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keys = keyGen.genKeyPair();
		StringWriter writer = new StringWriter();
		JcaPEMWriter pemWriter = null;
		try {
			pemWriter = new JcaPEMWriter(writer);
			pemWriter.writeObject(keys.getPrivate());
		} finally {
			if (pemWriter != null) {
				pemWriter.close();
			}
		}

		String privateKeyStr = writer.toString();
		System.out.println(privateKeyStr);
		CustomSession session = new CustomSession("pohsun", "127.0.0.1", 22, "password");
		session.addIdentity("remote", privateKeyStr.getBytes(), null, "");
		// com.jcraft.jsch.KeyPair.load(null,
		// privateKeyStr.getBytes("US-ASCII"), null);
		// PKCS8EncodedKeySpec privspec = new
		// PKCS8EncodedKeySpec(BaseEncoding.base64().decode(base64));
		// KeyFactory factory = KeyFactory.getInstance("RSA");
		// PrivateKey privkey = factory.generatePrivate(privspec);
		// byte[] decoded = BaseEncoding.base64().decode(base64);
		// CustomSession session = new CustomSession("pohsun", "127.0.0.1", 22);
		// session.addIdentity("remote", decoded.getBytes(), null, "");

	}

	@Test
	public void testKeyGenerated() throws JSchException {
		CustomKeyPair kpair = CustomKeyPair.genKeyPair(2, 2048);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		kpair.writePrivateKey(out);
		byte[] bs = out.toByteArray();
		System.err.println(new String(bs));
		System.err.println(bs.length);
		CustomSession session = new CustomSession("pohsun", "127.0.0.1", 22, "password");
		session.addIdentity("remote", bs, null, "");

	}
}
