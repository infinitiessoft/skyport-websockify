/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.rdp.custom;

//import java.awt.Cursor;

import com.lixia.rdp.Cache;
import com.lixia.rdp.Options;
import com.lixia.rdp.PstCache;
import com.lixia.rdp.RdpPacket_Localised;
import com.lixia.rdp.crypto.BlockMessageDigest;
import com.lixia.rdp.crypto.MD5;
import com.lixia.rdp.crypto.RC4;
import com.lixia.rdp.crypto.SHA1;

public class RDPSession {

	private static final int SEC_RANDOM_SIZE = 32;
	private static final int SEC_MODULUS_SIZE = 64;
	private int McsUserID;
	// private int server_rdp_version;
	// private boolean use_rdp5 = true;
	private boolean readCert = false;
	private byte[] server_random;
	private byte[] sec_sign_key = null;
	private byte[] sec_decrypt_key = null;
	private byte[] sec_encrypt_key = null;
	private byte[] sec_decrypt_update_key = null;
	private byte[] sec_encrypt_update_key = null;
	private byte[] sec_crypted_random = null;
	private byte[] exponent = null;
	private byte[] modulus = null;
	private byte[] client_random = new byte[SEC_RANDOM_SIZE];
	private int keylength = 0;
	private BlockMessageDigest sha1 = null;
	private BlockMessageDigest md5 = null;
	private RC4 rc4_enc = null;
	private RC4 rc4_dec = null;
	private RC4 rc4_update = null;
	public int server_public_key_len = SEC_MODULUS_SIZE;
	private boolean licenceIssued = false;
	private final byte[] pad_54 = { 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
			54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54 };

	private final byte[] pad_92 = { 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92,
			92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92, 92 };
	private int enc_count = 0;
	// setting
	// private boolean autologin = false;
	// private boolean bulk_compression = false;
	// private boolean console_audio = false;
	private CustomVChannels channels;
	// private boolean encryption = true;
	// private boolean packet_encryption = true;
	public int keylayout = 0x809;
	private int dec_count = 0;
	private int rdp_shareid = 0;
	private CustomLicense licence;
	public boolean useLockingKeyState = true;
	// private CustomRdpBufferedImageCanvas surface = null;
	protected CustomOrdersPanel orders = null;
	private Cache cache = null;
	// private Cursor g_null_cursor = null;

	private RdpPacket_Localised stream = null;
	private int next_packet;
	private PstCache pstCache;
	private Options options;


	public RDPSession(Options options) {
		sha1 = new SHA1();
		md5 = new MD5();
		rc4_dec = new RC4();
		rc4_enc = new RC4();
		rc4_update = new RC4();
		sec_sign_key = new byte[16]; // changed from 8 - rdesktop 1.2.0
		sec_decrypt_key = new byte[16];
		sec_encrypt_key = new byte[16];
		sec_decrypt_update_key = new byte[16]; // changed from 8 - rdesktop
												// 1.2.0
		sec_encrypt_update_key = new byte[16]; // changed from 8 - rdesktop
												// 1.2.0
		sec_crypted_random = new byte[64];
		// this.setSurface(surface);
		this.orders = new CustomOrdersPanel(options);
		this.pstCache = new PstCache(options, orders);
		this.cache = new Cache(pstCache);
		this.setOptions(options);
		orders.registerCache(cache);
		// orders.registerDrawingSurface(surface);
		licence = new CustomLicense(options, this);
	}

	public int getMcsUserID() {
		return McsUserID;
	}

	public void setMcsUserID(int mcsUserID) {
		McsUserID = mcsUserID;
	}

	// public int getServer_rdp_version() {
	// return server_rdp_version;
	// }
	//
	// public void setServer_rdp_version(int server_rdp_version) {
	// this.server_rdp_version = server_rdp_version;
	// }

	// public boolean isUse_rdp5() {
	// return use_rdp5;
	// }
	//
	// public void setUse_rdp5(boolean use_rdp5) {
	// this.use_rdp5 = use_rdp5;
	// }

	public boolean isReadCert() {
		return readCert;
	}

	public void setReadCert(boolean readCert) {
		this.readCert = readCert;
	}

	public byte[] getServer_random() {
		return server_random;
	}

	public void setServer_random(byte[] server_random) {
		this.server_random = server_random;
	}

	public byte[] getSec_sign_key() {
		return sec_sign_key;
	}

	public void setSec_sign_key(byte[] sec_sign_key) {
		this.sec_sign_key = sec_sign_key;
	}

	public byte[] getSec_decrypt_key() {
		return sec_decrypt_key;
	}

	public void setSec_decrypt_key(byte[] sec_decrypt_key) {
		this.sec_decrypt_key = sec_decrypt_key;
	}

	public byte[] getSec_encrypt_key() {
		return sec_encrypt_key;
	}

	public void setSec_encrypt_key(byte[] sec_encrypt_key) {
		this.sec_encrypt_key = sec_encrypt_key;
	}

	public byte[] getSec_decrypt_update_key() {
		return sec_decrypt_update_key;
	}

	public void setSec_decrypt_update_key(byte[] sec_decrypt_update_key) {
		this.sec_decrypt_update_key = sec_decrypt_update_key;
	}

	public byte[] getSec_encrypt_update_key() {
		return sec_encrypt_update_key;
	}

	public void setSec_encrypt_update_key(byte[] sec_encrypt_update_key) {
		this.sec_encrypt_update_key = sec_encrypt_update_key;
	}

	public byte[] getSec_crypted_random() {
		return sec_crypted_random;
	}

	public void setSec_crypted_random(byte[] sec_crypted_random) {
		this.sec_crypted_random = sec_crypted_random;
	}

	public byte[] getExponent() {
		return exponent;
	}

	public void setExponent(byte[] exponent) {
		this.exponent = exponent;
	}

	public byte[] getModulus() {
		return modulus;
	}

	public void setModulus(byte[] modulus) {
		this.modulus = modulus;
	}

	public byte[] getClient_random() {
		return client_random;
	}

	public void setClient_random(byte[] client_random) {
		this.client_random = client_random;
	}

	public int getKeylength() {
		return keylength;
	}

	public void setKeylength(int keylength) {
		this.keylength = keylength;
	}

	public BlockMessageDigest getSha1() {
		return sha1;
	}

	public void setSha1(BlockMessageDigest sha1) {
		this.sha1 = sha1;
	}

	public BlockMessageDigest getMd5() {
		return md5;
	}

	public void setMd5(BlockMessageDigest md5) {
		this.md5 = md5;
	}

	public RC4 getRc4_enc() {
		return rc4_enc;
	}

	public void setRc4_enc(RC4 rc4_enc) {
		this.rc4_enc = rc4_enc;
	}

	public RC4 getRc4_dec() {
		return rc4_dec;
	}

	public void setRc4_dec(RC4 rc4_dec) {
		this.rc4_dec = rc4_dec;
	}

	public RC4 getRc4_update() {
		return rc4_update;
	}

	public void setRc4_update(RC4 rc4_update) {
		this.rc4_update = rc4_update;
	}

	public int getServer_public_key_len() {
		return server_public_key_len;
	}

	public void setServer_public_key_len(int server_public_key_len) {
		this.server_public_key_len = server_public_key_len;
	}

	public boolean isLicenceIssued() {
		return licenceIssued;
	}

	public void setLicenceIssued(boolean licenceIssued) {
		this.licenceIssued = licenceIssued;
	}

	public byte[] getPad54() {
		return pad_54;
	}

	public byte[] getPad92() {
		return pad_92;
	}

	public int getEnc_count() {
		return enc_count;
	}

	public void setEnc_count(int enc_count) {
		this.enc_count = enc_count;
	}

	// public boolean isAutologin() {
	// return autologin;
	// }
	//
	// public void setAutologin(boolean autologin) {
	// this.autologin = autologin;
	// }

	// public boolean isBulk_compression() {
	// return bulk_compression;
	// }
	//
	// public void setBulk_compression(boolean bulk_compression) {
	// this.bulk_compression = bulk_compression;
	// }

	// public boolean isConsole_audio() {
	// return console_audio;
	// }
	//
	// public void setConsole_audio(boolean console_audio) {
	// this.console_audio = console_audio;
	// }

	public CustomVChannels getChannels() {
		return channels;
	}

	public void setChannels(CustomVChannels channels) {
		this.channels = channels;
	}

	// public boolean isEncryption() {
	// return encryption;
	// }
	//
	// public void setEncryption(boolean encryption) {
	// this.encryption = encryption;
	// }

	// public boolean isPacket_encryption() {
	// return packet_encryption;
	// }
	//
	// public void setPacket_encryption(boolean packet_encryption) {
	// this.packet_encryption = packet_encryption;
	// }

	public int getDec_count() {
		return dec_count;
	}

	public void setDec_count(int dec_count) {
		this.dec_count = dec_count;
	}

	public int getRdp_shareid() {
		return rdp_shareid;
	}

	public void setRdp_shareid(int rdp_shareid) {
		this.rdp_shareid = rdp_shareid;
	}

	public CustomLicense getLicence() {
		return licence;
	}

	public void setLicence(CustomLicense licence) {
		this.licence = licence;
	}

	// public int getKeylayout() {
	// return keylayout;
	// }
	//
	// public void setKeylayout(int keylayout) {
	// this.keylayout = keylayout;
	// }

	public boolean isUseLockingKeyState() {
		return useLockingKeyState;
	}

	public void setUseLockingKeyState(boolean useLockingKeyState) {
		this.useLockingKeyState = useLockingKeyState;
	}

	// public CustomRdpBufferedImageCanvas getSurface() {
	// return surface;
	// }
	//
	// public void setSurface(CustomRdpBufferedImageCanvas surface) {
	// this.surface = surface;
	// }

	public CustomOrdersPanel getOrders() {
		return orders;
	}

	public void setOrders(CustomOrdersPanel orders) {
		this.orders = orders;
	}

	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	// public Cursor getG_null_cursor() {
	// return g_null_cursor;
	// }
	//
	// public void setG_null_cursor(Cursor g_null_cursor) {
	// this.g_null_cursor = g_null_cursor;
	// }

	public RdpPacket_Localised getStream() {
		return stream;
	}

	public void setStream(RdpPacket_Localised stream) {
		this.stream = stream;
	}

	public int getNext_packet() {
		return next_packet;
	}

	public void setNext_packet(int next_packet) {
		this.next_packet = next_packet;
	}

	public Options getOptions() {
		return options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public PstCache getPstCache() {
		return pstCache;
	}

	public void setPstCache(PstCache pstCache) {
		this.pstCache = pstCache;
	}

}
