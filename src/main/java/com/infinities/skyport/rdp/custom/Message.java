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

import com.lixia.rdp.RdpPacket_Localised;

public class Message {

	private int type;
	private int channel;
	private RdpPacket_Localised packet;


	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public RdpPacket_Localised getPacket() {
		return packet;
	}

	public void setPacket(RdpPacket_Localised packet) {
		this.packet = packet;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

}
