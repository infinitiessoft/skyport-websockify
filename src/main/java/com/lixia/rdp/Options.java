/* Options.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:14 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Global static storage of user-definable options
 */
package com.lixia.rdp;

//import java.awt.image.DirectColorModel;

public class Options {

	public static final int DIRECT_BITMAP_DECOMPRESSION = 0;
	public static final int BUFFEREDIMAGE_BITMAP_DECOMPRESSION = 1;
	public static final int INTEGER_BITMAP_DECOMPRESSION = 2;

	private int bitmap_decompression_store = INTEGER_BITMAP_DECOMPRESSION;

	private boolean low_latency = true; // disables bandwidth saving tcp
										// packets
	private int keylayout = 1033;// 0x809; // UK by default
	private String username = "root"; // -u username
	private String domain = ""; // -d domain
	private String password = ""; // -p password
	private String hostname = ""; // -n hostname
	private String command = ""; // -s command
	private String directory = ""; // -d directory
	private String windowTitle = "Elusiva Everywhere"; // -T windowTitle
	private int width = 800; // -g widthxheight
	private int height = 600; // -g widthxheight
	private int port = 3389; // -t port
	private boolean fullscreen = false;
	private boolean built_in_licence = false;

	// setting
	private boolean autologin = false;
	private boolean bulk_compression = false;
	private boolean console_audio = false;

	private boolean load_licence = false;
	private boolean save_licence = false;

	private String licence_path = "./";

	private boolean debug_keyboard = false;
	private boolean debug_hexdump = false;

	private boolean enable_menu = false;
	// private boolean paste_hack = true;
	private boolean no_loginProgress = false;

	private boolean seamless_active = false;
	private boolean http_mode = false;
	private String http_server = "192.168.0.115:8080/WSService/RDPSocket";

	private int screenInsets_title = 14;

	private boolean altkey_quiet = false;
	private boolean caps_sends_up_and_down = true;
	private boolean remap_hash = true;
	private boolean useLockingKeyState = true;

	private boolean use_rdp5 = true;
	private int server_bpp = 16; // Bits per pixel
	private int Bpp = (server_bpp + 7) / 8; // Bytes per pixel

	private int bpp_mask = 0xFFFFFF >> 8 * (3 - Bpp); // Correction value
														// to ensure only
														// the relevant
														// number of bytes
														// are used for a
														// pixel

	private int imgCount = 0;

//	private DirectColorModel colour_model = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);


	/**
	 * Set a new value for the server's bits per pixel
	 * 
	 * @param server_bpp
	 *            New bpp value
	 */
	public void set_bpp(int server_bpp) {
		this.server_bpp = server_bpp;
		Bpp = (server_bpp + 7) / 8;
		// if(server_bpp == 8) bpp_mask = 0xFF;
		// else bpp_mask = 0xFFFFFF;
		bpp_mask = 0xFFFFFF >> 8 * (3 - Bpp);
//		colour_model = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
	}


	private int server_rdp_version;

	private int win_button_size = 0; /* If zero, disable single app mode */
	private boolean bitmap_compression = true;
	private boolean persistent_bitmap_caching = false;
	private boolean bitmap_caching = true;
	private boolean precache_bitmaps = true;
	private boolean polygon_ellipse_orders = false;
	private boolean sendmotion = true;
	private boolean orders = true;
	private boolean encryption = true;
	private boolean packet_encryption = true;
	private boolean desktop_save = true;
	private boolean grab_keyboard = true;
	private boolean hide_decorations = false;
	private boolean console_session = false;
	private boolean owncolmap;

	private boolean use_ssl = false;
	private boolean map_clipboard = true;
	private int RDP5_DISABLE_NOTHING = 0x00;

	private int RDP5_NO_WALLPAPER = 0x01;

	private int RDP5_NO_FULLWINDOWDRAG = 0x02;

	private int RDP5_NO_MENUANIMATIONS = 0x04;

	private int RDP5_NO_THEMING = 0x08;

	private int RDP5_NO_CURSOR_SHADOW = 0x20;

	private int RDP5_NO_CURSORSETTINGS = 0x40;
	private int rdp5_performanceflags = RDP5_NO_CURSOR_SHADOW | RDP5_NO_CURSORSETTINGS | RDP5_NO_FULLWINDOWDRAG
			| RDP5_NO_MENUANIMATIONS | RDP5_NO_THEMING | RDP5_NO_WALLPAPER;
	private boolean save_graphics = false;

	private boolean keys_register = true;
	private boolean is_debug = false;


	public int getBitmap_decompression_store() {
		return bitmap_decompression_store;
	}

	public void setBitmap_decompression_store(int bitmap_decompression_store) {
		this.bitmap_decompression_store = bitmap_decompression_store;
	}

	public boolean isLow_latency() {
		return low_latency;
	}

	public void setLow_latency(boolean low_latency) {
		this.low_latency = low_latency;
	}

	public int getKeylayout() {
		return keylayout;
	}

	public void setKeylayout(int keylayout) {
		this.keylayout = keylayout;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getWindowTitle() {
		return windowTitle;
	}

	public void setWindowTitle(String windowTitle) {
		this.windowTitle = windowTitle;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}

	public boolean isBuilt_in_licence() {
		return built_in_licence;
	}

	public void setBuilt_in_licence(boolean built_in_licence) {
		this.built_in_licence = built_in_licence;
	}

	public boolean isAutologin() {
		return autologin;
	}

	public void setAutologin(boolean autologin) {
		this.autologin = autologin;
	}

	public boolean isBulk_compression() {
		return bulk_compression;
	}

	public void setBulk_compression(boolean bulk_compression) {
		this.bulk_compression = bulk_compression;
	}

	public boolean isConsole_audio() {
		return console_audio;
	}

	public void setConsole_audio(boolean console_audio) {
		this.console_audio = console_audio;
	}

	public boolean isLoad_licence() {
		return load_licence;
	}

	public void setLoad_licence(boolean load_licence) {
		this.load_licence = load_licence;
	}

	public boolean isSave_licence() {
		return save_licence;
	}

	public void setSave_licence(boolean save_licence) {
		this.save_licence = save_licence;
	}

	public String getLicence_path() {
		return licence_path;
	}

	public void setLicence_path(String licence_path) {
		this.licence_path = licence_path;
	}

	public boolean isDebug_keyboard() {
		return debug_keyboard;
	}

	public void setDebug_keyboard(boolean debug_keyboard) {
		this.debug_keyboard = debug_keyboard;
	}

	public boolean isDebug_hexdump() {
		return debug_hexdump;
	}

	public void setDebug_hexdump(boolean debug_hexdump) {
		this.debug_hexdump = debug_hexdump;
	}

	public boolean isEnable_menu() {
		return enable_menu;
	}

	public void setEnable_menu(boolean enable_menu) {
		this.enable_menu = enable_menu;
	}

	public boolean isNo_loginProgress() {
		return no_loginProgress;
	}

	public void setNo_loginProgress(boolean no_loginProgress) {
		this.no_loginProgress = no_loginProgress;
	}

	public boolean isSeamless_active() {
		return seamless_active;
	}

	public void setSeamless_active(boolean seamless_active) {
		this.seamless_active = seamless_active;
	}

	public boolean isHttp_mode() {
		return http_mode;
	}

	public void setHttp_mode(boolean http_mode) {
		this.http_mode = http_mode;
	}

	public String getHttp_server() {
		return http_server;
	}

	public void setHttp_server(String http_server) {
		this.http_server = http_server;
	}

	public int getScreenInsets_title() {
		return screenInsets_title;
	}

	public void setScreenInsets_title(int screenInsets_title) {
		this.screenInsets_title = screenInsets_title;
	}

	public boolean isAltkey_quiet() {
		return altkey_quiet;
	}

	public void setAltkey_quiet(boolean altkey_quiet) {
		this.altkey_quiet = altkey_quiet;
	}

	public boolean isCaps_sends_up_and_down() {
		return caps_sends_up_and_down;
	}

	public void setCaps_sends_up_and_down(boolean caps_sends_up_and_down) {
		this.caps_sends_up_and_down = caps_sends_up_and_down;
	}

	public boolean isRemap_hash() {
		return remap_hash;
	}

	public void setRemap_hash(boolean remap_hash) {
		this.remap_hash = remap_hash;
	}

	public boolean isUseLockingKeyState() {
		return useLockingKeyState;
	}

	public void setUseLockingKeyState(boolean useLockingKeyState) {
		this.useLockingKeyState = useLockingKeyState;
	}

	public boolean isUse_rdp5() {
		return use_rdp5;
	}

	public void setUse_rdp5(boolean use_rdp5) {
		this.use_rdp5 = use_rdp5;
	}

	public int getServer_bpp() {
		return server_bpp;
	}

	public void setServer_bpp(int server_bpp) {
		this.server_bpp = server_bpp;
	}

	public int getBpp() {
		return Bpp;
	}

	public void setBpp(int bpp) {
		Bpp = bpp;
	}

	public int getBpp_mask() {
		return bpp_mask;
	}

	public void setBpp_mask(int bpp_mask) {
		this.bpp_mask = bpp_mask;
	}

	public int getImgCount() {
		return imgCount;
	}

	public void setImgCount(int imgCount) {
		this.imgCount = imgCount;
	}

//	public DirectColorModel getColour_model() {
//		return colour_model;
//	}
//
//	public void setColour_model(DirectColorModel colour_model) {
//		this.colour_model = colour_model;
//	}

	public int getServer_rdp_version() {
		return server_rdp_version;
	}

	public void setServer_rdp_version(int server_rdp_version) {
		this.server_rdp_version = server_rdp_version;
	}

	public int getWin_button_size() {
		return win_button_size;
	}

	public void setWin_button_size(int win_button_size) {
		this.win_button_size = win_button_size;
	}

	public boolean isBitmap_compression() {
		return bitmap_compression;
	}

	public void setBitmap_compression(boolean bitmap_compression) {
		this.bitmap_compression = bitmap_compression;
	}

	public boolean isPersistent_bitmap_caching() {
		return persistent_bitmap_caching;
	}

	public void setPersistent_bitmap_caching(boolean persistent_bitmap_caching) {
		this.persistent_bitmap_caching = persistent_bitmap_caching;
	}

	public boolean isBitmap_caching() {
		return bitmap_caching;
	}

	public void setBitmap_caching(boolean bitmap_caching) {
		this.bitmap_caching = bitmap_caching;
	}

	public boolean isPrecache_bitmaps() {
		return precache_bitmaps;
	}

	public void setPrecache_bitmaps(boolean precache_bitmaps) {
		this.precache_bitmaps = precache_bitmaps;
	}

	public boolean isPolygon_ellipse_orders() {
		return polygon_ellipse_orders;
	}

	public void setPolygon_ellipse_orders(boolean polygon_ellipse_orders) {
		this.polygon_ellipse_orders = polygon_ellipse_orders;
	}

	public boolean isSendmotion() {
		return sendmotion;
	}

	public void setSendmotion(boolean sendmotion) {
		this.sendmotion = sendmotion;
	}

	public boolean isOrders() {
		return orders;
	}

	public void setOrders(boolean orders) {
		this.orders = orders;
	}

	public boolean isEncryption() {
		return encryption;
	}

	public void setEncryption(boolean encryption) {
		this.encryption = encryption;
	}

	public boolean isPacket_encryption() {
		return packet_encryption;
	}

	public void setPacket_encryption(boolean packet_encryption) {
		this.packet_encryption = packet_encryption;
	}

	public boolean isDesktop_save() {
		return desktop_save;
	}

	public void setDesktop_save(boolean desktop_save) {
		this.desktop_save = desktop_save;
	}

	public boolean isGrab_keyboard() {
		return grab_keyboard;
	}

	public void setGrab_keyboard(boolean grab_keyboard) {
		this.grab_keyboard = grab_keyboard;
	}

	public boolean isHide_decorations() {
		return hide_decorations;
	}

	public void setHide_decorations(boolean hide_decorations) {
		this.hide_decorations = hide_decorations;
	}

	public boolean isConsole_session() {
		return console_session;
	}

	public void setConsole_session(boolean console_session) {
		this.console_session = console_session;
	}

	public boolean isOwncolmap() {
		return owncolmap;
	}

	public void setOwncolmap(boolean owncolmap) {
		this.owncolmap = owncolmap;
	}

	public boolean isUse_ssl() {
		return use_ssl;
	}

	public void setUse_ssl(boolean use_ssl) {
		this.use_ssl = use_ssl;
	}

	public boolean isMap_clipboard() {
		return map_clipboard;
	}

	public void setMap_clipboard(boolean map_clipboard) {
		this.map_clipboard = map_clipboard;
	}

	public int getRDP5_DISABLE_NOTHING() {
		return RDP5_DISABLE_NOTHING;
	}

	public void setRDP5_DISABLE_NOTHING(int rDP5_DISABLE_NOTHING) {
		RDP5_DISABLE_NOTHING = rDP5_DISABLE_NOTHING;
	}

	public int getRDP5_NO_WALLPAPER() {
		return RDP5_NO_WALLPAPER;
	}

	public void setRDP5_NO_WALLPAPER(int rDP5_NO_WALLPAPER) {
		RDP5_NO_WALLPAPER = rDP5_NO_WALLPAPER;
	}

	public int getRDP5_NO_FULLWINDOWDRAG() {
		return RDP5_NO_FULLWINDOWDRAG;
	}

	public void setRDP5_NO_FULLWINDOWDRAG(int rDP5_NO_FULLWINDOWDRAG) {
		RDP5_NO_FULLWINDOWDRAG = rDP5_NO_FULLWINDOWDRAG;
	}

	public int getRDP5_NO_MENUANIMATIONS() {
		return RDP5_NO_MENUANIMATIONS;
	}

	public void setRDP5_NO_MENUANIMATIONS(int rDP5_NO_MENUANIMATIONS) {
		RDP5_NO_MENUANIMATIONS = rDP5_NO_MENUANIMATIONS;
	}

	public int getRDP5_NO_THEMING() {
		return RDP5_NO_THEMING;
	}

	public void setRDP5_NO_THEMING(int rDP5_NO_THEMING) {
		RDP5_NO_THEMING = rDP5_NO_THEMING;
	}

	public int getRDP5_NO_CURSOR_SHADOW() {
		return RDP5_NO_CURSOR_SHADOW;
	}

	public void setRDP5_NO_CURSOR_SHADOW(int rDP5_NO_CURSOR_SHADOW) {
		RDP5_NO_CURSOR_SHADOW = rDP5_NO_CURSOR_SHADOW;
	}

	public int getRDP5_NO_CURSORSETTINGS() {
		return RDP5_NO_CURSORSETTINGS;
	}

	public void setRDP5_NO_CURSORSETTINGS(int rDP5_NO_CURSORSETTINGS) {
		RDP5_NO_CURSORSETTINGS = rDP5_NO_CURSORSETTINGS;
	}

	public int getRdp5_performanceflags() {
		return rdp5_performanceflags;
	}

	public void setRdp5_performanceflags(int rdp5_performanceflags) {
		this.rdp5_performanceflags = rdp5_performanceflags;
	}

	public boolean isSave_graphics() {
		return save_graphics;
	}

	public void setSave_graphics(boolean save_graphics) {
		this.save_graphics = save_graphics;
	}

	public boolean isKeys_register() {
		return keys_register;
	}

	public void setKeys_register(boolean keys_register) {
		this.keys_register = keys_register;
	}

	public boolean isIs_debug() {
		return is_debug;
	}

	public void setIs_debug(boolean is_debug) {
		this.is_debug = is_debug;
	}

}
