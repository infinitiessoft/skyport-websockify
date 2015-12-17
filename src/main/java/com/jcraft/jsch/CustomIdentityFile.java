package com.jcraft.jsch;

public class CustomIdentityFile implements Identity {

	private CustomKeyPair kpair;
	private String identity;


	static CustomIdentityFile newInstance(String prvfile, String pubfile) throws JSchException {
		CustomKeyPair kpair = CustomKeyPair.load(prvfile, pubfile);
		return new CustomIdentityFile(prvfile, kpair);
	}

	static CustomIdentityFile newInstance(String name, byte[] prvkey, byte[] pubkey) throws JSchException {

		CustomKeyPair kpair = CustomKeyPair.load(prvkey, pubkey);
		return new CustomIdentityFile(name, kpair);
	}

	private CustomIdentityFile(String name, CustomKeyPair kpair) throws JSchException {
		this.identity = name;
		this.kpair = kpair;
	}

	/**
	 * Decrypts this identity with the specified pass-phrase.
	 * 
	 * @param passphrase
	 *            the pass-phrase for this identity.
	 * @return <tt>true</tt> if the decryption is succeeded or this identity is
	 *         not cyphered.
	 */
	@Override
	public boolean setPassphrase(byte[] passphrase) throws JSchException {
		return kpair.decrypt(passphrase);
	}

	/**
	 * Returns the public-key blob.
	 * 
	 * @return the public-key blob
	 */
	@Override
	public byte[] getPublicKeyBlob() {
		return kpair.getPublicKeyBlob();
	}

	/**
	 * Signs on data with this identity, and returns the result.
	 * 
	 * @param data
	 *            data to be signed
	 * @return the signature
	 */
	@Override
	public byte[] getSignature(byte[] data) {
		return kpair.getSignature(data);
	}

	/**
	 * @deprecated This method should not be invoked.
	 * @see #setPassphrase(byte[] passphrase)
	 */
	@Deprecated
	@Override
	public boolean decrypt() {
		throw new RuntimeException("not implemented");
	}

	/**
	 * Returns the name of the key algorithm.
	 * 
	 * @return "ssh-rsa" or "ssh-dss"
	 */
	@Override
	public String getAlgName() {
		return new String(kpair.getKeyTypeName());
	}

	/**
	 * Returns the name of this identity. It will be useful to identify this
	 * object in the {@link IdentityRepository}.
	 */
	@Override
	public String getName() {
		return identity;
	}

	/**
	 * Returns <tt>true</tt> if this identity is cyphered.
	 * 
	 * @return <tt>true</tt> if this identity is cyphered.
	 */
	@Override
	public boolean isEncrypted() {
		return kpair.isEncrypted();
	}

	/**
	 * Disposes internally allocated data, like byte array for the private key.
	 */
	@Override
	public void clear() {
		kpair.dispose();
		kpair = null;
	}

	/**
	 * Returns an instance of {@link KeyPair} used in this {@link Identity}.
	 * 
	 * @return an instance of {@link KeyPair} used in this {@link Identity}.
	 */
	public CustomKeyPair getKeyPair() {
		return kpair;
	}
}
