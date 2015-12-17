package com.lixia.rdp.orders.altsec;

public class StreamBitmapNextOrder {

	private int bitmapFlags;
	private int bitmapType;
	private int bitmapBlockSize;


	public int getBitmapFlags() {
		return bitmapFlags;
	}

	public void setBitmapFlags(int bitmapFlags) {
		this.bitmapFlags = bitmapFlags;
	}

	public int getBitmapType() {
		return bitmapType;
	}

	public void setBitmapType(int bitmapType) {
		this.bitmapType = bitmapType;
	}

	public int getBitmapBlockSize() {
		return bitmapBlockSize;
	}

	public void setBitmapBlockSize(int bitmapBlockSize) {
		this.bitmapBlockSize = bitmapBlockSize;
	}

	public void reset() {
		bitmapFlags = 0;
		bitmapType = 0;
		bitmapBlockSize = 0;
	}

}
