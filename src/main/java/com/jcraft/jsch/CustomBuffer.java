package com.jcraft.jsch;


/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
 Copyright (c) 2002-2015 ymnk, JCraft,Inc. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright 
 notice, this list of conditions and the following disclaimer in 
 the documentation and/or other materials provided with the distribution.

 3. The names of the authors may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
 INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class CustomBuffer {

	static final int buffer_margin = 32 + // maximum padding length
	20 + // maximum mac length
	32; // margin for deflater; deflater may inflate data

	final byte[] tmp = new byte[4];
	private byte[] buffer;
	private int index;
	private int s;


	public CustomBuffer(int size) {
		setBuffer(new byte[size]);
		setIndex(0);
		setS(0);
	}

	public CustomBuffer(byte[] buffer) {
		this.setBuffer(buffer);
		setIndex(0);
		setS(0);
	}

	public CustomBuffer() {
		this(1024 * 10 * 2);
	}

	public void putByte(byte foo) {
		getBuffer()[index++] = foo;
	}

	public void putByte(byte[] foo) {
		putByte(foo, 0, foo.length);
	}

	public void putByte(byte[] foo, int begin, int length) {
		System.arraycopy(foo, begin, getBuffer(), getIndex(), length);
		setIndex(getIndex() + length);
	}

	public void putString(byte[] foo) {
		putString(foo, 0, foo.length);
	}

	public void putString(byte[] foo, int begin, int length) {
		putInt(length);
		putByte(foo, begin, length);
	}

	public void putInt(int val) {
		tmp[0] = (byte) (val >>> 24);
		tmp[1] = (byte) (val >>> 16);
		tmp[2] = (byte) (val >>> 8);
		tmp[3] = (byte) (val);
		System.arraycopy(tmp, 0, getBuffer(), getIndex(), 4);
		setIndex(getIndex() + 4);
	}

	public void putLong(long val) {
		tmp[0] = (byte) (val >>> 56);
		tmp[1] = (byte) (val >>> 48);
		tmp[2] = (byte) (val >>> 40);
		tmp[3] = (byte) (val >>> 32);
		System.arraycopy(tmp, 0, getBuffer(), getIndex(), 4);
		tmp[0] = (byte) (val >>> 24);
		tmp[1] = (byte) (val >>> 16);
		tmp[2] = (byte) (val >>> 8);
		tmp[3] = (byte) (val);
		System.arraycopy(tmp, 0, getBuffer(), getIndex() + 4, 4);
		setIndex(getIndex() + 8);
	}

	public void skip(int n) {
		setIndex(getIndex() + n);
	}

	void putPad(int n) {
		while (n > 0) {
			getBuffer()[index++] = (byte) 0;
			n--;
		}
	}

	public void putMPInt(byte[] foo) {
		int i = foo.length;
		if ((foo[0] & 0x80) != 0) {
			i++;
			putInt(i);
			putByte((byte) 0);
		} else {
			putInt(i);
		}
		putByte(foo);
	}

	public int getLength() {
		return getIndex() - getS();
	}

	public int getOffSet() {
		return getS();
	}

	public void setOffSet(int s) {
		this.setS(s);
	}

	public long getLong() {
		long foo = getInt() & 0xffffffffL;
		foo = ((foo << 32)) | (getInt() & 0xffffffffL);
		return foo;
	}

	public int getInt() {
		int foo = getShort();
		foo = ((foo << 16) & 0xffff0000) | (getShort() & 0xffff);
		return foo;
	}

	public long getUInt() {
		long foo = 0L;
		long bar = 0L;
		foo = getByte();
		foo = ((foo << 8) & 0xff00) | (getByte() & 0xff);
		bar = getByte();
		bar = ((bar << 8) & 0xff00) | (getByte() & 0xff);
		foo = ((foo << 16) & 0xffff0000) | (bar & 0xffff);
		return foo;
	}

	public int getShort() {
		int foo = getByte();
		foo = ((foo << 8) & 0xff00) | (getByte() & 0xff);
		return foo;
	}

	public int getByte() {
		return (getBuffer()[s++] & 0xff);
	}

	public void getByte(byte[] foo) {
		getByte(foo, 0, foo.length);
	}

	void getByte(byte[] foo, int start, int len) {
		System.arraycopy(getBuffer(), getS(), foo, start, len);
		setS(getS() + len);
	}

	public int getByte(int len) {
		int foo = getS();
		setS(getS() + len);
		return foo;
	}

	public byte[] getMPInt() {
		int i = getInt(); // uint32
		if (i < 0 || // bigger than 0x7fffffff
				i > 8 * 1024) {
			// TODO: an exception should be thrown.
			i = 8 * 1024; // the session will be broken, but working around
							// OOME.
		}
		byte[] foo = new byte[i];
		getByte(foo, 0, i);
		return foo;
	}

	public byte[] getMPIntBits() {
		int bits = getInt();
		int bytes = (bits + 7) / 8;
		byte[] foo = new byte[bytes];
		getByte(foo, 0, bytes);
		if ((foo[0] & 0x80) != 0) {
			byte[] bar = new byte[foo.length + 1];
			bar[0] = 0; // ??
			System.arraycopy(foo, 0, bar, 1, foo.length);
			foo = bar;
		}
		return foo;
	}

	public byte[] getString() {
		int i = getInt(); // uint32
		if (i < 0 || // bigger than 0x7fffffff
				i > 256 * 1024) {
			// TODO: an exception should be thrown.
			i = 256 * 1024; // the session will be broken, but working around
							// OOME.
		}
		byte[] foo = new byte[i];
		getByte(foo, 0, i);
		return foo;
	}

	public byte[] getString(int[] start, int[] len) {
		int i = getInt();
		start[0] = getByte(i);
		len[0] = i;
		return getBuffer();
	}

	public void reset() {
		setIndex(0);
		setS(0);
	}

	public void shift() {
		if (getS() == 0)
			return;
		System.arraycopy(getBuffer(), getS(), getBuffer(), 0, getIndex() - getS());
		setIndex(getIndex() - getS());
		setS(0);
	}

	public void rewind() {
		setS(0);
	}

	public byte getCommand() {
		return getBuffer()[5];
	}
	
	public void checkFreeSize(int n) {
		if(getBuffer().length < (getIndex() + n)) {
			byte[] tmp = new byte[getBuffer().length*2];
			System.arraycopy(getBuffer(), 0, tmp, 0, getIndex());
			setBuffer(tmp);
		}
	}

//	void checkFreeSize(int n) {
//		int size = getIndex() + n + buffer_margin;
//		if (getBuffer().length < size) {
//			int i = getBuffer().length * 2;
//			if (i < size)
//				i = size;
//			byte[] tmp = new byte[i];
//			System.arraycopy(getBuffer(), 0, tmp, 0, getIndex());
//			setBuffer(tmp);
//		}
//	}

	byte[][] getBytes(int n, String msg) throws JSchException {
		byte[][] tmp = new byte[n][];
		for (int i = 0; i < n; i++) {
			int j = getInt();
			if (getLength() < j) {
				throw new JSchException(msg);
			}
			tmp[i] = new byte[j];
			getByte(tmp[i]);
		}
		return tmp;
	}

	/*
	 * static Buffer fromBytes(byte[]... args){ int length = args.length*4;
	 * for(int i = 0; i < args.length; i++){ length += args[i].length; } Buffer
	 * buf = new Buffer(length); for(int i = 0; i < args.length; i++){
	 * buf.putString(args[i]); } return buf; }
	 */

	static CustomBuffer fromBytes(byte[][] args) {
		int length = args.length * 4;
		for (int i = 0; i < args.length; i++) {
			length += args[i].length;
		}
		CustomBuffer buf = new CustomBuffer(length);
		for (int i = 0; i < args.length; i++) {
			buf.putString(args[i]);
		}
		return buf;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getS() {
		return s;
	}

	public void setS(int s) {
		this.s = s;
	}

	/*
	 * static String[] chars={ "0","1","2","3","4","5","6","7","8","9",
	 * "a","b","c","d","e","f" }; static void dump_buffer(){ int foo; for(int
	 * i=0; i<tmp_buffer_index; i++){ foo=tmp_buffer[i]&0xff;
	 * System.err.print(chars[(foo>>>4)&0xf]); System.err.print(chars[foo&0xf]);
	 * if(i%16==15){ System.err.println(""); continue; } if(i>0 && i%2==1){
	 * System.err.print(" "); } } System.err.println(""); } static void
	 * dump(byte[] b){ dump(b, 0, b.length); } static void dump(byte[] b, int s,
	 * int l){ for(int i=s; i<s+l; i++){
	 * System.err.print(Integer.toHexString(b[i]&0xff)+":"); }
	 * System.err.println(""); }
	 */

}
