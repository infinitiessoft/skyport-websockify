package com.netiq.websockify;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsockifySslContext {

	private static final Logger logger = LoggerFactory.getLogger(WebsockifySslContext.class);
	private static final String PROTOCOL = "TLS";
	private SSLContext _serverContext;


	/**
	 * Returns the singleton instance for this class
	 */
	public static WebsockifySslContext
			getInstance(String keystoreType, String keystore, String password, String keyPassword) {
		WebsockifySslContext context = SingletonHolder.INSTANCE_MAP.get(keystore);
		if (context == null) {
			context = new WebsockifySslContext(keystoreType, keystore, password, keyPassword);
			SingletonHolder.INSTANCE_MAP.put(keystore, context);
		}
		return context;
	}


	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 * 
	 * See http://en.wikipedia.org/wiki/Singleton_pattern
	 */
	private static class SingletonHolder {

		public static final HashMap<String, WebsockifySslContext> INSTANCE_MAP = new HashMap<String, WebsockifySslContext>();
	}


	/**
	 * Constructor for singleton
	 */
	private WebsockifySslContext(String keystoreType, String keystore, String password) {
		this(keystoreType, keystore, password, password);
	}

	/**
	 * Constructor for singleton
	 */
	private WebsockifySslContext(String keystoreType, String keystore, String password, String keyPassword) {
		try {
			SSLContext serverContext = null;
			try {
				serverContext = getSSLContext(keystoreType, keystore, password, keyPassword);
			} catch (Exception e) {
				logger.error("Error creating SSL context for keystore", e);
				throw new Error("Failed to initialize the server-side SSLContext", e);
			}
			_serverContext = serverContext;
		} catch (Exception ex) {
			logger.error("Error initializing SslContextManager. ", ex);
		}
	}

	/**
	 * Returns the server context with server side key store
	 */
	public SSLContext getServerContext() {
		return _serverContext;
	}

	private static SSLContext
			getSSLContext(String keystoreType, String keyStoreFilePath, String password, String keyPassword)
					throws KeyStoreException, FileNotFoundException, CertificateException, NoSuchAlgorithmException,
					IOException, UnrecoverableKeyException, KeyManagementException {
		// Key store (Server side certificate)
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}

		logger.debug("keystore: {}, filePath:{}, password: {}, keyPassword:{}", new Object[] { keystoreType,
				keyStoreFilePath, password, keyPassword });
		KeyStore ks = KeyStore.getInstance(keystoreType);
		FileInputStream fin = new FileInputStream(keyStoreFilePath);
		ks.load(fin, password.toCharArray());

		// Set up key manager factory to use our key store
		// Assume key password is the same as the key store file
		// password
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
		kmf.init(ks, keyPassword.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(ks);

		// Initialise the SSLContext to work with our key managers.
		SSLContext context = SSLContext.getInstance(PROTOCOL);
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return context;
	}

	/**
	 * Validates that a keystore with the given parameters exists and can be
	 * used for an SSL context.
	 * 
	 * @param keystore
	 *            - path to the keystore file
	 * @param password
	 *            - password to the keystore file
	 * @param keyPassword
	 *            - password to the private key in the keystore file
	 * @return null if valid, otherwise a string describing the error.
	 */
	public static void validateKeystore(String keystoreType, String keystore, String password, String keyPassword)
			throws KeyManagementException, UnrecoverableKeyException, IOException, NoSuchAlgorithmException,
			CertificateException, KeyStoreException {

		getSSLContext(keystoreType, keystore, password, keyPassword);
	}
}