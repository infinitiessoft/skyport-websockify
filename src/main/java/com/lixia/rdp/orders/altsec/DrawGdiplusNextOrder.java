package com.lixia.rdp.orders.altsec;

public class DrawGdiplusNextOrder {

	private int cbSize;


	public int getCbSize() {
		return cbSize;
	}

	public void setCbSize(int cbSize) {
		this.cbSize = cbSize;
	}

	public void reset() {
		cbSize = 0;
	}
}
