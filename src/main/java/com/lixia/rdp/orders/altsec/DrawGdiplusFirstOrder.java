package com.lixia.rdp.orders.altsec;

public class DrawGdiplusFirstOrder {

	private int cbSize;
	private int cbTotalSize;
	private int cbTotalEmfSize;


	public int getCbSize() {
		return cbSize;
	}

	public void setCbSize(int cbSize) {
		this.cbSize = cbSize;
	}

	public int getCbTotalSize() {
		return cbTotalSize;
	}

	public void setCbTotalSize(int cbTotalSize) {
		this.cbTotalSize = cbTotalSize;
	}

	public int getCbTotalEmfSize() {
		return cbTotalEmfSize;
	}

	public void setCbTotalEmfSize(int cbTotalEmfSize) {
		this.cbTotalEmfSize = cbTotalEmfSize;
	}

	public void reset() {
		cbSize = 0;
		cbTotalSize = 0;
		cbTotalEmfSize = 0;
	}

}
