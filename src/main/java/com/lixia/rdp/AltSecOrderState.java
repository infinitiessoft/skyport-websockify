package com.lixia.rdp;

import com.lixia.rdp.orders.altsec.CreateNineGridBitmapOrder;
import com.lixia.rdp.orders.altsec.CreateOffscreenBitmapOrder;
import com.lixia.rdp.orders.altsec.DrawGdiCacheEndOrder;
import com.lixia.rdp.orders.altsec.DrawGdiCacheFirstOrder;
import com.lixia.rdp.orders.altsec.DrawGdiCacheNextOrder;
import com.lixia.rdp.orders.altsec.DrawGdiplusEndOrder;
import com.lixia.rdp.orders.altsec.DrawGdiplusFirstOrder;
import com.lixia.rdp.orders.altsec.DrawGdiplusNextOrder;
import com.lixia.rdp.orders.altsec.FrameMarkerOrder;
import com.lixia.rdp.orders.altsec.StreamBitmapFirstOrder;
import com.lixia.rdp.orders.altsec.StreamBitmapNextOrder;
import com.lixia.rdp.orders.altsec.SwitchSurfaceOrder;

public class AltSecOrderState {

	private CreateNineGridBitmapOrder createNineGridBitmapOrder;
	private CreateOffscreenBitmapOrder createOffscreenBitmapOrder;
	private DrawGdiCacheEndOrder drawGdiCacheEndOrder;
	private DrawGdiCacheFirstOrder drawGdiCacheFirstOrder;
	private DrawGdiCacheNextOrder DrawGdiCacheNextOrder;
	private DrawGdiplusEndOrder DrawGdiplusEndOrder;
	private DrawGdiplusFirstOrder DrawGdiplusFirstOrder;
	private DrawGdiplusNextOrder DrawGdiplusNextOrder;
	private FrameMarkerOrder frameMarkerOrder;
	private StreamBitmapFirstOrder streamBitmapFirstOrder;
	private StreamBitmapNextOrder streamBitmapNextOrder;
	private SwitchSurfaceOrder switchSurfaceOrder;


	public AltSecOrderState() {
		super();
		this.createNineGridBitmapOrder = new CreateNineGridBitmapOrder();
		this.createOffscreenBitmapOrder = new CreateOffscreenBitmapOrder();
		this.drawGdiCacheEndOrder = new DrawGdiCacheEndOrder();
		this.drawGdiCacheFirstOrder = new DrawGdiCacheFirstOrder();
		DrawGdiCacheNextOrder = new DrawGdiCacheNextOrder();
		DrawGdiplusEndOrder = new DrawGdiplusEndOrder();
		DrawGdiplusFirstOrder = new DrawGdiplusFirstOrder();
		DrawGdiplusNextOrder = new DrawGdiplusNextOrder();
		this.frameMarkerOrder = new FrameMarkerOrder();
		this.streamBitmapFirstOrder = new StreamBitmapFirstOrder();
		this.streamBitmapNextOrder = new StreamBitmapNextOrder();
		this.switchSurfaceOrder = new SwitchSurfaceOrder();
	}

	public CreateNineGridBitmapOrder getCreateNineGridBitmapOrder() {
		return createNineGridBitmapOrder;
	}

	public void setCreateNineGridBitmapOrder(CreateNineGridBitmapOrder createNineGridBitmapOrder) {
		this.createNineGridBitmapOrder = createNineGridBitmapOrder;
	}

	public CreateOffscreenBitmapOrder getCreateOffscreenBitmapOrder() {
		return createOffscreenBitmapOrder;
	}

	public void setCreateOffscreenBitmapOrder(CreateOffscreenBitmapOrder createOffscreenBitmapOrder) {
		this.createOffscreenBitmapOrder = createOffscreenBitmapOrder;
	}

	public DrawGdiCacheEndOrder getDrawGdiCacheEndOrder() {
		return drawGdiCacheEndOrder;
	}

	public void setDrawGdiCacheEndOrder(DrawGdiCacheEndOrder drawGdiCacheEndOrder) {
		this.drawGdiCacheEndOrder = drawGdiCacheEndOrder;
	}

	public DrawGdiCacheFirstOrder getDrawGdiCacheFirstOrder() {
		return drawGdiCacheFirstOrder;
	}

	public void setDrawGdiCacheFirstOrder(DrawGdiCacheFirstOrder drawGdiCacheFirstOrder) {
		this.drawGdiCacheFirstOrder = drawGdiCacheFirstOrder;
	}

	public DrawGdiCacheNextOrder getDrawGdiCacheNextOrder() {
		return DrawGdiCacheNextOrder;
	}

	public void setDrawGdiCacheNextOrder(DrawGdiCacheNextOrder drawGdiCacheNextOrder) {
		DrawGdiCacheNextOrder = drawGdiCacheNextOrder;
	}

	public DrawGdiplusEndOrder getDrawGdiplusEndOrder() {
		return DrawGdiplusEndOrder;
	}

	public void setDrawGdiplusEndOrder(DrawGdiplusEndOrder drawGdiplusEndOrder) {
		DrawGdiplusEndOrder = drawGdiplusEndOrder;
	}

	public DrawGdiplusFirstOrder getDrawGdiplusFirstOrder() {
		return DrawGdiplusFirstOrder;
	}

	public void setDrawGdiplusFirstOrder(DrawGdiplusFirstOrder drawGdiplusFirstOrder) {
		DrawGdiplusFirstOrder = drawGdiplusFirstOrder;
	}

	public DrawGdiplusNextOrder getDrawGdiplusNextOrder() {
		return DrawGdiplusNextOrder;
	}

	public void setDrawGdiplusNextOrder(DrawGdiplusNextOrder drawGdiplusNextOrder) {
		DrawGdiplusNextOrder = drawGdiplusNextOrder;
	}

	public FrameMarkerOrder getFrameMarkerOrder() {
		return frameMarkerOrder;
	}

	public void setFrameMarkerOrder(FrameMarkerOrder frameMarkerOrder) {
		this.frameMarkerOrder = frameMarkerOrder;
	}

	public StreamBitmapFirstOrder getStreamBitmapFirstOrder() {
		return streamBitmapFirstOrder;
	}

	public void setStreamBitmapFirstOrder(StreamBitmapFirstOrder streamBitmapFirstOrder) {
		this.streamBitmapFirstOrder = streamBitmapFirstOrder;
	}

	public StreamBitmapNextOrder getStreamBitmapNextOrder() {
		return streamBitmapNextOrder;
	}

	public void setStreamBitmapNextOrder(StreamBitmapNextOrder streamBitmapNextOrder) {
		this.streamBitmapNextOrder = streamBitmapNextOrder;
	}

	public SwitchSurfaceOrder getSwitchSurfaceOrder() {
		return switchSurfaceOrder;
	}

	public void setSwitchSurfaceOrder(SwitchSurfaceOrder switchSurfaceOrder) {
		this.switchSurfaceOrder = switchSurfaceOrder;
	}

	public void reset() {
		this.createNineGridBitmapOrder.reset();
		this.createOffscreenBitmapOrder.reset();
		this.drawGdiCacheEndOrder.reset();
		this.drawGdiCacheFirstOrder.reset();
		DrawGdiCacheNextOrder.reset();
		DrawGdiplusEndOrder.reset();
		DrawGdiplusFirstOrder.reset();
		DrawGdiplusNextOrder.reset();
		this.frameMarkerOrder.reset();
		this.streamBitmapFirstOrder.reset();
		this.streamBitmapNextOrder.reset();
		this.switchSurfaceOrder.reset();
	}

}
