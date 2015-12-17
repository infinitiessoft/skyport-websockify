package com.jcraft.jsch;

import com.cloud.consoleproxy.ssh.Config;

public class CustomKeyPairDSA extends CustomKeyPair {

	private byte[] P_array;
	private byte[] Q_array;
	private byte[] G_array;
	private byte[] pub_array;
	private byte[] prv_array;

	// private int key_size=0;
	private int key_size = 1024;


	public CustomKeyPairDSA() {
		this(null, null, null, null, null);
	}

	public CustomKeyPairDSA(byte[] P_array, byte[] Q_array, byte[] G_array, byte[] pub_array, byte[] prv_array) {
		super();
		this.P_array = P_array;
		this.Q_array = Q_array;
		this.G_array = G_array;
		this.pub_array = pub_array;
		this.prv_array = prv_array;
		if (P_array != null)
			key_size = (new java.math.BigInteger(P_array)).bitLength();
	}

	@Override
	void generate(int key_size) throws JSchException {
		this.key_size = key_size;
		try {
			Class<?> c = Class.forName(Config.getConfig("keypairgen.dsa"));
			KeyPairGenDSA keypairgen = (KeyPairGenDSA) (c.newInstance());
			keypairgen.init(key_size);
			P_array = keypairgen.getP();
			Q_array = keypairgen.getQ();
			G_array = keypairgen.getG();
			pub_array = keypairgen.getY();
			prv_array = keypairgen.getX();

			keypairgen = null;
		} catch (Exception e) {
			// System.err.println("KeyPairDSA: "+e);
			if (e instanceof Throwable)
				throw new JSchException(e.toString(), e);
			throw new JSchException(e.toString());
		}
	}


	private static final byte[] begin = Util.str2byte("-----BEGIN DSA PRIVATE KEY-----");
	private static final byte[] end = Util.str2byte("-----END DSA PRIVATE KEY-----");


	@Override
	byte[] getBegin() {
		return begin;
	}

	@Override
	byte[] getEnd() {
		return end;
	}

	@Override
	byte[] getPrivateKey() {
		int content = 1 + countLength(1) + 1 + // INTEGER
				1 + countLength(P_array.length) + P_array.length + // INTEGER P
				1 + countLength(Q_array.length) + Q_array.length + // INTEGER Q
				1 + countLength(G_array.length) + G_array.length + // INTEGER G
				1 + countLength(pub_array.length) + pub_array.length + // INTEGER
																		// pub
				1 + countLength(prv_array.length) + prv_array.length; // INTEGER
																		// prv

		int total = 1 + countLength(content) + content; // SEQUENCE

		byte[] plain = new byte[total];
		int index = 0;
		index = writeSEQUENCE(plain, index, content);
		index = writeINTEGER(plain, index, new byte[1]); // 0
		index = writeINTEGER(plain, index, P_array);
		index = writeINTEGER(plain, index, Q_array);
		index = writeINTEGER(plain, index, G_array);
		index = writeINTEGER(plain, index, pub_array);
		index = writeINTEGER(plain, index, prv_array);
		return plain;
	}

	@Override
	boolean parse(byte[] plain) {
		try {

			if (vendor == VENDOR_FSECURE) {
				if (plain[0] != 0x30) { // FSecure
					Buffer buf = new Buffer(plain);
					buf.getInt();
					P_array = buf.getMPIntBits();
					G_array = buf.getMPIntBits();
					Q_array = buf.getMPIntBits();
					pub_array = buf.getMPIntBits();
					prv_array = buf.getMPIntBits();
					if (P_array != null)
						key_size = (new java.math.BigInteger(P_array)).bitLength();
					return true;
				}
				return false;
			} else if (vendor == VENDOR_PUTTY) {
				Buffer buf = new Buffer(plain);
				buf.skip(plain.length);

				try {
					byte[][] tmp = buf.getBytes(1, "");
					prv_array = tmp[0];
				} catch (JSchException e) {
					return false;
				}

				return true;
			}

			int index = 0;
			int length = 0;

			if (plain[index] != 0x30)
				return false;
			index++; // SEQUENCE
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}

			if (plain[index] != 0x02)
				return false;
			index++; // INTEGER
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			index += length;

			index++;
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			P_array = new byte[length];
			System.arraycopy(plain, index, P_array, 0, length);
			index += length;

			index++;
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			Q_array = new byte[length];
			System.arraycopy(plain, index, Q_array, 0, length);
			index += length;

			index++;
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			G_array = new byte[length];
			System.arraycopy(plain, index, G_array, 0, length);
			index += length;

			index++;
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			pub_array = new byte[length];
			System.arraycopy(plain, index, pub_array, 0, length);
			index += length;

			index++;
			length = plain[index++] & 0xff;
			if ((length & 0x80) != 0) {
				int foo = length & 0x7f;
				length = 0;
				while (foo-- > 0) {
					length = (length << 8) + (plain[index++] & 0xff);
				}
			}
			prv_array = new byte[length];
			System.arraycopy(plain, index, prv_array, 0, length);
			index += length;

			if (P_array != null)
				key_size = (new java.math.BigInteger(P_array)).bitLength();
		} catch (Exception e) {
			// System.err.println(e);
			// e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public byte[] getPublicKeyBlob() {
		byte[] foo = super.getPublicKeyBlob();
		if (foo != null)
			return foo;

		if (P_array == null)
			return null;
		byte[][] tmp = new byte[5][];
		tmp[0] = sshdss;
		tmp[1] = P_array;
		tmp[2] = Q_array;
		tmp[3] = G_array;
		tmp[4] = pub_array;
		return Buffer.fromBytes(tmp).buffer;
	}


	private static final byte[] sshdss = Util.str2byte("ssh-dss");


	@Override
	byte[] getKeyTypeName() {
		return sshdss;
	}

	@Override
	public int getKeyType() {
		return DSA;
	}

	@Override
	public int getKeySize() {
		return key_size;
	}

	@Override
	public byte[] getSignature(byte[] data) {
		try {
			Class<?> c = Class.forName(Config.getConfig("signature.dss"));
			SignatureDSA dsa = (SignatureDSA) (c.newInstance());
			dsa.init();
			dsa.setPrvKey(prv_array, P_array, Q_array, G_array);

			dsa.update(data);
			byte[] sig = dsa.sign();
			byte[][] tmp = new byte[2][];
			tmp[0] = sshdss;
			tmp[1] = sig;
			return Buffer.fromBytes(tmp).buffer;
		} catch (Exception e) {
			// System.err.println("e "+e);
		}
		return null;
	}

	@Override
	public Signature getVerifier() {
		try {
			Class<?> c = Class.forName(Config.getConfig("signature.dss"));
			SignatureDSA dsa = (SignatureDSA) (c.newInstance());
			dsa.init();

			if (pub_array == null && P_array == null && getPublicKeyBlob() != null) {
				Buffer buf = new Buffer(getPublicKeyBlob());
				buf.getString();
				P_array = buf.getString();
				Q_array = buf.getString();
				G_array = buf.getString();
				pub_array = buf.getString();
			}

			dsa.setPubKey(pub_array, P_array, Q_array, G_array);
			return dsa;
		} catch (Exception e) {
			// System.err.println("e "+e);
		}
		return null;
	}

	static CustomKeyPair fromSSHAgent(Buffer buf) throws JSchException {

		byte[][] tmp = buf.getBytes(7, "invalid key format");

		byte[] P_array = tmp[1];
		byte[] Q_array = tmp[2];
		byte[] G_array = tmp[3];
		byte[] pub_array = tmp[4];
		byte[] prv_array = tmp[5];
		CustomKeyPairDSA kpair = new CustomKeyPairDSA(P_array, Q_array, G_array, pub_array, prv_array);
		kpair.publicKeyComment = new String(tmp[6]);
		kpair.vendor = VENDOR_OPENSSH;
		return kpair;
	}

	@Override
	public byte[] forSSHAgent() throws JSchException {
		if (isEncrypted()) {
			throw new JSchException("key is encrypted.");
		}
		Buffer buf = new Buffer();
		buf.putString(sshdss);
		buf.putString(P_array);
		buf.putString(Q_array);
		buf.putString(G_array);
		buf.putString(pub_array);
		buf.putString(prv_array);
		buf.putString(Util.str2byte(publicKeyComment));
		byte[] result = new byte[buf.getLength()];
		buf.getByte(result, 0, result.length);
		return result;
	}

	@Override
	public void dispose() {
		super.dispose();
		Util.bzero(prv_array);
	}
}
