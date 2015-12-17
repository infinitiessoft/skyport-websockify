package com.lixia.rdp.orders.altsec;

public class CreateNineGridBitmapOrder {

	private int bitmapBpp;
	private int bitmapId;
	private NineGridBitmapInfo nineGridInfo = new NineGridBitmapInfo();


	public class NineGridBitmapInfo {

		private int flFlags;
		private int ulLeftWidth;
		private int ulRightWidth;
		private int ulTopHeight;
		private int ulBottomHeight;
		private int crTransparent;


		public int getFlFlags() {
			return flFlags;
		}

		public void setFlFlags(int flFlags) {
			this.flFlags = flFlags;
		}

		public int getUlLeftWidth() {
			return ulLeftWidth;
		}

		public void setUlLeftWidth(int ulLeftWidth) {
			this.ulLeftWidth = ulLeftWidth;
		}

		public int getUlRightWidth() {
			return ulRightWidth;
		}

		public void setUlRightWidth(int ulRightWidth) {
			this.ulRightWidth = ulRightWidth;
		}

		public int getUlTopHeight() {
			return ulTopHeight;
		}

		public void setUlTopHeight(int ulTopHeight) {
			this.ulTopHeight = ulTopHeight;
		}

		public int getUlBottomHeight() {
			return ulBottomHeight;
		}

		public void setUlBottomHeight(int ulBottomHeight) {
			this.ulBottomHeight = ulBottomHeight;
		}

		public int getCrTransparent() {
			return crTransparent;
		}

		public void setCrTransparent(int crTransparent) {
			this.crTransparent = crTransparent;
		}

	}


	public int getBitmapBpp() {
		return bitmapBpp;
	}

	public void setBitmapBpp(int bitmapBpp) {
		this.bitmapBpp = bitmapBpp;
	}

	public int getBitmapId() {
		return bitmapId;
	}

	public void setBitmapId(int bitmapId) {
		this.bitmapId = bitmapId;
	}

	public NineGridBitmapInfo getNineGridInfo() {
		return nineGridInfo;
	}

	public void setNineGridInfo(NineGridBitmapInfo nineGridInfo) {
		this.nineGridInfo = nineGridInfo;
	}

	public void reset() {
		bitmapBpp = 0;
		bitmapId = 0;
		nineGridInfo = new NineGridBitmapInfo();
	}

}
