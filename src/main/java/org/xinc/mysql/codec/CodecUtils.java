/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.xinc.mysql.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CodecException;
import io.netty.util.AsciiString;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Set;

final class CodecUtils {

	static final int NULL_VALUE = 0xfb;
	static final int SHORT_VALUE = 0xfc;
	static final int MEDIUM_VALUE = 0xfd;
	static final int LONG_VALUE = 0xfe;

	private CodecUtils() {
		// Non-instantiable
	}

	static long readLengthEncodedInteger(ByteBuf buf) {
		return readLengthEncodedInteger(buf, buf.readUnsignedByte());
	}

	// https://dev.mysql.com/doc/internals/en/integer.html
	static long readLengthEncodedInteger(ByteBuf buf, int firstByte) {
		firstByte = firstByte & 0xff;
		if (firstByte < NULL_VALUE) {
			return firstByte;
		}
		if (firstByte == NULL_VALUE) {
			return -1;
		}
		if (firstByte == SHORT_VALUE) {
			return buf.readUnsignedShortLE();
		}
		if (firstByte == MEDIUM_VALUE) {
			return buf.readUnsignedMediumLE();
		}
		if (firstByte == LONG_VALUE) {
			final long length = buf.readLongLE();
			if (length < 0) {
				throw new CodecException("Received a length value too large to handle: " + Long.toHexString(length));
			}
			return length;
		}
		throw new CodecException("Received an invalid length value " + firstByte);
	}

	//    static String readLengthEncodedString(ByteBuf buf, Charset charset) {
//        return readLengthEncodedString(buf, buf.readByte(), charset);
//    }
//
//    static String readLengthEncodedString(ByteBuf buf, int firstByte, Charset charset) {
//        final long length = readLengthEncodedInteger(buf, firstByte);
//        return readFixedLengthString(buf, (int) length, charset);
//    }
//
	static AsciiString readNullTerminatedString(ByteBuf buf) {
		final int len = findNullTermLen(buf);
		if (len < 0) {
			return null;
		}
		final AsciiString s = readFixedLengthString(buf, len);
		buf.readByte();
		return s;
	}

	static String readNullTerminatedString(ByteBuf buf, Charset charset) {
		final int len = findNullTermLen(buf);
		if (len < 0) {
			return null;
		}
		final String s = readFixedLengthString(buf, len, charset);
		buf.readByte();
		return s;
	}

	static int findNullTermLen(ByteBuf buf) {
		final int termIdx = buf.indexOf(buf.readerIndex(), buf.capacity(), (byte) 0);
		if (termIdx < 0) {
			return -1;
		}
		return termIdx - buf.readerIndex();
	}

	static AsciiString readFixedLengthString(ByteBuf buf, int len) {
		final byte[] bytes = new byte[len];
		buf.readBytes(bytes);
		return new AsciiString(bytes);
	}

	static String readFixedLengthString(ByteBuf buf, int length, Charset charset) {
		if (length < 0) {
			return null;
		}
		final String s = buf.toString(buf.readerIndex(), length, charset);
		buf.readerIndex(buf.readerIndex() + length);
		return s;
	}

	static String readLengthEncodedString(ByteBuf buf, Charset charset) {
		final long len = readLengthEncodedInteger(buf);
		return readFixedLengthString(buf, (int) len, charset);
	}

	static String readLengthEncodedString(ByteBuf buf, int firstByte, Charset charset) {
		final long len = readLengthEncodedInteger(buf, firstByte);
		return readFixedLengthString(buf, (int) len, charset);
	}

	static <E extends Enum<E>> EnumSet<E> readShortEnumSet(ByteBuf buf, Class<E> enumClass) {
		return toEnumSet(enumClass, buf.readUnsignedShortLE());
	}

	static <E extends Enum<E>> EnumSet<E> readIntEnumSet(ByteBuf buf, Class<E> enumClass) {
		return toEnumSet(enumClass, buf.readUnsignedIntLE());
	}

	static <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> enumClass, long vector) {
		EnumSet<E> set = EnumSet.noneOf(enumClass);
		for (E e : enumClass.getEnumConstants()) {
			final long mask = 1 << e.ordinal();
			if ((mask & vector) != 0) {
				set.add(e);
			}
		}
		return set;
	}

//    private static <E> E toEnum(Class<E> enumClass, int i) {
//        E[] enumConstants = enumClass.getEnumConstants();
//        if (i > enumConstants.length) {
//            throw new IndexOutOfBoundsException(String.format(
//                    "%d is too large of an ordinal to convert to the enum %s",
//                    i, enumClass.getName()));
//        }
//        return enumConstants[i];
//    }

	static <E extends Enum<E>> long toLong(Set<E> set) {
		long vector = 0;
		for (E e : set) {
			if (e.ordinal() >= Long.SIZE) {
				throw new IllegalArgumentException("The enum set is too large to fit in a bit vector: " + set);
			}
			vector |= 1L << e.ordinal();
		}
		return vector;
	}

	static void writeLengthEncodedInt(ByteBuf buf, Long n) {
		if (n == null) {
			buf.writeByte(NULL_VALUE);
		} else if (n < 0) {
			throw new IllegalArgumentException("Cannot encode a negative length: " + n);
		} else if (n < NULL_VALUE) {
			buf.writeByte(n.intValue());
		} else if (n < 0xffff) {
			buf.writeByte(SHORT_VALUE);
			buf.writeShortLE(n.intValue());
		} else if (n < 0xffffff) {
			buf.writeByte(MEDIUM_VALUE);
			buf.writeMediumLE(n.intValue());
		} else {
			buf.writeByte(LONG_VALUE);
			buf.writeLongLE(n);
		}
	}

	static void writeLengthEncodedString(ByteBuf buf, CharSequence sequence, Charset charset) {
		final ByteBuf tmpBuf = Unpooled.buffer();
		System.out.println(sequence);
		try {
			tmpBuf.writeCharSequence(sequence, charset);
			writeLengthEncodedInt(buf, (long) tmpBuf.readableBytes());
			buf.writeBytes(tmpBuf);
		}catch (Exception e){

			e.printStackTrace();
		}
		finally {
			tmpBuf.release();
		}
	}

	static void writeNullTerminatedString(ByteBuf buf, CharSequence sequence, Charset charset) {
		if (sequence != null) {
			buf.writeCharSequence(sequence, charset);
		}
		buf.writeByte(0);
	}

}
