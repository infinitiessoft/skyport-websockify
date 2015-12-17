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
