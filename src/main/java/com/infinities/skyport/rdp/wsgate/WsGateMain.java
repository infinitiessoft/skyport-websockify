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
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.base.Strings;
//
//public class WsGateMain {
//
//	public final static String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
//	private static boolean gSignaled = false;
//
//	// disable two connections to the same host
//	private Map<String, Boolean> activeConnections;
//
//	private final static Logger logger = LoggerFactory.getLogger(WsGateMain.class);
//
//
//	public void main() {
//		WsGate srv;
//		Config pvm = srv.getConfig();
//		int port = -1;
//		boolean https = false;
//		boolean need2 = false;
//		if (pvm.getSslPort() != null) {
//			port = pvm.getSslPort();
//			https = true;
//			if (pvm.getGlobalPort() != null) {
//				need2 = true;
//			}
//		} else if (pvm.getGlobalPort() != null) {
//			port = pvm.getGlobalPort();
//		}
//
//		MyRawSocketHandler sh = new MyRawSocketHandler(srv);
//		srv.setRawSocketHandler(sh);
//
//		EHSServerParameters oSP;
//		oSP.setPort(port);
//		oSP.setBindAddress("0.0.0.0");
//		oSP.setNoRouterRequest(1);
//
//		if (https) {
//			if (!Strings.isNullOrEmpty(pvm.getSslBindAddr())) {
//				oSP.setBindAddress(pvm.getSslBindAddr());
//			}
//			oSP.setHttps(1);
//			if (!Strings.isNullOrEmpty(pvm.getSslCertFile())) {
//				oSP.setCertificate(pvm.getSslCertFile());
//			}
//			if (!Strings.isNullOrEmpty(pvm.getSslCertPass())) {
//				oSP.setPassPhrase(pvm.getSslCertPass());
//			}
//		} else {
//			if (!Strings.isNullOrEmpty(pvm.getGlobalBindAddr())) {
//				oSP.setBindAddress(pvm.getGlobalBindAddr());
//			}
//		}
//		if (pvm.getHttpMaxRequestSize() != null) {
//			oSP.setMaxRequestSize(pvm.getHttpMaxRequestSize());
//		}
//		boolean sleepInLoop = true;
//		if (!Strings.isNullOrEmpty(pvm.getThreadingMode())) {
//			String mode = pvm.getThreadingMode();
//			if (mode.equals("single")) {
//				oSP.setMode("singlethreaded");
//				sleepInLoop = false;
//			} else if (mode.equals("pool")) {
//				oSP.setMode("threadpool");
//				if (pvm.getThreadingPoolSize() != null) {
//					oSP.setThreadCount(pvm.getThreadingPoolSize());
//				}
//			} else if (mode.equals("prerequest")) {
//				oSP.setMode("onethreadperrequest");
//			} else {
//				throw new IllegalStateException("Invalid threading mode: " + mode);
//			}
//		} else {
//			oSP.setMode("onethreadperrequest");
//		}
//		if (!Strings.isNullOrEmpty(pvm.getSslCertFile())) {
//			oSP.setCertificate(pvm.getSslCertFile());
//		}
//		if (!Strings.isNullOrEmpty(pvm.getSslCertPass())) {
//			oSP.setPassPhrase(pvm.getSslCertPass());
//		}
//
//		boolean daemon = false;
//		if (!Strings.isNullOrEmpty(pvm.getGlobalDaemon()) && pvm.getForegound() == 0) {
//			daemon = srv.getDaemon();
//			if (daemon) {
//				// start daemon
//			}
//		}
//
//		try {
//			WsGate psrv = null;
//			logger.info("wsgate starting");
//			srv.startServer(oSP);
//			logger.info("Listening on {}:{}", new Object[] { oSP.getBindAddress(), oSP.getPort() });
//
//			if (need2) {
//				psrv = new WsGate();
//				psrv.setConfigFile(srv.getConfigFile());
//				psrv.readConfig();
//				oSP.setHttps(0);
//				oSP.setPort(pvm.getGlobalPort());
//				if (!Strings.isNullOrEmpty(pvm.getGlobalBindAddr())) {
//					oSP.setBindAddress(pvm.getGlobalBindAddr());
//				}
//				psrv.setSourceEHS(srv);
//				MyRawSocketHandler psh = new MyRawSocketHandler(psrv);
//				psrv.setRawSocketHandler(psh);
//				psrv.startServer(oSP);
//				logger.info("Listening on {}:{}", new Object[] { oSP.getBindAddress(), oSP.getPort() });
//			}
//
//			if (daemon) {
//				while (!(srv.shouldTerminate() || (psrv != null && psrv.shouldTerminate()) || gSignaled)) {
//					if (sleepInLoop) {
//						Thread.sleep(50000);
//					} else {
//						srv.handleData(1000);
//						if (psrv != null) {
//							psrv.handleData(1000);
//						}
//					}
//				}
//			}
//
//			srv.stopServer();
//			if (psrv != null) {
//				psrv.stopServer();
//			}
//		} catch (Exception e) {
//			logger.error("start server failed", e);
//		}
//	}
//}
