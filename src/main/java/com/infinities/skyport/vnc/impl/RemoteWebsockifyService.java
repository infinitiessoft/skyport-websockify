package com.infinities.skyport.vnc.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.infinities.skyport.compute.CMD;
import com.infinities.skyport.exception.SkyportException;
import com.infinities.skyport.model.console.ConsoleType;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.service.WebsockifyService;
import com.infinities.skyport.util.JsonUtil;
import com.infinities.skyport.websockify.WebsockifyServer;

public class RemoteWebsockifyService implements WebsockifyService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger logger = LoggerFactory.getLogger(RemoteWebsockifyService.class);
	private final ServerConfiguration configuration;


	public RemoteWebsockifyService(ServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void close() throws IOException {
		logger.debug("close RemoteWebsockifyService");
	}

	@Override
	public void initialize() throws Exception {
		String protocol = configuration.isEnableSSL() ? "https://" : "http://";
		String ip = configuration.getIp();
		String port = configuration.getPort();
		String context = "/skyportWeb";
		String loginname = configuration.getUsername();
		String loginpass = configuration.getPassword();

		StringBuffer url = new StringBuffer();
		url.append(protocol).append(ip).append(":").append(port).append(context).append("?cmd=").append(CMD.LOGIN)
				.append("&loginname=").append(loginname).append("&loginpass=").append(loginpass);

		String ret = invokeCommand(url.toString());
		logger.debug("connect to Remote websockifyService: {}", ret);
		JsonNode node = JsonUtil.readJson(ret);
		if (0 == node.get("status").asInt()) {
			throw new SkyportException("cannot login to remote websockify server, please check connection data: "
					+ node.get("msg").asText());
		}

	}

	@Override
	public WebsockifyProfile sockify(WebsockifyProfile profile) throws Exception {
		String protocol = configuration.isEnableSSL() ? "https://" : "http://";
		String ip = configuration.getIp();
		String port = configuration.getPort();
		String context = "/skyportWeb";

		String host = profile.getTargetHost();
		int targetPort = profile.getTargetPort();
		boolean ssl = profile.isRequireSSL();
		String token = profile.getToken();
		String password = URLEncoder.encode(profile.getPassword(), "UTF-8");
		ConsoleType type = profile.getConsoleType();
		String loginname = configuration.getUsername();
		String loginpass = configuration.getPassword();
		boolean internal_ssl = profile.isInternalSSL();

		StringBuffer url = new StringBuffer();
		url.append(protocol).append(ip).append(":").append(port).append(context).append("?cmd=").append(CMD.WEBSOCKIFY)
				.append("&ip=").append(host).append("&port=").append(targetPort).append("&ssl=").append(ssl)
				.append("&token=").append(token).append("&password=").append(password).append("&type=").append(type.name())
				.append("&loginname=").append(loginname).append("&loginpass=").append(loginpass).append("&internal_ssl=")
				.append(internal_ssl);
		// String u = URLEncoder.encode(url.toString(), "utf-8");
		String u = new URI(url.toString()).toASCIIString();

		logger.debug("connect to Remote websockifyService: {}", u);
		String ret = invokeCommand(u);
		JsonNode node = JsonUtil.readJson(ret);

		if (0 == node.get("status").asInt()) {
			throw new SkyportException("websockify fail: " + node.get("msg").asText());
		}

		WebsockifyProfile retProfile = JsonUtil.readJson(node.get("data"), WebsockifyProfile.class);
		logger.debug("profile: {}", retProfile.toString());

		return retProfile;
	}

	@Override
	public WebsockifyProfile release(WebsockifyProfile profile) throws Exception {
		String protocol = configuration.isEnableSSL() ? "https://" : "http://";
		String ip = configuration.getIp();
		String port = configuration.getPort();
		String context = "/skyportWeb";

		String host = profile.getTargetHost();
		int targetPort = profile.getTargetPort();
		boolean ssl = profile.isRequireSSL();
		String token = profile.getToken();
		String password = profile.getPassword();
		ConsoleType type = profile.getConsoleType();
		String loginname = configuration.getUsername();
		String loginpass = configuration.getPassword();

		StringBuffer url = new StringBuffer();
		url.append(protocol).append(ip).append(":").append(port).append(context).append("?cmd=")
				.append(CMD.RELEASE_WEBSOCKET).append("&ip=").append(host).append("&port=").append(targetPort)
				.append("&ssl=").append(ssl).append("&token=").append(token).append("&password=").append(password)
				.append("&type=").append(type.name()).append("&loginname=").append(loginname).append("&loginpass=")
				.append(loginpass);

		String ret = invokeCommand(url.toString());
		JsonNode node = JsonUtil.readJson(ret);
		logger.debug("connect to Remote websockifyService: {}", ret);
		if (0 == node.get("status").asInt()) {
			throw new SkyportException("websockify fail: " + node.get("msg").asText());
		}

		WebsockifyProfile retProfile = JsonUtil.readJson(node.get("data"), WebsockifyProfile.class);
		logger.debug("profile: {}", retProfile.toString());

		return retProfile;
	}

	@Override
	public void setSecurityManager(Object securityManager) {
		logger.debug("set SecurityManager for RemoteWebsockifyService");
	}

	@Override
	public void setWebsockifyServer(WebsockifyServer skyportWebsockifyServer) {
		logger.debug("set WebsockifyServer for RemoteWebsockifyService");
	}

	private String invokeCommand(String url) throws Exception {
		InputStream im = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		HttpURLConnection uc = null;
		StringBuffer mess = new StringBuffer();
		try {
			uc = getConnection(url);
			// uc.setRequestMethod("GET");
			// uc.setDoOutput(true);
			// uc.setDoInput(true);
			uc.connect();
			im = uc.getInputStream();
			isr = new InputStreamReader(im);
			br = new BufferedReader(isr);

			int c = 0;
			while ((c = br.read()) != -1) {
				mess.append((char) c);
			}

			// while ((line = br.readLine()) != null) {
			// mess.append(line);
			// }
		} catch (Exception e) {
			throw e;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("Unexpected exception when close BufferedReader", e);
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
					logger.error("Unexpected exception when close InputStreamReader", e);
				}
			}
			if (im != null) {
				try {
					im.close();
				} catch (IOException e) {
					logger.error("Unexpected exception when close InputStream", e);
				}
			}
			if (uc != null) {
				uc.disconnect();
			}
		}

		return mess.toString();
	}

	private HttpURLConnection getConnection(String url) throws Exception {
		if (url.contains("https")) {
			HostnameVerifier hv = new HostnameVerifier() {

				@Override
				public boolean verify(String urlHostName, SSLSession session) {
					System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
					return true;
				}
			};
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		}
		URL ul = new URL(url);
		URLConnection connection = ul.openConnection();

		if (url.contains("https")) {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };

			SSLContext sc = SSLContext.getInstance("SSL");

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			final SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
			((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
			connection.setConnectTimeout(60000);
			connection.setReadTimeout(60000);
		}
		return (HttpURLConnection) connection;

	}

}
