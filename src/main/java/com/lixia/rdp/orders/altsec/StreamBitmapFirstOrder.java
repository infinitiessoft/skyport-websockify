package com.lixia.rdp.orders.altsec;

public class StreamBitmapFirstOrder {

	private int bitmapFlags;
	private int bitmapBpp;
	private int bitmapType;
	private int bitmapWidth;
	private int bitmapHeight;
	private int bitmapSize;
	private int bitmapBlockSize;


	public int getBitmapFlags() {
		return bitmapFlags;
	}

	public void setBitmapFlags(int bitmapFlags) {
		this.bitmapFlags = bitmapFlags;
	}

	public int getBitmapBpp() {
		return bitmapBpp;
	}

	public void setBitmapBpp(int bitmapBpp) {
		this.bitmapBpp = bitmapBpp;
	}

	public int getBitmapType() {
		return bitmapType;
	}

	public void setBitmapType(int bitmapType) {
		this.bitmapType = bitmapType;
	}

	public int getBitmapWidth() {
		return bitmapWidth;
	}

	public void setBitmapWidth(int bitmapWidth) {
		this.bitmapWidth = bitmapWidth;
	}

	public int getBitmapHeight() {
		return bitmapHeight;
	}

	public void setBitmapHeight(int bitmapHeight) {
		this.bitmapHeight = bitmapHeight;
	}

	public int getBitmapSize() {
		return bitmapSize;
	}

	public void setBitmapSize(int bitmapSize) {
		this.bitmapSize = bitmapSize;
	}

	public int getBitmapBlockSize() {
		return bitmapBlockSize;
	}

	public void setBitmapBlockSize(int bitmapBlockSize) {
		this.bitmapBlockSize = bitmapBlockSize;
	}

	public void reset() {
		bitmapFlags = 0;
		bitmapBpp = 0;
		bitmapType = 0;
		bitmapWidth = 0;
		bitmapHeight = 0;
		bitmapSize = 0;
		bitmapBlockSize = 0;
	}

}
