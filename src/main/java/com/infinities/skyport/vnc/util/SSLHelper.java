/*
 * Copyright 2005, 2015009 Citrix Systems, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation, with the additional linking exception as
 * follows:
 * 
 *   Linking this library statically or dynamically with other modules is
 *   making a combined work based on this library. Thus, the terms and
 *   conditions of the GNU General Public License cover the whole combination.
 * 
 *   As a special exception, the copyright holders of this library give you
 *   permission to link this library with independent modules to produce an
 *   executable, regardless of the license terms of these independent modules,
 *   and to copy and distribute the resulting executable under terms of your
 *   choice, provided that you also meet, for each linked independent module,
 *   the terms and conditions of the license of that module. An independent
 *   module is a module which is not derived from or based on this library. If
 *   you modify this library, you may extend this exception to your version of
 *   the library, but you are not obligated to do so. If you do not wish to do
 *   so, delete this exception statement from your version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.infinities.skyport.vnc.util;

import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.vnc.handler.SslHandler;

public class SSLHelper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(SSLHelper.class);

	public static final String[] cipherSuites = { "TLS_DHE_RSA_WITH_AES_128_CBC_SHA" };
	private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			logger.debug("UNKNOWN CLIENT CERTIFICATE: {}", certs[0].getSubjectDN());
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			logger.debug("UNKNOWN SERVER CERTIFICATE: {}", certs[0].getSubjectDN());
		}
	} };
	private static SSLContext sSSLContext = null;
	private static SSLHelper instance = new SSLHelper();


	/**
	 * Factory method to get the singleton of this class
	 * 
	 * @return <Code>SSLHelper</Code>
	 */
	public static SSLHelper getInstance() {
		return instance;
	}

	public SSLContext getSSLContext() {
		return sSSLContext;
	}

	public SslHandler getTrustAllSSLHandler() {
		javax.net.ssl.SSLContext sslContext = SSLHelper.getInstance().getSSLContext();
		try {
			sslContext.init(null, trustAllCerts, new SecureRandom());
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(true);
		sslEngine.setNeedClientAuth(false);
		sslEngine.setWantClientAuth(false);

		SslHandler handler = new SslHandler(sslEngine);
		// handler.setIssueHandshake(true);
		return handler;
	}

	public SslHandler getTrustAllClientSSLHandler() {
		javax.net.ssl.SSLContext sslContext = SSLHelper.getInstance().getSSLContext();
		try {
			sslContext.init(null, trustAllCerts, new SecureRandom());
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(false);
		// sslEngine.setNeedClientAuth(false);
		// sslEngine.setWantClientAuth(false);

		SslHandler handler = new SslHandler(sslEngine);
		// handler.setIssueHandshake(true);
		return handler;
	}

	// private void init() {
	// try {
	// SSLContext pSSLContext = SSLContext.getInstance("SSL", "SunJSSE");
	//
	// // The reference implementation only supports X.509 keys
	// KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509",
	// "SunJSSE");
	// TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509",
	// "SunJSSE");
	//
	// // Sun's default kind of key store
	// KeyStore ks = KeyStore.getInstance("JCEKS", "SunJCE");
	//
	// char[] password = "b3646d1424de7a06".toCharArray();
	// ks.load(this.getClass().getResourceAsStream("client.ks"), password);
	// // ks.load(ResourceLoader.getResourceAsStream("appserver.ks"),
	// // password);
	// kmf.init(ks, password);
	// tmf.init(ks);
	//
	// pSSLContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
	// sSSLContext = pSSLContext;
	// } catch (Exception e) {
	// logger.error("ERROR: failed to initialize SSLContext", e);
	// // TODO: handle exceptions
	// }
	// }

	private void init() {
		try {
			SSLContext pSSLContext = SSLContext.getInstance("SSL");
			TrustManager[] trustManagers = new TrustManager[] { NO_OP_TRUST_MANAGER };
			pSSLContext.init(null, trustManagers, null);
			sSSLContext = pSSLContext;
		} catch (Exception e) {
			logger.error("ERROR: failed to initialize SSLContext", e);
			// TODO: handle exceptions
		}
	}


	private static TrustManager NO_OP_TRUST_MANAGER = new X509TrustManager() {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			return;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			return;
		}
	};


	private SSLHelper() {
		init();
	}
}
