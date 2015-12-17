package com.lixia.rdp.orders.altsec;

public class DrawGdiCacheFirstOrder {

	private int flags;
	private int cacheType;
	private int cacheIndex;
	private int cbSize;
	private int cbTotalSize;


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

	public int getCbTotalSize() {
		return cbTotalSize;
	}

	public void setCbTotalSize(int cbTotalSize) {
		this.cbTotalSize = cbTotalSize;
	}

	public void reset() {
		flags = 0;
		cacheType = 0;
		cacheIndex = 0;
		cbSize = 0;
		cbTotalSize = 0;
	}
}
