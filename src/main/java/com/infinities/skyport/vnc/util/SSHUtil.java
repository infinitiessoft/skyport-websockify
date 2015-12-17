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
package com.infinities.skyport.vnc.util;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;

import com.jcraft.jsch.HASH;
import com.jcraft.jsch.JSchException;

public class SSHUtil {

	public static final int SESSION_TIMEOUT = 60000;

	public static Map<Integer, byte[]> keyMap = new HashMap<Integer, byte[]>();

	static {
		// ESC
		keyMap.put(27, new byte[] { (byte) 0x1b });
		// ENTER
		keyMap.put(13, new byte[] { (byte) 0x0d });
		// LEFT
		keyMap.put(37, new byte[] { (byte) 0x1b, (byte) 0x4f, (byte) 0x44 });
		// UP
		keyMap.put(38, new byte[] { (byte) 0x1b, (byte) 0x4f, (byte) 0x41 });
		// RIGHT
		keyMap.put(39, new byte[] { (byte) 0x1b, (byte) 0x4f, (byte) 0x43 });
		// DOWN
		keyMap.put(40, new byte[] { (byte) 0x1b, (byte) 0x4f, (byte) 0x42 });
		// BS
		keyMap.put(8, new byte[] { (byte) 0x7f });
		// TAB
		keyMap.put(9, new byte[] { (byte) 0x09 });
		// CTR
		keyMap.put(17, new byte[] {});
		// DEL
		keyMap.put(46, "\033[3~".getBytes());
		// CTR-A
		keyMap.put(65, new byte[] { (byte) 0x01 });
		// CTR-B
		keyMap.put(66, new byte[] { (byte) 0x02 });
		// CTR-C
		keyMap.put(67, new byte[] { (byte) 0x03 });
		// CTR-D
		keyMap.put(68, new byte[] { (byte) 0x04 });
		// CTR-E
		keyMap.put(69, new byte[] { (byte) 0x05 });
		// CTR-F
		keyMap.put(70, new byte[] { (byte) 0x06 });
		// CTR-G
		keyMap.put(71, new byte[] { (byte) 0x07 });
		// CTR-H
		keyMap.put(72, new byte[] { (byte) 0x08 });
		// CTR-I
		keyMap.put(73, new byte[] { (byte) 0x09 });
		// CTR-J
		keyMap.put(74, new byte[] { (byte) 0x0A });
		// CTR-K
		keyMap.put(75, new byte[] { (byte) 0x0B });
		// CTR-L
		keyMap.put(76, new byte[] { (byte) 0x0C });
		// CTR-M
		keyMap.put(77, new byte[] { (byte) 0x0D });
		// CTR-N
		keyMap.put(78, new byte[] { (byte) 0x0E });
		// CTR-O
		keyMap.put(79, new byte[] { (byte) 0x0F });
		// CTR-P
		keyMap.put(80, new byte[] { (byte) 0x10 });
		// CTR-Q
		keyMap.put(81, new byte[] { (byte) 0x11 });
		// CTR-R
		keyMap.put(82, new byte[] { (byte) 0x12 });
		// CTR-S
		keyMap.put(83, new byte[] { (byte) 0x13 });
		// CTR-T
		keyMap.put(84, new byte[] { (byte) 0x14 });
		// CTR-U
		keyMap.put(85, new byte[] { (byte) 0x15 });
		// CTR-V
		keyMap.put(86, new byte[] { (byte) 0x16 });
		// CTR-W
		keyMap.put(87, new byte[] { (byte) 0x17 });
		// CTR-X
		keyMap.put(88, new byte[] { (byte) 0x18 });
		// CTR-Y
		keyMap.put(89, new byte[] { (byte) 0x19 });
		// CTR-Z
		keyMap.put(90, new byte[] { (byte) 0x1A });
		// CTR-[
		keyMap.put(219, new byte[] { (byte) 0x1B });
		// CTR-]
		keyMap.put(221, new byte[] { (byte) 0x1D });
		// INSERT
		keyMap.put(45, "\033[2~".getBytes());
		// PG UP
		keyMap.put(33, "\033[5~".getBytes());
		// PG DOWN
		keyMap.put(34, "\033[6~".getBytes());
		// END
		keyMap.put(35, "\033[4~".getBytes());
		// HOME
		keyMap.put(36, "\033[1~".getBytes());
	}

	private static final byte[] b64 = str2byte("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=");


	private static byte val(byte foo) {
		if (foo == '=')
			return 0;
		for (int j = 0; j < b64.length; j++) {
			if (foo == b64[j])
				return (byte) j;
		}
		return 0;
	}

	public static byte[] fromBase64(byte[] buf, int start, int length) {
		byte[] foo = new byte[length];
		int j = 0;
		for (int i = start; i < start + length; i += 4) {
			foo[j] = (byte) ((val(buf[i]) << 2) | ((val(buf[i + 1]) & 0x30) >>> 4));
			if (buf[i + 2] == (byte) '=') {
				j++;
				break;
			}
			foo[j + 1] = (byte) (((val(buf[i + 1]) & 0x0f) << 4) | ((val(buf[i + 2]) & 0x3c) >>> 2));
			if (buf[i + 3] == (byte) '=') {
				j += 2;
				break;
			}
			foo[j + 2] = (byte) (((val(buf[i + 2]) & 0x03) << 6) | (val(buf[i + 3]) & 0x3f));
			j += 3;
		}
		byte[] bar = new byte[j];
		System.arraycopy(foo, 0, bar, 0, j);
		return bar;
	}

	public static byte[] toBase64(byte[] buf, int start, int length) {

		byte[] tmp = new byte[length * 2];
		int i, j, k;

		int foo = (length / 3) * 3 + start;
		i = 0;
		for (j = start; j < foo; j += 3) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
			tmp[i++] = b64[k];
			k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
			tmp[i++] = b64[k];
			k = buf[j + 2] & 0x3f;
			tmp[i++] = b64[k];
		}

		foo = (start + length) - foo;
		if (foo == 1) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = ((buf[j] & 0x03) << 4) & 0x3f;
			tmp[i++] = b64[k];
			tmp[i++] = (byte) '=';
			tmp[i++] = (byte) '=';
		} else if (foo == 2) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
			tmp[i++] = b64[k];
			k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
			tmp[i++] = b64[k];
			tmp[i++] = (byte) '=';
		}
		byte[] bar = new byte[i];
		System.arraycopy(tmp, 0, bar, 0, i);
		return bar;

		// return sun.misc.BASE64Encoder().encode(buf);
	}

	public static String[] split(String foo, String split) {
		if (foo == null)
			return null;
		byte[] buf = str2byte(foo);
		java.util.Vector<String> bar = new java.util.Vector<String>();
		int start = 0;
		int index;
		while (true) {
			index = foo.indexOf(split, start);
			if (index >= 0) {
				bar.addElement(byte2str(buf, start, index - start));
				start = index + 1;
				continue;
			}
			bar.addElement(byte2str(buf, start, buf.length - start));
			break;
		}
		String[] result = new String[bar.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (String) (bar.elementAt(i));
		}
		return result;
	}

	static boolean glob(byte[] pattern, byte[] name) {
		return glob0(pattern, 0, name, 0);
	}

	static private boolean glob0(byte[] pattern, int pattern_index, byte[] name, int name_index) {
		if (name.length > 0 && name[0] == '.') {
			if (pattern.length > 0 && pattern[0] == '.') {
				if (pattern.length == 2 && pattern[1] == '*')
					return true;
				return glob(pattern, pattern_index + 1, name, name_index + 1);
			}
			return false;
		}
		return glob(pattern, pattern_index, name, name_index);
	}

	static private boolean glob(byte[] pattern, int pattern_index, byte[] name, int name_index) {
		// System.err.println("glob: "+new
		// String(pattern)+", "+pattern_index+" "+new
		// String(name)+", "+name_index);

		int patternlen = pattern.length;
		if (patternlen == 0)
			return false;

		int namelen = name.length;
		int i = pattern_index;
		int j = name_index;

		while (i < patternlen && j < namelen) {
			if (pattern[i] == '\\') {
				if (i + 1 == patternlen)
					return false;
				i++;
				if (pattern[i] != name[j])
					return false;
				i += skipUTF8Char(pattern[i]);
				j += skipUTF8Char(name[j]);
				continue;
			}

			if (pattern[i] == '*') {
				while (i < patternlen) {
					if (pattern[i] == '*') {
						i++;
						continue;
					}
					break;
				}
				if (patternlen == i)
					return true;

				byte foo = pattern[i];
				if (foo == '?') {
					while (j < namelen) {
						if (glob(pattern, i, name, j)) {
							return true;
						}
						j += skipUTF8Char(name[j]);
					}
					return false;
				} else if (foo == '\\') {
					if (i + 1 == patternlen)
						return false;
					i++;
					foo = pattern[i];
					while (j < namelen) {
						if (foo == name[j]) {
							if (glob(pattern, i + skipUTF8Char(foo), name, j + skipUTF8Char(name[j]))) {
								return true;
							}
						}
						j += skipUTF8Char(name[j]);
					}
					return false;
				}

				while (j < namelen) {
					if (foo == name[j]) {
						if (glob(pattern, i, name, j)) {
							return true;
						}
					}
					j += skipUTF8Char(name[j]);
				}
				return false;
			}

			if (pattern[i] == '?') {
				i++;
				j += skipUTF8Char(name[j]);
				continue;
			}

			if (pattern[i] != name[j])
				return false;

			i += skipUTF8Char(pattern[i]);
			j += skipUTF8Char(name[j]);

			if (!(j < namelen)) { // name is end
				if (!(i < patternlen)) { // pattern is end
					return true;
				}
				if (pattern[i] == '*') {
					break;
				}
			}
			continue;
		}

		if (i == patternlen && j == namelen)
			return true;

		if (!(j < namelen) && // name is end
				pattern[i] == '*') {
			boolean ok = true;
			while (i < patternlen) {
				if (pattern[i++] != '*') {
					ok = false;
					break;
				}
			}
			return ok;
		}

		return false;
	}

	static String quote(String path) {
		byte[] _path = str2byte(path);
		int count = 0;
		for (int i = 0; i < _path.length; i++) {
			byte b = _path[i];
			if (b == '\\' || b == '?' || b == '*')
				count++;
		}
		if (count == 0)
			return path;
		byte[] _path2 = new byte[_path.length + count];
		for (int i = 0, j = 0; i < _path.length; i++) {
			byte b = _path[i];
			if (b == '\\' || b == '?' || b == '*') {
				_path2[j++] = '\\';
			}
			_path2[j++] = b;
		}
		return byte2str(_path2);
	}

	static String unquote(String path) {
		byte[] foo = str2byte(path);
		byte[] bar = unquote(foo);
		if (foo.length == bar.length)
			return path;
		return byte2str(bar);
	}

	static byte[] unquote(byte[] path) {
		int pathlen = path.length;
		int i = 0;
		while (i < pathlen) {
			if (path[i] == '\\') {
				if (i + 1 == pathlen)
					break;
				System.arraycopy(path, i + 1, path, i, path.length - (i + 1));
				pathlen--;
				i++;
				continue;
			}
			i++;
		}
		if (pathlen == path.length)
			return path;
		byte[] foo = new byte[pathlen];
		System.arraycopy(path, 0, foo, 0, pathlen);
		return foo;
	}


	private static String[] chars = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };


	public static String getFingerPrint(HASH hash, byte[] data) {
		try {
			hash.init();
			hash.update(data, 0, data.length);
			byte[] foo = hash.digest();
			StringBuffer sb = new StringBuffer();
			int bar;
			for (int i = 0; i < foo.length; i++) {
				bar = foo[i] & 0xff;
				sb.append(chars[(bar >>> 4) & 0xf]);
				sb.append(chars[(bar) & 0xf]);
				if (i + 1 < foo.length)
					sb.append(":");
			}
			return sb.toString();
		} catch (Exception e) {
			return "???";
		}
	}

	public static boolean array_equals(byte[] foo, byte bar[]) {
		int i = foo.length;
		if (i != bar.length)
			return false;
		for (int j = 0; j < i; j++) {
			if (foo[j] != bar[j])
				return false;
		}
		// try{while(true){i--; if(foo[i]!=bar[i])return false;}}catch(Exception
		// e){}
		return true;
	}

	static Socket createSocket(String host, int port, int timeout) throws JSchException {
		Socket socket = null;
		if (timeout == 0) {
			try {
				socket = new Socket(host, port);
				return socket;
			} catch (Exception e) {
				String message = e.toString();
				if (e instanceof Throwable)
					throw new JSchException(message, (Throwable) e);
				throw new JSchException(message);
			}
		}
		final String _host = host;
		final int _port = port;
		final Socket[] sockp = new Socket[1];
		final Exception[] ee = new Exception[1];
		String message = "";
		Thread tmp = new Thread(new Runnable() {

			public void run() {
				sockp[0] = null;
				try {
					sockp[0] = new Socket(_host, _port);
				} catch (Exception e) {
					ee[0] = e;
					if (sockp[0] != null && sockp[0].isConnected()) {
						try {
							sockp[0].close();
						} catch (Exception eee) {
						}
					}
					sockp[0] = null;
				}
			}
		});
		tmp.setName("Opening Socket " + host);
		tmp.start();
		try {
			tmp.join(timeout);
			message = "timeout: ";
		} catch (java.lang.InterruptedException eee) {
		}
		if (sockp[0] != null && sockp[0].isConnected()) {
			socket = sockp[0];
		} else {
			message += "socket is not established";
			if (ee[0] != null) {
				message = ee[0].toString();
			}
			tmp.interrupt();
			tmp = null;
			throw new JSchException(message);
		}
		return socket;
	}

	static byte[] str2byte(String str, String encoding) {
		if (str == null)
			return null;
		try {
			return str.getBytes(encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	public static byte[] str2byte(String str) {
		return str2byte(str, "UTF-8");
	}

	static String byte2str(byte[] str, String encoding) {
		return byte2str(str, 0, str.length, encoding);
	}

	static String byte2str(byte[] str, int s, int l, String encoding) {
		try {
			return new String(str, s, l, encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			return new String(str, s, l);
		}
	}

	public static String byte2str(byte[] str) {
		return byte2str(str, 0, str.length, "UTF-8");
	}

	public static String byte2str(byte[] str, int s, int l) {
		return byte2str(str, s, l, "UTF-8");
	}


	public static final byte[] empty = str2byte("");


	/*
	 * static byte[] char2byte(char[] foo){ int len=0; for(int i=0;
	 * i<foo.length; i++){ if((foo[i]&0xff00)==0) len++; else len+=2; } byte[]
	 * bar=new byte[len]; for(int i=0, j=0; i<foo.length; i++){
	 * if((foo[i]&0xff00)==0){ bar[j++]=(byte)foo[i]; } else{
	 * bar[j++]=(byte)(foo[i]>>>8); bar[j++]=(byte)foo[i]; } } return bar; }
	 */
	public static void bzero(byte[] foo) {
		if (foo == null)
			return;
		for (int i = 0; i < foo.length; i++)
			foo[i] = 0;
	}

	public static String diffString(String str, String[] not_available) {
		String[] stra = split(str, ",");
		String result = null;
		loop: for (int i = 0; i < stra.length; i++) {
			for (int j = 0; j < not_available.length; j++) {
				if (stra[i].equals(not_available[j])) {
					continue loop;
				}
			}
			if (result == null) {
				result = stra[i];
			} else {
				result = result + "," + stra[i];
			}
		}
		return result;
	}

	private static int skipUTF8Char(byte b) {
		if ((byte) (b & 0x80) == 0)
			return 1;
		if ((byte) (b & 0xe0) == (byte) 0xc0)
			return 2;
		if ((byte) (b & 0xf0) == (byte) 0xe0)
			return 3;
		return 1;
	}

	public static String checkTilde(String str) {
		try {
			if (str.startsWith("~")) {
				str = str.replace("~", System.getProperty("user.home"));
			}
		} catch (SecurityException e) {
		}
		return str;
	}

	public static byte[] readString(ChannelBuffer buffer) {
		int i = buffer.readInt();
		if (i < 0 || // bigger than 0x7fffffff
				i > 256 * 1024) {
			// TODO: an exception should be thrown.
			i = 256 * 1024; // the session will be broken, but working around
							// OOME.
		}
		return buffer.readBytes(i).array();
	}

}
