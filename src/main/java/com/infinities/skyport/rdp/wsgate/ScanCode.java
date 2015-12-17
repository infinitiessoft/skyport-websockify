package com.infinities.skyport.rdp.wsgate;

public class ScanCode {

	private final static int KBDEXT = 0x0100;


	public static int RDP_SCANCODE_CODE(int _rdp_scancode) {
		return (_rdp_scancode & 0xFF);
	}

	public static boolean RDP_SCANCODE_EXTENDED(int _rdp_scancode) {
		return ((_rdp_scancode) & KBDEXT) != 0 ? true : false;
	}

	public static int MAKE_RDP_SCANCODE(int _code, boolean _extended) {
		return (((_code) & 0xFF) | ((_extended) ? KBDEXT : 0));
	}


	/*
	 * Defines for known RDP_SCANCODE protocol values. Mostly the same as the
	 * PKBDLLHOOKSTRUCT scanCode, "A hardware scan code for the key",
	 * 
	 * @msdn{ms644967}. Based @msdn{ms894073} US, @msdn{ms894072} UK,
	 * 
	 * @msdn{ms892472}
	 */

	public final static int RDP_SCANCODE_UNKNOWN = MAKE_RDP_SCANCODE(0x00, false);

	public final static int RDP_SCANCODE_ESCAPE = MAKE_RDP_SCANCODE(0x01, false); /* VK_ESCAPE */
	public final static int RDP_SCANCODE_KEY_1 = MAKE_RDP_SCANCODE(0x02, false); /* VK_KEY_1 */
	public final static int RDP_SCANCODE_KEY_2 = MAKE_RDP_SCANCODE(0x03, false); /* VK_KEY_2 */
	public final static int RDP_SCANCODE_KEY_3 = MAKE_RDP_SCANCODE(0x04, false); /* VK_KEY_3 */
	public final static int RDP_SCANCODE_KEY_4 = MAKE_RDP_SCANCODE(0x05, false); /* VK_KEY_4 */
	public final static int RDP_SCANCODE_KEY_5 = MAKE_RDP_SCANCODE(0x06, false); /* VK_KEY_5 */
	public final static int RDP_SCANCODE_KEY_6 = MAKE_RDP_SCANCODE(0x07, false); /* VK_KEY_6 */
	public final static int RDP_SCANCODE_KEY_7 = MAKE_RDP_SCANCODE(0x08, false); /* VK_KEY_7 */
	public final static int RDP_SCANCODE_KEY_8 = MAKE_RDP_SCANCODE(0x09, false); /* VK_KEY_8 */
	public final static int RDP_SCANCODE_KEY_9 = MAKE_RDP_SCANCODE(0x0A, false); /* VK_KEY_9 */
	public final static int RDP_SCANCODE_KEY_0 = MAKE_RDP_SCANCODE(0x0B, false); /* VK_KEY_0 */
	public final static int RDP_SCANCODE_OEM_MINUS = MAKE_RDP_SCANCODE(0x0C, false); /* VK_OEM_MINUS */
	public final static int RDP_SCANCODE_OEM_PLUS = MAKE_RDP_SCANCODE(0x0D, false); /* VK_OEM_PLUS */
	public final static int RDP_SCANCODE_BACKSPACE = MAKE_RDP_SCANCODE(0x0E, false); /*
																					 * VK_BACK
																					 * Backspace
																					 */
	public final static int RDP_SCANCODE_TAB = MAKE_RDP_SCANCODE(0x0F, false); /* VK_TAB */
	public final static int RDP_SCANCODE_KEY_Q = MAKE_RDP_SCANCODE(0x10, false); /* VK_KEY_Q */
	public final static int RDP_SCANCODE_KEY_W = MAKE_RDP_SCANCODE(0x11, false); /* VK_KEY_W */
	public final static int RDP_SCANCODE_KEY_E = MAKE_RDP_SCANCODE(0x12, false); /* VK_KEY_E */
	public final static int RDP_SCANCODE_KEY_R = MAKE_RDP_SCANCODE(0x13, false); /* VK_KEY_R */
	public final static int RDP_SCANCODE_KEY_T = MAKE_RDP_SCANCODE(0x14, false); /* VK_KEY_T */
	public final static int RDP_SCANCODE_KEY_Y = MAKE_RDP_SCANCODE(0x15, false); /* VK_KEY_Y */
	public final static int RDP_SCANCODE_KEY_U = MAKE_RDP_SCANCODE(0x16, false); /* VK_KEY_U */
	public final static int RDP_SCANCODE_KEY_I = MAKE_RDP_SCANCODE(0x17, false); /* VK_KEY_I */
	public final static int RDP_SCANCODE_KEY_O = MAKE_RDP_SCANCODE(0x18, false); /* VK_KEY_O */
	public final static int RDP_SCANCODE_KEY_P = MAKE_RDP_SCANCODE(0x19, false); /* VK_KEY_P */
	public final static int RDP_SCANCODE_OEM_4 = MAKE_RDP_SCANCODE(0x1A, false); /*
																				 * VK_OEM_4
																				 * '['
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_OEM_6 = MAKE_RDP_SCANCODE(0x1B, false); /*
																				 * VK_OEM_6
																				 * ']'
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_RETURN = MAKE_RDP_SCANCODE(0x1C, false); /*
																				 * VK_RETURN
																				 * Normal
																				 * Enter
																				 */
	public final static int RDP_SCANCODE_LCONTROL = MAKE_RDP_SCANCODE(0x1D, false); /* VK_LCONTROL */
	public final static int RDP_SCANCODE_KEY_A = MAKE_RDP_SCANCODE(0x1E, false); /* VK_KEY_A */
	public final static int RDP_SCANCODE_KEY_S = MAKE_RDP_SCANCODE(0x1F, false); /* VK_KEY_S */
	public final static int RDP_SCANCODE_KEY_D = MAKE_RDP_SCANCODE(0x20, false); /* VK_KEY_D */
	public final static int RDP_SCANCODE_KEY_F = MAKE_RDP_SCANCODE(0x21, false); /* VK_KEY_F */
	public final static int RDP_SCANCODE_KEY_G = MAKE_RDP_SCANCODE(0x22, false); /* VK_KEY_G */
	public final static int RDP_SCANCODE_KEY_H = MAKE_RDP_SCANCODE(0x23, false); /* VK_KEY_H */
	public final static int RDP_SCANCODE_KEY_J = MAKE_RDP_SCANCODE(0x24, false); /* VK_KEY_J */
	public final static int RDP_SCANCODE_KEY_K = MAKE_RDP_SCANCODE(0x25, false); /* VK_KEY_K */
	public final static int RDP_SCANCODE_KEY_L = MAKE_RDP_SCANCODE(0x26, false); /* VK_KEY_L */
	public final static int RDP_SCANCODE_OEM_1 = MAKE_RDP_SCANCODE(0x27, false); /*
																				 * VK_OEM_1
																				 * ';'
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_OEM_7 = MAKE_RDP_SCANCODE(0x28, false); /*
																				 * VK_OEM_7
																				 * "'"
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_OEM_3 = MAKE_RDP_SCANCODE(0x29, false); /*
																				 * VK_OEM_3
																				 * Top
																				 * left
																				 * ,
																				 * '`'
																				 * on
																				 * US
																				 * ,
																				 * JP
																				 * DBE_SBCSCHAR
																				 */
	public final static int RDP_SCANCODE_LSHIFT = MAKE_RDP_SCANCODE(0x2A, false); /* VK_LSHIFT */
	public final static int RDP_SCANCODE_OEM_5 = MAKE_RDP_SCANCODE(0x2B, false); /*
																				 * VK_OEM_5
																				 * Next
																				 * to
																				 * Enter
																				 * ,
																				 * '\'
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_KEY_Z = MAKE_RDP_SCANCODE(0x2C, false); /* VK_KEY_Z */
	public final static int RDP_SCANCODE_KEY_X = MAKE_RDP_SCANCODE(0x2D, false); /* VK_KEY_X */
	public final static int RDP_SCANCODE_KEY_C = MAKE_RDP_SCANCODE(0x2E, false); /* VK_KEY_C */
	public final static int RDP_SCANCODE_KEY_V = MAKE_RDP_SCANCODE(0x2F, false); /* VK_KEY_V */
	public final static int RDP_SCANCODE_KEY_B = MAKE_RDP_SCANCODE(0x30, false); /* VK_KEY_B */
	public final static int RDP_SCANCODE_KEY_N = MAKE_RDP_SCANCODE(0x31, false); /* VK_KEY_N */
	public final static int RDP_SCANCODE_KEY_M = MAKE_RDP_SCANCODE(0x32, false); /* VK_KEY_M */
	public final static int RDP_SCANCODE_OEM_COMMA = MAKE_RDP_SCANCODE(0x33, false); /* VK_OEM_COMMA */
	public final static int RDP_SCANCODE_OEM_PERIOD = MAKE_RDP_SCANCODE(0x34, false); /* VK_OEM_PERIOD */
	public final static int RDP_SCANCODE_OEM_2 = MAKE_RDP_SCANCODE(0x35, false); /*
																				 * VK_OEM_2
																				 * '/'
																				 * on
																				 * US
																				 */
	public final static int RDP_SCANCODE_RSHIFT = MAKE_RDP_SCANCODE(0x36, false); /* VK_RSHIFT */
	public final static int RDP_SCANCODE_MULTIPLY = MAKE_RDP_SCANCODE(0x37, false); /*
																					 * VK_MULTIPLY
																					 * Numerical
																					 */
	public final static int RDP_SCANCODE_LMENU = MAKE_RDP_SCANCODE(0x38, false); /*
																				 * VK_LMENU
																				 * Left
																				 * 'Alt'
																				 * key
																				 */
	public final static int RDP_SCANCODE_SPACE = MAKE_RDP_SCANCODE(0x39, false); /* VK_SPACE */
	public final static int RDP_SCANCODE_CAPSLOCK = MAKE_RDP_SCANCODE(0x3A, false); /*
																					 * VK_CAPITAL
																					 * 'Caps
																					 * Lock
																					 * ',
																					 * JP
																					 * DBE_ALPHANUMERIC
																					 */
	public final static int RDP_SCANCODE_F1 = MAKE_RDP_SCANCODE(0x3B, false); /* VK_F1 */
	public final static int RDP_SCANCODE_F2 = MAKE_RDP_SCANCODE(0x3C, false); /* VK_F2 */
	public final static int RDP_SCANCODE_F3 = MAKE_RDP_SCANCODE(0x3D, false); /* VK_F3 */
	public final static int RDP_SCANCODE_F4 = MAKE_RDP_SCANCODE(0x3E, false); /* VK_F4 */
	public final static int RDP_SCANCODE_F5 = MAKE_RDP_SCANCODE(0x3F, false); /* VK_F5 */
	public final static int RDP_SCANCODE_F6 = MAKE_RDP_SCANCODE(0x40, false); /* VK_F6 */
	public final static int RDP_SCANCODE_F7 = MAKE_RDP_SCANCODE(0x41, false); /* VK_F7 */
	public final static int RDP_SCANCODE_F8 = MAKE_RDP_SCANCODE(0x42, false); /* VK_F8 */
	public final static int RDP_SCANCODE_F9 = MAKE_RDP_SCANCODE(0x43, false); /* VK_F9 */
	public final static int RDP_SCANCODE_F10 = MAKE_RDP_SCANCODE(0x44, false); /* VK_F10 */
	public final static int RDP_SCANCODE_NUMLOCK = MAKE_RDP_SCANCODE(0x45, false); /* VK_NUMLOCK *//*
																									 * Note
																									 * :
																									 * when
																									 * this
																									 * seems
																									 * to
																									 * appear
																									 * in
																									 * PKBDLLHOOKSTRUCT
																									 * it
																									 * means
																									 * Pause
																									 * which
																									 * must
																									 * be
																									 * sent
																									 * as
																									 * Ctrl
																									 * +
																									 * NumLock
																									 */
	public final static int RDP_SCANCODE_SCROLLLOCK = MAKE_RDP_SCANCODE(0x46, false); /*
																					 * VK_SCROLL
																					 * 'Scroll
																					 * Lock
																					 * ',
																					 * JP
																					 * OEM_SCROLL
																					 */
	public final static int RDP_SCANCODE_NUMPAD7 = MAKE_RDP_SCANCODE(0x47, false); /* VK_NUMPAD7 */
	public final static int RDP_SCANCODE_NUMPAD8 = MAKE_RDP_SCANCODE(0x48, false); /* VK_NUMPAD8 */
	public final static int RDP_SCANCODE_NUMPAD9 = MAKE_RDP_SCANCODE(0x49, false); /* VK_NUMPAD9 */
	public final static int RDP_SCANCODE_SUBTRACT = MAKE_RDP_SCANCODE(0x4A, false); /* VK_SUBTRACT */
	public final static int RDP_SCANCODE_NUMPAD4 = MAKE_RDP_SCANCODE(0x4B, false); /* VK_NUMPAD4 */
	public final static int RDP_SCANCODE_NUMPAD5 = MAKE_RDP_SCANCODE(0x4C, false); /* VK_NUMPAD5 */
	public final static int RDP_SCANCODE_NUMPAD6 = MAKE_RDP_SCANCODE(0x4D, false); /* VK_NUMPAD6 */
	public final static int RDP_SCANCODE_ADD = MAKE_RDP_SCANCODE(0x4E, false); /* VK_ADD */
	public final static int RDP_SCANCODE_NUMPAD1 = MAKE_RDP_SCANCODE(0x4F, false); /* VK_NUMPAD1 */
	public final static int RDP_SCANCODE_NUMPAD2 = MAKE_RDP_SCANCODE(0x50, false); /* VK_NUMPAD2 */
	public final static int RDP_SCANCODE_NUMPAD3 = MAKE_RDP_SCANCODE(0x51, false); /* VK_NUMPAD3 */
	public final static int RDP_SCANCODE_NUMPAD0 = MAKE_RDP_SCANCODE(0x52, false); /* VK_NUMPAD0 */
	public final static int RDP_SCANCODE_DECIMAL = MAKE_RDP_SCANCODE(0x53, false); /*
																					 * VK_DECIMAL
																					 * Numerical
																					 * ,
																					 * '.'
																					 * on
																					 * US
																					 */
	public final static int RDP_SCANCODE_SYSREQ = MAKE_RDP_SCANCODE(0x54, false); /*
																				 * Sys
																				 * Req
																				 */
	public final static int RDP_SCANCODE_OEM_102 = MAKE_RDP_SCANCODE(0x56, false); /*
																					 * VK_OEM_102
																					 * Lower
																					 * left
																					 * '\'
																					 * on
																					 * US
																					 */
	public final static int RDP_SCANCODE_F11 = MAKE_RDP_SCANCODE(0x57, false); /* VK_F11 */
	public final static int RDP_SCANCODE_F12 = MAKE_RDP_SCANCODE(0x58, false); /* VK_F12 */
	public final static int RDP_SCANCODE_SLEEP = MAKE_RDP_SCANCODE(0x5F, false); /*
																				 * VK_SLEEP
																				 * OEM_8
																				 * on
																				 * FR
																				 * (
																				 * undocumented
																				 * ?
																				 * )
																				 */
	public final static int RDP_SCANCODE_ZOOM = MAKE_RDP_SCANCODE(0x62, false); /*
																				 * VK_ZOOM
																				 * (
																				 * undocumented
																				 * ?
																				 * )
																				 */
	public final static int RDP_SCANCODE_HELP = MAKE_RDP_SCANCODE(0x63, false); /*
																				 * VK_HELP
																				 * (
																				 * undocumented
																				 * ?
																				 * )
																				 */

	public final static int RDP_SCANCODE_F13 = MAKE_RDP_SCANCODE(0x64, false); /* VK_F13 *//*
																							 * JP
																							 * agree
																							 * ,
																							 * should
																							 * 0x7d
																							 * according
																							 * to
																							 * ms894073
																							 */
	public final static int RDP_SCANCODE_F14 = MAKE_RDP_SCANCODE(0x65, false); /* VK_F14 */
	public final static int RDP_SCANCODE_F15 = MAKE_RDP_SCANCODE(0x66, false); /* VK_F15 */
	public final static int RDP_SCANCODE_F16 = MAKE_RDP_SCANCODE(0x67, false); /* VK_F16 */
	public final static int RDP_SCANCODE_F17 = MAKE_RDP_SCANCODE(0x68, false); /* VK_F17 */
	public final static int RDP_SCANCODE_F18 = MAKE_RDP_SCANCODE(0x69, false); /* VK_F18 */
	public final static int RDP_SCANCODE_F19 = MAKE_RDP_SCANCODE(0x6A, false); /* VK_F19 */
	public final static int RDP_SCANCODE_F20 = MAKE_RDP_SCANCODE(0x6B, false); /* VK_F20 */
	public final static int RDP_SCANCODE_F21 = MAKE_RDP_SCANCODE(0x6C, false); /* VK_F21 */
	public final static int RDP_SCANCODE_F22 = MAKE_RDP_SCANCODE(0x6D, false); /* VK_F22 */
	public final static int RDP_SCANCODE_F23 = MAKE_RDP_SCANCODE(0x6E, false); /* VK_F23 *//*
																							 * JP
																							 * agree
																							 */
	public final static int RDP_SCANCODE_F24 = MAKE_RDP_SCANCODE(0x6F, false); /* VK_F24 *//*
																							 * 0x87
																							 * according
																							 * to
																							 * ms894073
																							 */

	public final static int RDP_SCANCODE_HIRAGANA = MAKE_RDP_SCANCODE(0x70, false); /*
																					 * JP
																					 * DBE_HIRAGANA
																					 */
	public final static int RDP_SCANCODE_HANJA_KANJI = MAKE_RDP_SCANCODE(0x71, false); /*
																						 * VK_HANJA
																						 * /
																						 * VK_KANJI
																						 * (
																						 * undocumented
																						 * ?
																						 * )
																						 */
	public final static int RDP_SCANCODE_KANA_HANGUL = MAKE_RDP_SCANCODE(0x72, false); /*
																						 * VK_KANA
																						 * /
																						 * VK_HANGUL
																						 * (
																						 * undocumented
																						 * ?
																						 * )
																						 */
	public final static int RDP_SCANCODE_ABNT_C1 = MAKE_RDP_SCANCODE(0x73, false); /*
																					 * VK_ABNT_C1
																					 * JP
																					 * OEM_102
																					 */
	public final static int RDP_SCANCODE_F24_JP = MAKE_RDP_SCANCODE(0x76, false); /*
																				 * JP
																				 * F24
																				 */
	public final static int RDP_SCANCODE_CONVERT_JP = MAKE_RDP_SCANCODE(0x79, false); /*
																					 * JP
																					 * VK_CONVERT
																					 */
	public final static int RDP_SCANCODE_NONCONVERT_JP = MAKE_RDP_SCANCODE(0x7B, false); /*
																						 * JP
																						 * VK_NONCONVERT
																						 */
	public final static int RDP_SCANCODE_TAB_JP = MAKE_RDP_SCANCODE(0x7C, false); /*
																				 * JP
																				 * TAB
																				 */
	public final static int RDP_SCANCODE_BACKSLASH_JP = MAKE_RDP_SCANCODE(0x7D, false); /*
																						 * JP
																						 * OEM_5
																						 * (
																						 * '\')
																						 */
	public final static int RDP_SCANCODE_ABNT_C2 = MAKE_RDP_SCANCODE(0x7E, false); /*
																					 * JP
																					 * OEM_PA2
																					 */
	public final static int RDP_SCANCODE_HANJA = MAKE_RDP_SCANCODE(0x71, false); /*
																				 * KR
																				 * VK_HANJA
																				 */
	public final static int RDP_SCANCODE_HANGUL = MAKE_RDP_SCANCODE(0x72, false); /*
																				 * KR
																				 * VK_HANGUL
																				 */

	public final static int RDP_SCANCODE_RETURN_KP = MAKE_RDP_SCANCODE(0x1C, true); /*
																					 * not
																					 * RDP_SCANCODE_RETURN
																					 * Numerical
																					 * Enter
																					 */
	public final static int RDP_SCANCODE_RCONTROL = MAKE_RDP_SCANCODE(0x1D, true); /* VK_RCONTROL */
	public final static int RDP_SCANCODE_DIVIDE = MAKE_RDP_SCANCODE(0x35, true); /*
																				 * VK_DIVIDE
																				 * Numerical
																				 */
	public final static int RDP_SCANCODE_PRINTSCREEN = MAKE_RDP_SCANCODE(0x37, true); /*
																					 * VK_EXECUTE
																					 * /
																					 * VK_PRINT
																					 * /
																					 * VK_SNAPSHOT
																					 * Prbyte
																					 * Screen
																					 */
	public final static int RDP_SCANCODE_RMENU = MAKE_RDP_SCANCODE(0x38, true); /*
																				 * VK_RMENU
																				 * Right
																				 * 'Alt'
																				 * /
																				 * 'Alt
																				 * Gr
																				 * '
																				 */
	public final static int RDP_SCANCODE_PAUSE = MAKE_RDP_SCANCODE(0x46, true); /*
																				 * VK_PAUSE
																				 * Pause
																				 * /
																				 * Break
																				 * (
																				 * Slightly
																				 * special
																				 * handling
																				 * )
																				 */
	public final static int RDP_SCANCODE_HOME = MAKE_RDP_SCANCODE(0x47, true); /* VK_HOME */
	public final static int RDP_SCANCODE_UP = MAKE_RDP_SCANCODE(0x48, true); /* VK_UP */
	public final static int RDP_SCANCODE_PRIOR = MAKE_RDP_SCANCODE(0x49, true); /*
																				 * VK_PRIOR
																				 * 'Page
																				 * Up
																				 * '
																				 */
	public final static int RDP_SCANCODE_LEFT = MAKE_RDP_SCANCODE(0x4B, true); /* VK_LEFT */
	public final static int RDP_SCANCODE_RIGHT = MAKE_RDP_SCANCODE(0x4D, true); /* VK_RIGHT */
	public final static int RDP_SCANCODE_END = MAKE_RDP_SCANCODE(0x4F, true); /* VK_END */
	public final static int RDP_SCANCODE_DOWN = MAKE_RDP_SCANCODE(0x50, true); /* VK_DOWN */
	public final static int RDP_SCANCODE_NEXT = MAKE_RDP_SCANCODE(0x51, true); /*
																				 * VK_NEXT
																				 * 'Page
																				 * Down
																				 * '
																				 */
	public final static int RDP_SCANCODE_INSERT = MAKE_RDP_SCANCODE(0x52, true); /* VK_INSERT */
	public final static int RDP_SCANCODE_DELETE = MAKE_RDP_SCANCODE(0x53, true); /* VK_DELETE */
	public final static int RDP_SCANCODE_NULL = MAKE_RDP_SCANCODE(0x54, true); /*
																				 * <
																				 * 00
																				 * >
																				 */
	public final static int RDP_SCANCODE_HELP2 = MAKE_RDP_SCANCODE(0x56, true); /*
																				 * Help
																				 * -
																				 * documented
																				 * ,
																				 * different
																				 * from
																				 * VK_HELP
																				 */
	public final static int RDP_SCANCODE_LWIN = MAKE_RDP_SCANCODE(0x5B, true); /* VK_LWIN */
	public final static int RDP_SCANCODE_RWIN = MAKE_RDP_SCANCODE(0x5C, true); /* VK_RWIN */
	public final static int RDP_SCANCODE_APPS = MAKE_RDP_SCANCODE(0x5D, true); /*
																				 * VK_APPS
																				 * Application
																				 */
	public final static int RDP_SCANCODE_POWER_JP = MAKE_RDP_SCANCODE(0x5E, true); /*
																					 * JP
																					 * POWER
																					 */
	public final static int RDP_SCANCODE_SLEEP_JP = MAKE_RDP_SCANCODE(0x5F, true); /*
																					 * JP
																					 * SLEEP
																					 */

	/*
	 * _not_ valid scancode, but this is what a windows PKBDLLHOOKSTRUCT for
	 * NumLock contains
	 */
	public final static int RDP_SCANCODE_NUMLOCK_EXTENDED = MAKE_RDP_SCANCODE(0x45, true); /*
																							 * should
																							 * be
																							 * RDP_SCANCODE_NUMLOCK
																							 */
	public final static int RDP_SCANCODE_RSHIFT_EXTENDED = MAKE_RDP_SCANCODE(0x36, true); /*
																						 * should
																						 * be
																						 * RDP_SCANCODE_RSHIFT
																						 */

	/* Audio */
	public final static int RDP_SCANCODE_VOLUME_MUTE = MAKE_RDP_SCANCODE(0x20, true); /* VK_VOLUME_MUTE */
	public final static int RDP_SCANCODE_VOLUME_DOWN = MAKE_RDP_SCANCODE(0x2E, true); /* VK_VOLUME_DOWN */
	public final static int RDP_SCANCODE_VOLUME_UP = MAKE_RDP_SCANCODE(0x30, true); /* VK_VOLUME_UP */

	/* Media */
	public final static int RDP_SCANCODE_MEDIA_NEXT_TRACK = MAKE_RDP_SCANCODE(0x19, true); /* VK_MEDIA_NEXT_TRACK */
	public final static int RDP_SCANCODE_MEDIA_PREV_TRACK = MAKE_RDP_SCANCODE(0x10, true); /* VK_MEDIA_PREV_TRACK */
	public final static int RDP_SCANCODE_MEDIA_STOP = MAKE_RDP_SCANCODE(0x24, true); /* VK_MEDIA_MEDIA_STOP */
	public final static int RDP_SCANCODE_MEDIA_PLAY_PAUSE = MAKE_RDP_SCANCODE(0x22, true); /* VK_MEDIA_MEDIA_PLAY_PAUSE */

	/* Browser functions */
	public final static int RDP_SCANCODE_BROWSER_BACK = MAKE_RDP_SCANCODE(0x6A, true); /* VK_BROWSER_BACK */
	public final static int RDP_SCANCODE_BROWSER_FORWARD = MAKE_RDP_SCANCODE(0x69, true); /* VK_BROWSER_FORWARD */
	public final static int RDP_SCANCODE_BROWSER_REFRESH = MAKE_RDP_SCANCODE(0x67, true); /* VK_BROWSER_REFRESH */
	public final static int RDP_SCANCODE_BROWSER_STOP = MAKE_RDP_SCANCODE(0x68, true); /* VK_BROWSER_STOP */
	public final static int RDP_SCANCODE_BROWSER_SEARCH = MAKE_RDP_SCANCODE(0x65, true); /* VK_BROWSER_SEARCH */
	public final static int RDP_SCANCODE_BROWSER_FAVORITES = MAKE_RDP_SCANCODE(0x66, true); /* VK_BROWSER_FAVORITES */
	public final static int RDP_SCANCODE_BROWSER_HOME = MAKE_RDP_SCANCODE(0x32, true); /* VK_BROWSER_HOME */

	/* Misc. */
	public final static int RDP_SCANCODE_LAUNCH_MAIL = MAKE_RDP_SCANCODE(0x6C, true); /* VK_LAUNCH_MAIL */

	public final static int VK_KEY_0 = 0x30; /* '0' key */
	public final static int VK_KEY_1 = 0x31; /* '1' key */
	public final static int VK_KEY_2 = 0x32; /* '2' key */
	public final static int VK_KEY_3 = 0x33; /* '3' key */
	public final static int VK_KEY_4 = 0x34; /* '4' key */
	public final static int VK_KEY_5 = 0x35; /* '5' key */
	public final static int VK_KEY_6 = 0x36; /* '6' key */
	public final static int VK_KEY_7 = 0x37; /* '7' key */
	public final static int VK_KEY_8 = 0x38; /* '8' key */
	public final static int VK_KEY_9 = 0x39; /* '9' key */
	public final static int VK_KEY_A = 0x41; /* 'A' key */
	public final static int VK_KEY_B = 0x42; /* 'B' key */
	public final static int VK_KEY_C = 0x43; /* 'C' key */
	public final static int VK_KEY_D = 0x44; /* 'D' key */
	public final static int VK_KEY_E = 0x45; /* 'E' key */
	public final static int VK_KEY_F = 0x46; /* 'F' key */
	public final static int VK_KEY_G = 0x47; /* 'G' key */
	public final static int VK_KEY_H = 0x48; /* 'H' key */
	public final static int VK_KEY_I = 0x49; /* 'I' key */
	public final static int VK_KEY_J = 0x4A; /* 'J' key */
	public final static int VK_KEY_K = 0x4B; /* 'K' key */
	public final static int VK_KEY_L = 0x4C; /* 'L' key */
	public final static int VK_KEY_M = 0x4D; /* 'M' key */
	public final static int VK_KEY_N = 0x4E; /* 'N' key */
	public final static int VK_KEY_O = 0x4F; /* 'O' key */
	public final static int VK_KEY_P = 0x50; /* 'P' key */
	public final static int VK_KEY_Q = 0x51; /* 'Q' key */
	public final static int VK_KEY_R = 0x52; /* 'R' key */
	public final static int VK_KEY_S = 0x53; /* 'S' key */
	public final static int VK_KEY_T = 0x54; /* 'T' key */
	public final static int VK_KEY_U = 0x55; /* 'U' key */
	public final static int VK_KEY_V = 0x56; /* 'V' key */
	public final static int VK_KEY_W = 0x57; /* 'W' key */
	public final static int VK_KEY_X = 0x58; /* 'X' key */
	public final static int VK_KEY_Y = 0x59; /* 'Y' key */
	public final static int VK_KEY_Z = 0x5A; /* 'Z' key */

	public final static int VK_SPACE = 0x20; /* SPACEBAR */
	public final static int VK_PRIOR = 0x21; /* PAGE UP key */
	public final static int VK_NEXT = 0x22; /* PAGE DOWN key */
	public final static int VK_END = 0x23; /* END key */
	public final static int VK_HOME = 0x24; /* HOME key */
	public final static int VK_LEFT = 0x25; /* LEFT ARROW key */
	public final static int VK_UP = 0x26; /* UP ARROW key */
	public final static int VK_RIGHT = 0x27; /* RIGHT ARROW key */
	public final static int VK_DOWN = 0x28; /* DOWN ARROW key */
	public final static int VK_SELECT = 0x29; /* SELECT key */
	public final static int VK_PRINT = 0x2A; /* PRINT key */
	public final static int VK_EXECUTE = 0x2B;/* EXECUTE key */
	public final static int VK_SNAPSHOT = 0x2C; /* PRINT SCREEN key */
	public final static int VK_INSERT = 0x2D; /* INS key */
	public final static int VK_DELETE = 0x2E; /* DEL key */
	public final static int VK_HELP = 0x2F; /* HELP key */

	public final static int VK_F1 = 0x70; /* F1 key */
	public final static int VK_F2 = 0x71; /* F2 key */
	public final static int VK_F3 = 0x72; /* F3 key */
	public final static int VK_F4 = 0x73; /* F4 key */
	public final static int VK_F5 = 0x74; /* F5 key */
	public final static int VK_F6 = 0x75; /* F6 key */
	public final static int VK_F7 = 0x76; /* F7 key */
	public final static int VK_F8 = 0x77; /* F8 key */
	public final static int VK_F9 = 0x78; /* F9 key */
	public final static int VK_F10 = 0x79; /* F10 key */
	public final static int VK_F11 = 0x7A; /* F11 key */
}
