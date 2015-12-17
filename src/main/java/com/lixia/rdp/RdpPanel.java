package com.lixia.rdp;

public interface RdpPanel {

	boolean isConnected();

	void disconnect();

	void rdp5_process(RdpPacket_Localised s, boolean b) throws Exception;

	void sendInput(int time, int rdpInputScancode, int i, int j, int k) throws Exception;

}
