package com.js.testUtils;

import java.util.ArrayList;
import java.util.Random;
import static com.js.basic.Tools.*;

public class MyTestUtils {

	public static void permute(Random random, ArrayList array) {
		for (int i = 0; i < array.size(); i++) {
			int j = i + random.nextInt(array.size() - i);
			Object tmp = array.get(i);
			array.set(i, array.get(j));
			array.set(j, tmp);
		}
	}

	public static String hexDump(byte[] byteArray) {
		return hexDump(byteArray, 0, byteArray.length);
	}

	public static String hexDump(byte[] byteArray, int offset, int length) {
		return hexDump(byteArray, offset, length, "16gza");
	}

	/**
	 * Construct a hex dump of an array of bytes
	 * 
	 * @param byteArray
	 * @param offset
	 * @param length
	 * @param options
	 *            "D+[F]*" where D is decimal digit, representing the number of
	 *            bytes in each row, and F is zero or more of: hide (z)eros;
	 *            display in (g)roups of four; display (A)bsolute offset;
	 *            include (a)scii representation to right
	 * 
	 * @return
	 */
	public static String hexDump(byte[] byteArray, int offset, int length,
			String options) {
		int groupSize = (1 << 2); // Must be power of 2

		Object[] fmt = parseOptionsString(options);
		int rowSize = (Integer) fmt[0];
		options = (String) fmt[1];
		boolean hideZeroes = options.contains("z");
		boolean groups = options.contains("g");
		boolean absoluteIndex = options.contains("A");
		boolean withASCII = options.contains("a");

		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < length) {
			int rSize = rowSize;
			if (rSize + i > length)
				rSize = length - i;
			int address = absoluteIndex ? i + offset : i;
			toHex(sb, address, 4, true, false, false);
			sb.append(": ");
			if (groups)
				sb.append("| ");
			for (int j = 0; j < rowSize; j++) {
				if (j < rSize) {
					byte val = byteArray[offset + i + j];
					if (hideZeroes && val == 0) {
						sb.append("  ");
					} else {
						toHex(sb, val, 2, false, false, false);
					}
				} else {
					sb.append("  ");
				}
				sb.append(' ');
				if (groups) {
					if ((j & (groupSize - 1)) == groupSize - 1)
						sb.append("| ");
				}
			}
			if (withASCII) {
				sb.append(' ');
				for (int j = 0; j < rSize; j++) {
					byte v = byteArray[offset + i + j];
					if (v < 0x20 || v >= 0x80)
						v = '.';
					sb.append((char) v);
					if (groups && ((j & (groupSize - 1)) == groupSize - 1)) {
						sb.append(' ');
					}
				}
			}
			sb.append('\n');
			i += rSize;
		}
		return sb.toString();
	}

}
