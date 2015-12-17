package com.lixia.rdp.orders.altsec;

public class CreateOffscreenBitmapOrder {

	private int id;
	private int cx;
	private int cy;
	private OffscreenDeleteList deleteList = new OffscreenDeleteList();


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCX() {
		return cx;
	}

	public void setCX(int cx) {
		this.cx = cx;
	}

	public int getCY() {
		return cy;
	}

	public void setCY(int cy) {
		this.cy = cy;
	}

	public OffscreenDeleteList getDeleteList() {
		return deleteList;
	}

	public void setDeleteList(OffscreenDeleteList deleteList) {
		this.deleteList = deleteList;
	}


	public class OffscreenDeleteList {

		private int sIndices;
		private int cIndices;
		private int[] indices;


		public int getSIndices() {
			return sIndices;
		}

		public void setSIndices(int sIndices) {
			this.sIndices = sIndices;
		}

		public int getCIndices() {
			return cIndices;
		}

		public void setCIndices(int cIndices) {
			this.cIndices = cIndices;
		}

		public int[] getIndices() {
			return indices;
		}

		public void setIndices(int[] indices) {
			this.indices = indices;
		}

	}


	public void reset() {
		id = 0;
		cx = 0;
		cy = 0;
		deleteList = new OffscreenDeleteList();
	}

}
