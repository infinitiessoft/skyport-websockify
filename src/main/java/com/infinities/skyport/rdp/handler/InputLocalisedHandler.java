/*******************************************************************************
 * Copyright 2015 InfinitiesSoft Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.infinities.skyport.rdp.handler;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Vector;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.infinities.skyport.rdp.custom.RDPSession;
import com.infinities.skyport.rdp.custom.RDPUtils;
import com.infinities.skyport.rdp.wsgate.RDP;
import com.infinities.skyport.rdp.wsgate.ScanCode;
import com.lixia.rdp.RdesktopException;
import com.lixia.rdp.crypto.CryptoException;

public class InputLocalisedHandler extends SimpleChannelDownstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(InputLocalisedHandler.class);
	protected static boolean capsLockOn = false;
	protected static boolean numLockOn = false;
	protected static boolean scrollLockOn = false;

	protected static boolean serverAltDown = false;
	protected static boolean altDown = false;
	protected static boolean ctrlDown = false;

	protected static long last_mousemove = 0;

	// Using this flag value (0x0001) seems to do nothing, and after running
	// through other possible values, the RIGHT flag does not appear to be
	// implemented
	protected static final int KBD_FLAG_RIGHT = 0x0001;
	protected static final int KBD_FLAG_EXT = 0x0100;

	// QUIET flag is actually as below (not 0x1000 as in rdesktop)
	protected static final int KBD_FLAG_QUIET = 0x200;
	protected static final int KBD_FLAG_DOWN = 0x4000;
	protected static final int KBD_FLAG_UP = 0x8000;

	protected static final int RDP_KEYPRESS = 0;
	protected static final int RDP_KEYRELEASE = KBD_FLAG_DOWN | KBD_FLAG_UP;
	protected static final int MOUSE_FLAG_MOVE = 0x0800;

	protected static final int MOUSE_FLAG_BUTTON1 = 0x1000;
	protected static final int MOUSE_FLAG_BUTTON2 = 0x2000;
	protected static final int MOUSE_FLAG_BUTTON3 = 0x4000;

	protected static final int MOUSE_FLAG_BUTTON4 = 0x0280; // wheel up -
															// rdesktop 1.2.0
	protected static final int MOUSE_FLAG_BUTTON5 = 0x0380; // wheel down -
															// rdesktop 1.2.0
	protected static final int MOUSE_FLAG_DOWN = 0x8000;

	protected static final int RDP_INPUT_SYNCHRONIZE = 0;
	protected static final int RDP_INPUT_CODEPOINT = 1;
	protected static final int RDP_INPUT_VIRTKEY = 2;
	protected static final int RDP_INPUT_SCANCODE = 4;
	protected static final int RDP_INPUT_MOUSE = 0x8001;

	protected static int time = 0;

	public KeyEvent lastKeyEvent = null;
	public boolean modifiersValid = false;
	public boolean keyDownWindows = false;
	public static boolean key_Code = true;
	public static int keyCode = 0xfffffff;
	protected Vector<Integer> pressedKeys;
	// private KeyCodeFileBased newKeyMapper = null;
	private RDPSession session;

	private static final int WSOP_CS_MOUSE = 0;
	private static final int WSOP_CS_KUPDOWN = 1;
	private static final int WOSP_CS_KPRESS = 2;
	private static final int WSOP_CS_SPECIALCOMB = 3;

	// private static final int WSOP_CS_KUP = 0;
	// private static final int WSOP_CS_KDOWN = 1;
	private static final int KBD_FLAGS_EXTENDED = 0x0100;
	private static final int KBD_FLAGS_DOWN = 0x4000;
	private static final int KBD_FLAGS_RELEASE = 0x8000;


	public InputLocalisedHandler(RDPSession session) {
		this.session = session;
		pressedKeys = new Vector<Integer>();
	}

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		logger.debug("receive event");
		// Log all channel state changes.
		if (!(evt instanceof MessageEvent)) {
			context.sendDownstream(evt);
			return;
		}

		MessageEvent e = (MessageEvent) evt;
		if (!(e.getMessage() instanceof ChannelBuffer)) {
			logger.debug("{}, {}", new Object[] { e.getMessage().getClass(), e.getMessage().toString() });
			context.sendDownstream(evt);
			return;
		}

		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		byte[] buffer = msg.array();
		logger.debug("buffer size:{}", buffer.length);
		String decoded = new String(buffer);
		if (!Strings.isNullOrEmpty(decoded.trim())) {
			String[] array = decoded.split(",");
			logger.debug("array size:{}", array.length);

			handleClientEvent(array, session, context);
		}
		// logger.debug("handledownstream: {}", new Object[] { new
		// String(msg.array()) });
		// context.sendDownstream(evt);

	}

	private void handleClientEvent(String[] buffer, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {
		logger.debug("receive event:{}", buffer[0]);
		switch (Integer.parseInt(buffer[0])) {
		case WSOP_CS_MOUSE: // mouse event
			handleMouseEvent(buffer, session, context);
			break;
		case WSOP_CS_KUPDOWN: // key up,down event
			handleKeyEvent(buffer, session, context);
			break;
		case WOSP_CS_KPRESS: // key press event
			// handleKeyEvent(buffer);
			break;
		case WSOP_CS_SPECIALCOMB: // key combination event
			// handleKeyEvent(buffer);
			break;
		default:
			logger.warn("unimplemented command:{}", buffer[0]);
			break;
		}
	}

	private void handleKeyEvent(String[] buffer, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {
		int action = Integer.decode(buffer[1].trim());
		int code = Integer.parseInt(buffer[2].trim());
		code = ScanCode.RDP_SCANCODE_CODE(code);
		code = RDP.ASCII_TO_SCANCODE[code];
		int flag = ScanCode.RDP_SCANCODE_EXTENDED(code) ? KBD_FLAGS_EXTENDED : 0;
		logger.debug("action:{}, code:{}", new Object[] { action, code });
		flag = (action != 0 ? KBD_FLAGS_DOWN : KBD_FLAGS_RELEASE) | flag;
		sendKeyboardEvent(flag, code, session, context);
	}

	private void sendKeyboardEvent(int flag, int code, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {
		long t = getTime();
		RDPUtils.sendInput((int) t, RDP_INPUT_SCANCODE, flag, code, 0, session, context);
	}

	private void handleMouseEvent(String[] buffer, RDPSession session, ChannelHandlerContext context)
			throws RdesktopException, IOException, CryptoException {
		int device_flag = Integer.decode(buffer[1].trim());
		int x = Integer.parseInt(buffer[2].trim());
		int y = Integer.parseInt(buffer[3].trim());
		RDPUtils.sendInput(time, RDP_INPUT_MOUSE, device_flag, x, y, session, context);
	}

	// /**
	// * Handle a keyPressed event, sending any relevant keypresses to the
	// server
	// */
	// public void keyPressed(KeyEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// lastKeyEvent = e;
	// modifiersValid = true;
	// long time = getTime();
	//
	// // Some java versions have keys that don't generate keyPresses -
	// // here we add the key so we can later check if it happened
	// pressedKeys.addElement(new Integer(e.getKeyCode()));
	//
	// logger.debug("PRESSED keychar='" + e.getKeyChar() + "' keycode=0x" +
	// Integer.toHexString(e.getKeyCode()) + " char='"
	// + ((char) e.getKeyCode()) + "'");
	//
	// try {
	// if (!handleSpecialKeys(time, e, true, session, context)) {
	// // send
	// // logger.debug("key strokes: {}",
	// // newKeyMapper.getKeyStrokes(e));
	// sendKeyPresses(newKeyMapper.getKeyStrokes(e, session, context), session,
	// context);
	// }
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// // sendScancode(time, RDP_KEYPRESS, keys.getScancode(e));
	// }
	//
	// // private void hideFrameMenu() {
	// // if (frame != null)
	// // frame.hideMenu();
	// // }
	//
	// /**
	// * Handle a keyTyped event, sending any relevant keypresses to the server
	// */
	// public void keyTyped(KeyEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// // hideFrameMenu();
	// // System.out.println("TYPED keychar='" + e.getKeyChar() +
	// // "' keycode=0x"
	// // + Integer.toHexString(e.getKeyCode()) + " char='"
	// // + ((char) e.getKeyCode()) + "'");
	// lastKeyEvent = e;
	// modifiersValid = true;
	// long time = getTime();
	//
	// // Some java versions have keys that don't generate keyPresses -
	// // here we add the key so we can later check if it happened
	// pressedKeys.addElement(new Integer(e.getKeyCode()));
	//
	// logger.debug("TYPED keychar='" + e.getKeyChar() + "' keycode=0x" +
	// Integer.toHexString(e.getKeyCode()) + " char='"
	// + ((char) e.getKeyCode()) + "' modifier='" + e.getModifiers() + "'");
	//
	// try {
	// if (!handleSpecialKeys(time, e, true, session, context))
	// sendKeyPresses(newKeyMapper.getKeyStrokes(e, session, context), session,
	// context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// // sendScancode(time, RDP_KEYPRESS, keys.getScancode(e));
	// }
	//
	// /**
	// * Handle a keyReleased event, sending any relevent key events to the
	// server
	// */
	// public void keyReleased(KeyEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// // hideFrameMenu();
	// Integer keycode = new Integer(e.getKeyCode());
	// CustomInputJPanel.keyCode = keycode;
	//
	// if (!pressedKeys.contains(keycode)) {
	// this.keyPressed(e, session, context);
	// }
	//
	// pressedKeys.removeElement(keycode);
	//
	// lastKeyEvent = e;
	// modifiersValid = true;
	// long time = getTime();
	//
	// logger.debug("RELEASED keychar='" + e.getKeyChar() + "' keycode=0x" +
	// Integer.toHexString(e.getKeyCode())
	// + " char='" + ((char) e.getKeyCode()) + "'");
	// try {
	// if (!handleSpecialKeys(time, e, false, session, context)) {
	// String keyStroke = newKeyMapper.getKeyStrokes(e, session, context);
	// logger.debug("release key storkes: {}", keyStroke);
	// sendKeyPresses(keyStroke, session, context);
	// }
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	//
	// }
	//
	// public void mousePressed(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// // if (e.getY() != 0 && frame != null)
	// // frame.hideMenu();
	// /*
	// * if(Position.isFrameArea(e.getX(), e.getY())){ o_x = e.getX(); o_y =
	// * e.getY(); drag_flag = true; } else{ drag_flag = false; }
	// */
	// int time = getTime();
	// if ((e.getModifiers() & InputEvent.BUTTON1_MASK) ==
	// InputEvent.BUTTON1_MASK) {
	// // logger.debug("Mouse Button 1 Pressed.");
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1 |
	// MOUSE_FLAG_DOWN, e.getX(), e.getY(), session,
	// context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
	// InputEvent.BUTTON3_MASK) {
	// // logger.debug("Mouse Button 3 Pressed.");
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2 |
	// MOUSE_FLAG_DOWN, e.getX(), e.getY(), session,
	// context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// } else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
	// InputEvent.BUTTON2_MASK) {
	// // logger.debug("Middle Mouse Button Pressed.");
	// try {
	// middleButtonPressed(e, session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// }
	// }
	//
	// public void mouseReleased(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// int time = getTime();
	// if ((e.getModifiers() & InputEvent.BUTTON1_MASK) ==
	// InputEvent.BUTTON1_MASK) {
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON1, e.getX(),
	// e.getY(), session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// } else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
	// InputEvent.BUTTON3_MASK) {
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON2, e.getX(),
	// e.getY(), session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// } else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
	// InputEvent.BUTTON2_MASK) {
	// try {
	// middleButtonReleased(e, session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// }
	// }
	//
	// public void mouseMoved(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// int time = getTime();
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(),
	// e.getY(), session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// }
	//
	// public void mouseDragged(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// int time = getTime();
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_MOVE, e.getX(),
	// e.getY(), session, context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// }

	/**
	 * Retrieve the next "timestamp", by incrementing previous stamp (up to the
	 * maximum value of an integer, at which the timestamp is reverted to 1)
	 * 
	 * @return New timestamp value
	 */
	public static int getTime() {
		time++;
		if (time == Integer.MAX_VALUE)
			time = 1;
		return time;
	}

	// /**
	// * Deal with modifier keys as control, alt or caps lock
	// *
	// * @param time
	// * Time stamp for key event
	// * @param e
	// * Key event to check for special keys
	// * @param pressed
	// * True if key was pressed, false if released
	// * @return
	// * @throws Exception
	// */
	// public boolean handleSpecialKeys(long time, KeyEvent e, boolean pressed,
	// RDPSession session,
	// ChannelHandlerContext context) throws Exception {
	// if (handleShortcutKeys(time, e, pressed, session, context))
	// return true;
	//
	// switch (e.getKeyCode()) {
	// case KeyEvent.VK_CONTROL:
	// ctrlDown = pressed;
	// return false;
	// case KeyEvent.VK_ALT:
	// altDown = pressed;
	// return false;
	// case KeyEvent.VK_CAPS_LOCK:
	// if (pressed && Options.caps_sends_up_and_down)
	// capsLockOn = !capsLockOn;
	// if (!Options.caps_sends_up_and_down) {
	// if (pressed)
	// capsLockOn = true;
	// else
	// capsLockOn = false;
	// }
	// return false;
	// case KeyEvent.VK_NUM_LOCK:
	// if (pressed)
	// numLockOn = !numLockOn;
	// return false;
	// case KeyEvent.VK_SCROLL_LOCK:
	// if (pressed)
	// scrollLockOn = !scrollLockOn;
	// return false;
	// case KeyEvent.VK_PAUSE: // untested
	// if (pressed) { // E1 1D 45 E1 9D C5
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0,
	// session, context);
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x1d, 0,
	// session, context);
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x45, 0,
	// session, context);
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xe1, 0,
	// session, context);
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0x9d, 0,
	// session, context);
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYPRESS, 0xc5, 0,
	// session, context);
	// } else { // release left ctrl
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, RDP_KEYRELEASE, 0x1d,
	// 0, session, context);
	// }
	// break;
	//
	// // Removed, as java on MacOS send the option key as VK_META
	// /*
	// * case KeyEvent.VK_META: // Windows key logger.debug("Windows key
	// * received"); if(pressed){ sendScancode(time, RDP_KEYPRESS, 0x1d); //
	// * left ctrl sendScancode(time, RDP_KEYPRESS, 0x01); // escape } else{
	// * sendScancode(time, RDP_KEYRELEASE, 0x01); // escape
	// * sendScancode(time, RDP_KEYRELEASE, 0x1d); // left ctrl } break;
	// */
	//
	// // haven't found a way to detect BREAK key in java - VK_BREAK doesn't
	// // exist
	// /*
	// * case KeyEvent.VK_BREAK: if(pressed){
	// * sendScancode(time,RDP_KEYPRESS,(KeyCode.SCANCODE_EXTENDED | 0x46));
	// * sendScancode(time,RDP_KEYPRESS,(KeyCode.SCANCODE_EXTENDED | 0xc6)); }
	// * // do nothing on release break;
	// */
	// default:
	// return false; // not handled - use sendScancode instead
	// }
	// return true; // handled - no need to use sendScancode
	// }
	//
	// /**
	// * Send a sequence of key actions to the server
	// *
	// * @param pressSequence
	// * String representing a sequence of key actions. Actions are
	// * represented as a pair of consecutive characters, the first
	// * character's value (cast to integer) being the scancode to
	// * send, the second (cast to integer) of the pair representing
	// * the action (0 == UP, 1 == DOWN, 2 == QUIET UP, 3 == QUIET
	// * DOWN).
	// */
	// public void sendKeyPresses(String pressSequence, RDPSession session,
	// ChannelHandlerContext context) {
	// try {
	// String debugString = "Sending keypresses: " + pressSequence;
	// logger.debug("{}, size: {}", new Object[] { debugString,
	// pressSequence.length() });
	// for (int i = 0; i < pressSequence.length(); i += 2) {
	// int scancode = (int) pressSequence.charAt(i);
	// int action = (int) pressSequence.charAt(i + 1);
	// int flags = 0;
	//
	// if (action == KeyCode_FileBased.UP)
	// flags = RDP_KEYRELEASE;
	// else if (action == KeyCode_FileBased.DOWN)
	// flags = RDP_KEYPRESS;
	// else if (action == KeyCode_FileBased.QUIETUP)
	// flags = RDP_KEYRELEASE | KBD_FLAG_QUIET;
	// else if (action == KeyCode_FileBased.QUIETDOWN)
	// flags = RDP_KEYPRESS | KBD_FLAG_QUIET;
	//
	// long t = getTime();
	//
	// debugString += "(0x" + Integer.toHexString(scancode) + ", "
	// + ((action == KeyCode_FileBased.UP || action ==
	// KeyCode_FileBased.QUIETUP) ? "up" : "down")
	// + ((flags & KBD_FLAG_QUIET) != 0 ? " quiet" : "") + " at " + t + ")";
	// logger.debug("debug: {}", debugString);
	// // System.out.println("����"+debugString);
	// sendScancode(t, flags, scancode, session, context);
	// }
	// } catch (Exception ex) {
	// ex.printStackTrace();
	// return;
	// }
	// }
	//
	// /**
	// * Send a keyboard event to the server
	// *
	// * @param time
	// * Time stamp to identify this event
	// * @param flags
	// * Flags defining the nature of the event (eg:
	// * press/release/quiet/extended)
	// * @param scancode
	// * Scancode value identifying the key in question
	// * @throws Exception
	// */
	// public void sendScancode(long time, int flags, int scancode, RDPSession
	// session, ChannelHandlerContext context)
	// throws Exception {
	//
	// if (scancode == 0x38) { // be careful with alt
	// if ((flags & RDP_KEYRELEASE) != 0) {
	// // logger.info("Alt release, serverAltDown = " + serverAltDown);
	// serverAltDown = false;
	// }
	// if ((flags == RDP_KEYPRESS)) {
	// // logger.info("Alt press, serverAltDown = " + serverAltDown);
	// serverAltDown = true;
	// }
	// }
	//
	// if ((scancode & KeyCode.SCANCODE_EXTENDED) != 0) {
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, flags | KBD_FLAG_EXT,
	// scancode & ~KeyCode.SCANCODE_EXTENDED,
	// 0, session, context);
	// } else
	// RDPUtils.sendInput((int) time, RDP_INPUT_SCANCODE, flags, scancode, 0,
	// session, context);
	// }
	//
	// /**
	// * Act on any keyboard shortcuts that a specified KeyEvent may describe
	// *
	// * @param time
	// * Time stamp for event to send to server
	// * @param e
	// * Keyboard event to be checked for shortcut keys
	// * @param pressed
	// * True if key was pressed, false if released
	// * @return True if a shortcut key combination was detected and acted upon,
	// * false otherwise
	// * @throws Exception
	// */
	// public boolean handleShortcutKeys(long time, KeyEvent e, boolean pressed,
	// RDPSession session,
	// ChannelHandlerContext context) throws Exception {
	// if (!e.isAltDown())
	// return false;
	//
	// if (!altDown)
	// return false; // all of the below have ALT on
	//
	// switch (e.getKeyCode()) {
	//
	// /*
	// * case KeyEvent.VK_M: if(pressed) ((RdesktopFrame_Localised)
	// * canvas.getParent()).toggleMenu(); break;
	// */
	//
	// case KeyEvent.VK_ENTER:
	// sendScancode(time, RDP_KEYRELEASE, 0x38, session, context);
	// altDown = false;
	// // @deprecated ((RdesktopJPanel_Localised)
	// // canvas.getParent()).toggleFullScreen();
	// break;
	//
	// /*
	// * The below case block handles "real" ALT+TAB events. Once the TAB in
	// * an ALT+TAB combination has been pressed, the TAB is sent to the
	// * server with the quiet flag on, as is the subsequent ALT-up.
	// *
	// * This ensures that the initial ALT press is "undone" by the server.
	// *
	// * --- Tom Elliott, 7/04/05
	// */
	//
	// case KeyEvent.VK_TAB: // Alt+Tab received, quiet combination
	//
	// sendScancode(time, (pressed ? RDP_KEYPRESS : RDP_KEYRELEASE) |
	// KBD_FLAG_QUIET, 0x0f, session, context);
	// if (!pressed) {
	// sendScancode(time, RDP_KEYRELEASE | KBD_FLAG_QUIET, 0x38, session,
	// context); // Release
	// // Alt
	// }
	//
	// if (pressed)
	// logger.debug("Alt + Tab pressed, ignoring, releasing tab");
	// break;
	// case KeyEvent.VK_PAGE_UP: // Alt + PgUp = Alt-Tab
	// sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x0f,
	// session, context); // TAB
	// if (pressed)
	// logger.debug("shortcut pressed: sent ALT+TAB");
	// break;
	// case KeyEvent.VK_PAGE_DOWN: // Alt + PgDown = Alt-Shift-Tab
	// if (pressed) {
	// sendScancode(time, RDP_KEYPRESS, 0x2a, session, context); // Shift
	// sendScancode(time, RDP_KEYPRESS, 0x0f, session, context); // TAB
	// logger.debug("shortcut pressed: sent ALT+SHIFT+TAB");
	// } else {
	// sendScancode(time, RDP_KEYRELEASE, 0x0f, session, context); // TAB
	// sendScancode(time, RDP_KEYRELEASE, 0x2a, session, context); // Shift
	// }
	//
	// break;
	// case KeyEvent.VK_INSERT: // Alt + Insert = Alt + Esc
	// sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x01,
	// session, context); // ESC
	// if (pressed)
	// logger.debug("shortcut pressed: sent ALT+ESC");
	// break;
	// case KeyEvent.VK_HOME: // Alt + Home = Ctrl + Esc (Start)
	// if (pressed) {
	// sendScancode(time, RDP_KEYRELEASE, 0x38, session, context); // ALT
	// sendScancode(time, RDP_KEYPRESS, 0x1d, session, context); // left
	// // Ctrl
	// sendScancode(time, RDP_KEYPRESS, 0x01, session, context); // Esc
	// logger.debug("shortcut pressed: sent CTRL+ESC (Start)");
	//
	// } else {
	// sendScancode(time, RDP_KEYRELEASE, 0x01, session, context); // escape
	// sendScancode(time, RDP_KEYRELEASE, 0x1d, session, context); // left
	// // ctrl
	// // sendScancode(time,RDP_KEYPRESS,0x38); // ALT
	// }
	//
	// break;
	// case KeyEvent.VK_END: // Ctrl+Alt+End = Ctrl+Alt+Del
	// if (ctrlDown) {
	// sendScancode(time, pressed ? RDP_KEYPRESS : RDP_KEYRELEASE, 0x53 |
	// KeyCode.SCANCODE_EXTENDED, session,
	// context); // DEL
	// if (pressed)
	// logger.debug("shortcut pressed: sent CTRL+ALT+DEL");
	// }
	// break;
	// case KeyEvent.VK_DELETE: // Alt + Delete = Menu
	// if (pressed) {
	// sendScancode(time, RDP_KEYRELEASE, 0x38, session, context); // ALT
	// // need to do another press and release to shift focus from
	// // to/from menu bar
	// sendScancode(time, RDP_KEYPRESS, 0x38, session, context); // ALT
	// sendScancode(time, RDP_KEYRELEASE, 0x38, session, context); // ALT
	// sendScancode(time, RDP_KEYPRESS, 0x5d | KeyCode.SCANCODE_EXTENDED,
	// session, context); // Menu
	// logger.debug("shortcut pressed: sent MENU");
	// } else {
	// sendScancode(time, RDP_KEYRELEASE, 0x5d | KeyCode.SCANCODE_EXTENDED,
	// session, context); // Menu
	// // sendScancode(time,RDP_KEYPRESS,0x38); // ALT
	// }
	// break;
	// case KeyEvent.VK_SUBTRACT: // Ctrl + Alt + Minus (on NUM KEYPAD) =
	// // Alt+PrtSc
	// if (ctrlDown) {
	// if (pressed) {
	// sendScancode(time, RDP_KEYRELEASE, 0x1d, session, context); // Ctrl
	// sendScancode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED,
	// session, context); // PrtSc
	// logger.debug("shortcut pressed: sent ALT+PRTSC");
	// } else {
	// sendScancode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED,
	// session, context); // PrtSc
	// sendScancode(time, RDP_KEYPRESS, 0x1d, session, context); // Ctrl
	// }
	// }
	// break;
	// case KeyEvent.VK_ADD: // Ctrl + ALt + Plus (on NUM KEYPAD) = PrtSc
	// case KeyEvent.VK_EQUALS: // for laptops that can't do Ctrl-Alt+Plus
	// if (ctrlDown) {
	// if (pressed) {
	// sendScancode(time, RDP_KEYRELEASE, 0x38, session, context); // Alt
	// sendScancode(time, RDP_KEYRELEASE, 0x1d, session, context); // Ctrl
	// sendScancode(time, RDP_KEYPRESS, 0x37 | KeyCode.SCANCODE_EXTENDED,
	// session, context); // PrtSc
	// logger.debug("shortcut pressed: sent PRTSC");
	// } else {
	// sendScancode(time, RDP_KEYRELEASE, 0x37 | KeyCode.SCANCODE_EXTENDED,
	// session, context); // PrtSc
	// sendScancode(time, RDP_KEYPRESS, 0x1d, session, context); // Ctrl
	// sendScancode(time, RDP_KEYPRESS, 0x38, session, context); // Alt
	// }
	// }
	// break;
	// default:
	// return false;
	// }
	// return true;
	// }
	//
	// /**
	// * Handle release of the middle mouse button, sending relevent event data
	// to
	// * the server
	// *
	// * @param e
	// * MouseEvent detailing circumstances under which middle button
	// * was released
	// * @throws Exception
	// */
	// protected void middleButtonReleased(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) throws Exception {
	// /* if (!Options.paste_hack || !ctrlDown) */
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3, e.getX(),
	// e.getY(), session, context);
	// }
	//
	// /**
	// * Handle pressing of the middle mouse button, sending relevent event data
	// * to the server
	// *
	// * @param e
	// * MouseEvent detailing circumstances under which middle button
	// * was pressed
	// * @throws Exception
	// */
	// protected void middleButtonPressed(MouseEvent e, RDPSession session,
	// ChannelHandlerContext context) throws Exception {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON3 |
	// MOUSE_FLAG_DOWN, e.getX(), e.getY(), session, context);
	// }
	//
	// public void mouseWheelMoved(MouseWheelEvent e, RDPSession session,
	// ChannelHandlerContext context) {
	// int time = getTime();
	// // if(logger.isInfoEnabled()) logger.info("mousePressed at "+time);
	// if (e.getWheelRotation() < 0) { // up
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON4 |
	// MOUSE_FLAG_DOWN, e.getX(), e.getY(), session,
	// context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// } else { // down
	// try {
	// RDPUtils.sendInput(time, RDP_INPUT_MOUSE, MOUSE_FLAG_BUTTON5 |
	// MOUSE_FLAG_DOWN, e.getX(), e.getY(), session,
	// context);
	// } catch (Exception e1) {
	// throw new RuntimeException(e1);
	// }
	// }
	// }
}
