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
//package com.infinities.skyport.rdp.wsgate;
//
//import java.awt.Cursor;
//import java.io.IOException;
//import java.util.Calendar;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.base.Strings;
//import com.google.common.hash.Hashing;
//import com.google.common.io.BaseEncoding;
//
//public class WsGate extends HttpServlet {
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//	private static final Logger logger = LoggerFactory.getLogger(WsGate.class);
//	private static final String ws_magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
//
//
//	private enum MimeType {
//		TEXT, HTML, PNG, ICO, JAVASCRIPT, JSON, CSS, OGG, CUR, BINARY;
//	}
//
//
//	private Map.Entry<Calendar, String> cacheEntry;
//	private String m_sHostname;
//	private String m_sDocumentRoot;
//	private String m_sPidFile;
//	private boolean m_bDebug = false;
//	private boolean m_bEnableCore = false;
//	private Map<String, RDP> m_SessionMap;
//	private List<String> m_allowedHosts;
//	private List<String> m_deniedHosts;
//	private boolean m_bOrderDenyAllow = true;
//	private boolean m_bOverrideRdpHost = false;
//	private boolean m_bOverrideRdpPort = false;
//	private boolean m_bOverrideRdpUser = false;
//	private boolean m_bOverrideRdpPass = false;
//	private boolean m_bOverrideRdpPerf = false;
//	private boolean m_bOverrideRdpNowallp = false;
//	private boolean m_bOverrideRdpNowdrag = false;
//	private boolean m_bOverrideRdpNomani = false;
//	private boolean m_bOverrideRdpNotheme = false;
//	private boolean m_bOverrideRdpNotls = false;
//	private boolean m_bOverrideRdpNonla = false;
//	private boolean m_bOverrideRdpFntlm = false;
//	private String m_sRdpOverrideHost;
//	private String m_sRdpOverrideUser;
//	private String m_sRdpOverridePass;
//	private WsRdpParams m_RdpOverrideParams = new WsRdpParams();
//	private String m_sConfigFile;
//	private WsGateConfiguration m_pVm = new WsGateConfiguration();
//	private boolean m_bDaemon = false;
//	private boolean m_bRedirect = false;
//	// private StaticCache m_StaticCache;
//	private String m_sOpenStackAuthUrl;
//	private String m_sOpenStackUsername;
//	private String m_sOpenStackPassword;
//	private String m_sOpenStackTenantName;
//	private String m_sHyperVHostUsername;
//	private String m_sHyperVHostPassword;
//
//
//	public MimeType simpleMime(String fileName) {
//		if (fileName.endsWith(".txt")) {
//			return MimeType.TEXT;
//		}
//		if (fileName.endsWith(".html")) {
//			return MimeType.HTML;
//		}
//		if (fileName.endsWith(".png")) {
//			return MimeType.PNG;
//		}
//		if (fileName.endsWith(".ico")) {
//			return MimeType.ICO;
//		}
//		if (fileName.endsWith(".js")) {
//			return MimeType.JAVASCRIPT;
//		}
//		if (fileName.endsWith(".json")) {
//			return MimeType.JSON;
//		}
//		if (fileName.endsWith(".css")) {
//			return MimeType.CSS;
//		}
//		if (fileName.endsWith(".cur")) {
//			return MimeType.CUR;
//		}
//		if (fileName.endsWith(".ogg")) {
//			return MimeType.OGG;
//		}
//		return MimeType.BINARY;
//	}
//
//	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
//		response.setStatus(handleRequest(request, response));
//	}
//
//	protected int handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
//		String uri = request.getRequestURI();
//		String thisHost = Strings.isNullOrEmpty(m_sHostname) ? request.getLocalAddr() : m_sHostname;
//
//		if (m_bRedirect && (!request.isSecure())) {
//			return handleRedirectRequest(request, response, uri, thisHost);
//		}
//
//		if (uri.startsWith("/robots.txt")) {
//			return handleRobotsRequest(request, response, uri, thisHost);
//		}
//		if (uri.startsWith("/cur/")) {
//			return handleCursorRequest(request, response, uri, thisHost);
//		}
//		if (uri.startsWith("/wsgate?")) {
//			return handleWsgateRequest(request, response, uri, thisHost);
//		}
//		if (uri.startsWith("/connect?")) {
//			return HttpServletResponse.SC_OK;
//		}
//		return handleHttpRequest(request, response);
//	}
//
//	private int handleWsgateRequest(HttpServletRequest request, HttpServletResponse response, String uri, String thisHost) {
//		// FreeRDP Params
//		String dtsize;
//		String rdphost;
//		String rdppcb;
//		String rdpuser;
//		int rdpport;
//		String rdppass;
//		WsRdpParams params;
//		boolean setCookie = true;
//
//		if (uri.startsWith("/wsgate?token=")) {
//			setCookie = false;
//			try {
//				logger.info("Starting Openstack token authentication");
//				String tokenId = request.getParameter("token");
//				NovaConsoleTokenAuth tokenAuth = NovaConsoleTokenAuthFactory.getInstance();
//				novaConsoleInfo info = tokenAuth.getConsoleInfo(m_sOpenstackAuthUrl, m_sOpenstackUsername,
//						m_sOpenstackPassword, m_sOpenStackTenantName, tokenId);
//				logger.info("Host: {} Port: {} Internal access path: {}", new Object[] { info.getHost(), info.getPort(),
//						info.getInternalAccessPath() });
//				rdphost = info.getHost();
//				rdpport = info.getPort();
//				rdppcb = info.getInternalAccessPath();
//
//				rdpuser = m_sHyperVHostUsername;
//				rdppass = m_sHyperVHostPassword;
//			} catch (Exception e) {
//				logger.error("Openstack token authentication failed", e);
//				return HttpServletResponse.SC_BAD_REQUEST;
//			}
//		} else {
//			dtsize = request.getParameter("dtsize");
//			// TODO decode?
//			rdphost = request.getParameter("host");
//			rdppcb = request.getParameter("pcb");
//			rdpuser = request.getParameter("user");
//			rdpport = Integer.parseInt(request.getParameter("port"));
//			rdppass = new String(BaseEncoding.base64().decode(request.getParameter("pass")));
//		}
//
//		params.setPort(rdpport);
//		params.setWidth(1024);
//		params.setHeight(768);
//		params.setPerf(m_bOverrideRdpPerf ? m_RdpOverrideParams.getPerf() : nFormValue(request, "perf", 0));
//		params.setFntlm(m_bOverrideRdpFntlm ? m_RdpOverrideParams.getFntlm() : nFormValue(request, "fntlm", 0));
//		params.setNotls(m_bOverrideRdpNotls ? m_RdpOverrideParams.getNotls() : nFormValue(request, "notls", 0));
//		params.setNonla(m_bOverrideRdpNonla ? m_RdpOverrideParams.getNonla() : nFormValue(request, "nonla", 0));
//		params.setNowallp(m_bOverrideRdpNowallp ? m_RdpOverrideParams.getNowallp() : nFormValue(request, "nowallp", 0));
//		params.setNowdrag(m_bOverrideRdpNowdrag ? m_RdpOverrideParams.getNowdrag() : nFormValue(request, "nowdrag", 0));
//		params.setNomani(m_bOverrideRdpNomani ? m_RdpOverrideParams.getNomani() : nFormValue(request, "nomani", 0));
//		params.setNotheme(m_bOverrideRdpNotheme ? m_RdpOverrideParams.getNotheme() : nFormValue(request, "notheme", 0));
//
//		checkForPredefined(rdphost, rdpuser, rdppass);
//
//		if (!ConnectionIdAllowed(rdphost)) {
//			logInfo(request.getRemoteAddr(), rdphost, "403 Denied by access rules");
//			return HttpServletResponse.SC_FORBIDDEN;
//		}
//
//		if (!Strings.isNullOrEmpty(dtsize)) {
//			try {
//				String[] wh = dtsize.split("x");
//				if (wh.length == 2) {
//					params.setWidth(Integer.parseInt(wh[0]));
//					params.setHeight(Integer.parseInt(wh[1]));
//				}
//			} catch (Exception e) {
//				params.setWidth(1024);
//				params.setHeight(768);
//			}
//		}
//		int wsocketCheck = checkIfWSocketRequest(request, response, uri, thisHost);
//		if (wsocketCheck != 0) {
//			switch (wsocketCheck) {
//			case 400:
//				return HttpServletResponse.SC_BAD_REQUEST;
//			case 426:
//				return 426;
//			}
//		}
//
//		String wskey = request.getHeader("Sec-WebSocket-Key");
//		String sha1;
//		byte digest[] = new byte[5];
//		sha1 = wskey + ws_magic;
//		try {
//			Hashing.sha1().hashString(sha1).writeBytesTo(digest, 0, 5);
//		} catch (Exception e) {
//			logInfo(request.getRemoteAddr(), uri, "500 (Digest calculation failed)");
//			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
//		}
//
//		// ignore handle endianess
//
//		MyRawSocketHandler sh = getRawSocketHandler();
//		if (sh == null) {
//			throw new RuntimeException("No raw socket handler available");
//		}
//
//		try {
//			if (!sh.prepare(request, rdphost, rdppcb, rdpuser, rdppass, params)) {
//				logInfo(request.getRemoteAddr(), uri, "503 (RDP backend not available)");
//				return HttpServletResponse.SC_SERVICE_UNAVAILABLE;
//			}
//		} catch (Exception e) {
//			logger.info("caught exception", e);
//		}
//
//		if (setCookie) {
//			manageCookies(request, response, rdphost, rdppcb, rdpuser, thisHost);
//		} else {
//			manageCookies(request, response, "", "", "", thisHost);
//		}
//
//		// ignore remove header
//		String wsproto = request.getHeader("Sec-WebSocket-Protocol");
//		if (0 < wsproto.length()) {
//			response.setHeader("Sec-WebSocket-Protocol", wsproto);
//		}
//		response.setHeader("Upgrade", "websocket");
//		response.setHeader("Connection", "Upgrade");
//		response.setHeader("Sec-WebSocket-Accept", BaseEncoding.base64().encode(digest));
//		logInfo(request.getRemoteAddr(), uri, "101");
//		return HttpServletResponse.SC_SWITCHING_PROTOCOLS;
//	}
//
//	private int nFormValue(HttpServletRequest request, String name, int defval) {
//		String tmp = request.getParameter(name);
//		int ret = defval;
//		if (!Strings.isNullOrEmpty(tmp)) {
//			try {
//				ret = Integer.parseInt(tmp);
//			} catch (Exception e) {
//				ret = defval;
//			}
//		}
//		return ret;
//	}
//
//	private void manageCookies(HttpServletRequest request, HttpServletResponse response, String rdphost, String rdppcb,
//			String rdpuser, String thisHost) {
//		Cookie setcookie = new Cookie("lastpcb", rdppcb);
//		setcookie.setPath("/");
//		setcookie.setMaxAge(864000);
//		if (request.isSecure()) {
//			setcookie.setSecure(false);
//		}
//
//		Cookie delcookie = new Cookie("lastpcb", "%20");
//		delcookie.setPath("/");
//		delcookie.setMaxAge(0);
//		if (request.isSecure()) {
//			delcookie.setSecure(false);
//		}
//
//		if (Strings.isNullOrEmpty(rdppcb)) {
//			response.addCookie(delcookie);
//		} else {
//			response.addCookie(setcookie);
//		}
//
//		if (!Strings.isNullOrEmpty(rdphost)) {
//			delcookie = new Cookie("lasthost", "%20");
//			delcookie.setPath("/");
//			delcookie.setMaxAge(0);
//			if (request.isSecure()) {
//				delcookie.setSecure(false);
//			}
//			response.addCookie(delcookie);
//		} else {
//			setcookie = new Cookie("lasthost", (m_bOverrideRdpHost ? "<predefined>" : rdphost));
//			setcookie.setPath("/");
//			setcookie.setMaxAge(864000);
//			if (request.isSecure()) {
//				setcookie.setSecure(false);
//			}
//			response.addCookie(setcookie);
//		}
//
//		if (!Strings.isNullOrEmpty(rdpuser)) {
//			delcookie = new Cookie("lastuser", "%20");
//			delcookie.setPath("/");
//			delcookie.setMaxAge(0);
//			if (request.isSecure()) {
//				delcookie.setSecure(false);
//			}
//			response.addCookie(delcookie);
//		} else {
//			setcookie = new Cookie("lastuser", (m_bOverrideRdpUser ? "<predefined>" : rdpuser));
//			setcookie.setPath("/");
//			setcookie.setMaxAge(864000);
//			if (request.isSecure()) {
//				setcookie.setSecure(false);
//			}
//			response.addCookie(setcookie);
//		}
//	}
//
//	private int checkIfWSocketRequest(HttpServletRequest request, HttpServletResponse response, String uri, String thisHost) {
//		// TODO ignore http version check;
//
//		String wshost = toLowerCopy(request.getHeader("Host"));
//		String wsconn = toLowerCopy(request.getHeader("Connection"));
//		String wsupg = toLowerCopy(request.getHeader("Upgrade"));
//		String wsver = request.getHeader("Sec-WebSocket-Version");
//		String wskey = request.getHeader("Sec-WebSocket-Key");
//
//		String wsproto = request.getHeader("Sec-WebSocket-Protocol");
//		String wsext = request.getHeader("Sec-WebSocket-Extension");
//
//		if (!MultivalHeaderContains(wsconn, "upgrade")) {
//			logInfo(request.getRemoteAddr(), uri, "400 (No upgrade header)");
//			return 400;
//		}
//		if (!MultivalHeaderContains(wsupg, "websocket")) {
//			logInfo(request.getRemoteAddr(), uri, "400 (Upgrade header does not contain websocket tag)");
//			return 400;
//		}
//		if (!thisHost.equals(wshost)) {
//			logInfo(request.getRemoteAddr(), uri, "400 (Host header does not match)");
//			return 400;
//		}
//		String wskeyDecoded = new String(BaseEncoding.base64().decode(wskey));
//		if (16 != wskeyDecoded.length()) {
//			logInfo(request.getRemoteAddr(), uri, "400 (Invalid WebSocket key)");
//			return 400;
//		}
//		if (!MultivalHeaderContains(wsver, "13")) {
//			response.setHeader("Sec-WebSocket-Version", "13");
//			logInfo(request.getRemoteAddr(), uri, "426 (Protocol version not 13)");
//			return 426;
//		}
//		return 0;
//	}
//
//	private boolean MultivalHeaderContains(String header, String value) {
//		return header.trim().equals(value.trim());
//	}
//
//	private String toLowerCopy(String header) {
//		return header.toLowerCase();
//	}
//
//	private boolean ConnectionIdAllowed(String rdphost) {
//		// TODO ignore denied hosts
//		return true;
//	}
//
//	private void checkForPredefined(String rdphost, String rdpuser, String rdppass) {
//		// TODO ignore predefined
//	}
//
//	private int handleHttpRequest(HttpServletRequest request, HttpServletResponse response) {
//		return handleHttpRequest(request, response, false);
//	}
//
//	private int handleHttpRequest(HttpServletRequest request, HttpServletResponse response, boolean tokenAuth) {
//		String uri = request.getRequestURI();
//		logInfo(request.getRemoteAddr(), uri, "404 Not found");
//		return HttpServletResponse.SC_NOT_FOUND;
//		// String uri = request.getRequestURI();
//		// String thisHost = Strings.isNullOrEmpty(m_sHostname) ?
//		// request.getLocalAddr() : m_sHostname;
//		//
//		// boolean bDynDebug = m_bDebug;
//		// if (!bDynDebug) {
//		// if ("true".equals(request.getHeader("X-WSGate-Debug"))) {
//		// bDynDebug = true;
//		// }
//		// }
//		// if (!thisHost.equals(request.getHeader("Host"))) {
//		// logInfo(request.getRemoteAddr(), uri, "404 Not found");
//		// return HttpServletResponse.SC_NOT_FOUND;
//		// }
//		// URI p = new URI(m_sDocumentRoot);
//		// p = new URI(p.getPath() + uri);
//		// if (uri.endsWith("/")) {
//		// p = new URI(p.getPath() + (bDynDebug ? "/index-debug.html" :
//		// "/index.html"));
//		// }
//		// p.normalize();
//		//
//		// boolean extenalRequest = false;
//		// if (!new File(p).exists()) {
//		// p = new URI(m_sDocumentRoot);
//		// p = new URI(p.getPath() + "/index.html");
//		// }
//		//
//		// if (!new File(p).isFile()) {
//		// logInfo(request.getRemoteAddr(), uri, "404 Not found");
//		// logger.warn("Request from {} : {} ==> 403 Forbidden", new Object[] {
//		// request.getRemoteAddr(), uri });
//		// p = new URI(m_sDocumentRoot);
//		// p = new URI(p.getPath() + "/index.html");
//		// }
//		//
//		// Calendar mtime = lastWriteTime(p);
//		// if (notModified(request, response, mtime)) {
//		// return HttpServletResponse.SC_NOT_MODIFIED;
//		// }
//
//	}
//
//	private void logInfo(String remoteAddr, String uri, String response) {
//		logger.info("Request FROM: {}", remoteAddr);
//		logger.info("To URI: {} => {}", new Object[] { uri, response });
//	}
//
//	private int handleRedirectRequest(HttpServletRequest request, HttpServletResponse response, String uri, String thisHost) {
//		String dest = uri.startsWith("/wsgate?") ? "wss" : "https";
//
//		if (m_pVm.getSsl().getPort() != null) {
//			int sslPort = m_pVm.getSsl().getPort();
//			String thisSslHost = thisHost.substring(0, thisHost.indexOf(":")) + ":" + sslPort;
//			dest = dest + "://" + thisSslHost + uri;
//			response.setHeader("Location", dest);
//			logInfo(request.getRemoteAddr(), uri, "301 Moved permanently");
//			return HttpServletResponse.SC_MOVED_PERMANENTLY;
//		} else {
//			logInfo(request.getRemoteAddr(), uri, "404 Not found");
//			return HttpServletResponse.SC_NOT_FOUND;
//		}
//	}
//
//	private int handleCursorRequest(HttpServletRequest request, HttpServletResponse response, String uri, String thisHost) {
//		String idpart = uri.substring(0, 5);
//		String parts[] = idpart.split("/");
//		RDP it = m_SessionMap.get(parts[0]);
//
//		if (it != null) {
//			int cid = 0;
//			try {
//				cid = Integer.parseInt(parts[1]);
//			} catch (Exception e) {
//				cid = 0;
//			}
//			if (cid == 1) {
//				Cursor c = it.getCursor(cid);
//				Calendar ct = c.get(0);
//
//				if (0L != ct.getTimeInMillis()) {
//					if (notModified(request, response, ct)) {
//						return HttpServletResponse.SC_NOT_MODIFIED;
//					}
//					String png = c.get(1);
//					if (!Strings.isNullOrEmpty(png)) {
//						response.setContentType("image/cur");
//						response.setHeader("Last- Modified", ct.toString());
//						response.setContentLength(png.length());
//						response.getWriter().write(png);
//						response.getWriter().flush();
//						response.getWriter().close();
//						logInfo(request.getRemoteAddr(), uri, "200 OK");
//						return HttpServletResponse.SC_OK;
//					}
//				}
//			}
//		}
//		logInfo(request.getRemoteAddr(), uri, "404 Not found");
//		return HttpServletResponse.SC_NOT_FOUND;
//	}
//
//	private boolean notModified(HttpServletRequest request, HttpServletResponse response, Calendar mtime) {
//		String ifms = request.getHeader("if-modified-since");
//		if (!Strings.isNullOrEmpty(ifms)) {
//			Calendar fileTime = mtime;
//			Calendar reqTime = Calendar.getInstance();
//
//			if (fileTime.equals(reqTime) || fileTime.before(reqTime)) {
//				logger.info("Request from {}: {} => 304 Not modified",
//						new Object[] { request.getRemoteAddr(), request.getRequestURI() });
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private int handleRobotsRequest(HttpServletRequest request, HttpServletResponse response, String uri, String thisHost)
//			throws IOException {
//		response.setContentType("text/plain");
//		response.setContentLength(26);
//		response.getWriter().write("User-agent: *\nDisallow: /\n");
//		response.getWriter().flush();
//		response.getWriter().close();
//		return HttpServletResponse.SC_OK;
//	}
//
//}
