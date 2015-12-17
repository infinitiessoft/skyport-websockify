package com.lixia.rdp.orders.altsec;

public class DrawGdiCacheNextOrder {

	private int flags;
	private int cacheType;
	private int cacheIndex;
	private int cbSize;


	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getCacheType() {
		return cacheType;
	}

	public void setCacheType(int cacheType) {
		this.cacheType = cacheType;
	}

	public int getCacheIndex() {
		return cacheIndex;
	}

	public void setCacheIndex(int cacheIndex) {
		this.cacheIndex = cacheIndex;
	}

	public int getCbSize() {
		return cbSize;
	}

	public void setCbSize(int cbSize) {
		this.cbSize = cbSize;
	}

	public void reset() {
		flags = 0;
		cacheType = 0;
		cacheIndex = 0;
		cbSize = 0;
	}

}
