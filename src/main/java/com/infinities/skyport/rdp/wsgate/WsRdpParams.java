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
package com.infinities.skyport.rdp.wsgate;

public class WsRdpParams {

	// / The RDP port to connect to
	private int port;
	// / The desktop width for the RDP session.
	private int width;
	// / The desktop height for the RDP session.
	private int height;
	// / The performance flags for the RDP session.
	private int perf;
	// / The NTLM auth version.
	private int fntlm;
	// / Flag: Disable TLS.
	private int notls;
	// / Flag: Disable network level authentication.
	private int nonla;
	// / Flag: Disable wallpaper.
	private int nowallp;
	// / Flag: Disable full window drag.
	private int nowdrag;
	// / Flag: Disable menu animations.
	private int nomani;
	// / Flag: Disable theming.
	private int notheme;


	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getPerf() {
		return perf;
	}

	public void setPerf(int perf) {
		this.perf = perf;
	}

	public int getFntlm() {
		return fntlm;
	}

	public void setFntlm(int fntlm) {
		this.fntlm = fntlm;
	}

	public int getNotls() {
		return notls;
	}

	public void setNotls(int notls) {
		this.notls = notls;
	}

	public int getNonla() {
		return nonla;
	}

	public void setNonla(int nonla) {
		this.nonla = nonla;
	}

	public int getNowallp() {
		return nowallp;
	}

	public void setNowallp(int nowallp) {
		this.nowallp = nowallp;
	}

	public int getNowdrag() {
		return nowdrag;
	}

	public void setNowdrag(int nowdrag) {
		this.nowdrag = nowdrag;
	}

	public int getNomani() {
		return nomani;
	}

	public void setNomani(int nomani) {
		this.nomani = nomani;
	}

	public int getNotheme() {
		return notheme;
	}

	public void setNotheme(int notheme) {
		this.notheme = notheme;
	}

}
