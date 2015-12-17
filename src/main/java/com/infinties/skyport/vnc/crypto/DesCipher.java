package com.infinties.skyport.vnc.crypto;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DesCipher implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public byte[] encrypt(byte[] challenge, String password) {
		// VNC password consist of up to eight ASCII characters.
		try {
			SecretKey secretKey = generateKey(password);
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] response = cipher.doFinal(challenge);
			return response;
		} catch (Exception e) {
			return new byte[] {};
		}

	}

	public byte[] decrypt(byte[] encrypted, String password) {
		// VNC password consist of up to eight ASCII characters.
		try {
			SecretKey secretKey = generateKey(password);
			Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);

			byte[] response = cipher.doFinal(encrypted);
			return response;
		} catch (Exception e) {
			return new byte[] {};
		}
	}

	private SecretKey generateKey(String password) throws Exception {
		byte[] key = { 0, 0, 0, 0, 0, 0, 0, 0 }; // Padding
		byte[] passwordAsciiBytes = password.getBytes(Charset.availableCharsets().get("US-ASCII"));
		System.arraycopy(passwordAsciiBytes, 0, key, 0, Math.min(password.length(), 8));

		// Flip bytes (reverse bits) in key
		for (int i = 0; i < key.length; i++) {
			key[i] = flipByte(key[i]);
		}

		KeySpec desKeySpec = new DESKeySpec(key);
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);

		return secretKey;
	}

	private byte flipByte(byte b) {
		int b1_8 = (b & 0x1) << 7;
		int b2_7 = (b & 0x2) << 5;
		int b3_6 = (b & 0x4) << 3;
		int b4_5 = (b & 0x8) << 1;
		int b5_4 = (b & 0x10) >>> 1;
		int b6_3 = (b & 0x20) >>> 3;
		int b7_2 = (b & 0x40) >>> 5;
		int b8_1 = (b & 0x80) >>> 7;
		byte c = (byte) (b1_8 | b2_7 | b3_6 | b4_5 | b5_4 | b6_3 | b7_2 | b8_1);
		return c;
	}
}
