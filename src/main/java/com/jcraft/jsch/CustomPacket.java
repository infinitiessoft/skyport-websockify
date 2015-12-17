package com.jcraft.jsch;

public class CustomPacket {

	private static Random random = null;


	static void setRandom(Random foo) {
		random = foo;
	}


	CustomBuffer buffer;
	byte[] ba4 = new byte[4];


	public CustomPacket(CustomBuffer buffer) {
		this.buffer = buffer;
	}

	public void reset() {
		buffer.setIndex(5);
	}

	void padding(int bsize) {
		int len = buffer.getIndex();
		int pad = (-len) & (bsize - 1);
		if (pad < bsize) {
			pad += bsize;
		}
		len = len + pad - 4;
		ba4[0] = (byte) (len >>> 24);
		ba4[1] = (byte) (len >>> 16);
		ba4[2] = (byte) (len >>> 8);
		ba4[3] = (byte) (len);
		System.arraycopy(ba4, 0, buffer.getBuffer(), 0, 4);
		buffer.getBuffer()[4] = (byte) pad;
		synchronized (random) {
			random.fill(buffer.getBuffer(), buffer.getIndex(), pad);
		}
		buffer.skip(pad);
		// buffer.putPad(pad);
		/*
		 * for(int i=0; i<buffer.index; i++){
		 * System.err.print(Integer.toHexString(buffer.buffer[i]&0xff)+":"); }
		 * System.err.println("");
		 */
	}

	int shift(int len, int bsize, int mac) {
		int s = len + 5 + 9;
		int pad = (-s) & (bsize - 1);
		if (pad < bsize)
			pad += bsize;
		s += pad;
		s += mac;
		s += 32; // margin for deflater; deflater may inflate data

		/**/
		if (buffer.getBuffer().length < s + buffer.getIndex() - 5 - 9 - len) {
			byte[] foo = new byte[s + CustomBuffer.buffer_margin - 5 - 9 - len];
			System.arraycopy(buffer.getBuffer(), 0, foo, 0, buffer.getBuffer().length);
			buffer.setBuffer(foo);
		}
		/**/

		// if(buffer.buffer.length<len+5+9)
		// System.err.println("buffer.buffer.length="+buffer.buffer.length+" len+5+9="+(len+5+9));

		// if(buffer.buffer.length<s)
		// System.err.println("buffer.buffer.length="+buffer.buffer.length+" s="+(s));

		System.arraycopy(buffer.getBuffer(), len + 5 + 9, buffer.getBuffer(), s, buffer.getIndex() - 5 - 9 - len);

		buffer.setIndex(10);
		buffer.putInt(len);
		buffer.setIndex(len + 5 + 9);
		return s;
	}

	void unshift(byte command, int recipient, int s, int len) {
		System.arraycopy(buffer.getBuffer(), s, buffer.getBuffer(), 5 + 9, len);
		buffer.getBuffer()[5] = command;
		buffer.setIndex(6);
		buffer.putInt(recipient);
		buffer.putInt(len);
		buffer.setIndex(len + 5 + 9);
	}

	CustomBuffer getBuffer() {
		return buffer;
	}
}
