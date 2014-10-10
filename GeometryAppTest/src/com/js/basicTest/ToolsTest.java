package com.js.basicTest;

import com.js.testUtils.*;

import static com.js.basic.Tools.*;

public class ToolsTest extends MyTestCase {

	public void testASSERT() {
		if (!DEBUG_ONLY_FEATURES)
			return;
		String msg = "";
		ASSERT(true);

		try {
			ASSERT(false);
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		assertTrue(msg.contains("ASSERTION FAILED"));
	}

	public void testfBits() {
		assertStringsMatch("010001", dBits(17, "6"));
		assertStringsMatch(".1...1", dBits(17, "6d"));
		assertStringsMatch(" 1...1", dBits(17, "6dz"));
		assertStringsMatch("                               .", dBits(0, "32dz"));
		assertStringsMatch("   1...1", dBits(17));
	}

	public void testFormatHex() {
		assertStringsMatch("$       40", dh(64));
		assertStringsMatch("$        0", dh(0));
		assertStringsMatch("00000000", toHex(null, 0, 8, false, false, false));
		assertStringsMatch("0000_0000", toHex(null, 0, 8, false, true, false));
	}

	public void testFormatHexWithSnapshots() {
		IOSnapshot.open();
		for (int i = 0; i < 32; i++) {
			int v = 1 << i;
			System.out.println(dh(v));
			pr(dh(v, "8$zg"));
			pr(dh(v, "8$g"));
			pr(dh(v, "8$z"));
			pr(dh(v, "8$"));
			v = ~v;
			pr(dh(v, "8zg"));
			pr(dh(v, "8$g"));
			pr(dh(v, "8z"));
			pr(dh(v, "8$"));
			pr("");
		}
		IOSnapshot.close();
	}

	public void testChomp() {
		assertStringsMatch("hello", chomp("hello"));
		assertStringsMatch("hello", chomp("hello\n"));
		assertStringsMatch("hello", chomp("hello\n\n\n\n"));
	}

	public void testFormattedNumbersDisplay() {
		double[] doubleArray = { 4.2, 4.2222222, 444.2222222, 444.2256,
				444.9994, 444.9995, 999.9994, 999.9995, 0, 0.0000001,
				9999.9994, 99999.9994, 999999.9994, -9999.9994, -99999.9994,
				-999999.9994, };
		IOSnapshot.open();
		pr("Floating point:");
		for (int i = 0; i < doubleArray.length; i++) {
			pr(d(doubleArray[i], 3, 3));
			pr(d(-doubleArray[i], 3, 3));
		}
		pr(d(4.2f, 1, 0));
		pr(d(4.2f, 0, 0));
		pr(d(44.2f, 1, 0));
		pr(d(44.2f, 2, 0));
		pr("Integers:");
		int[] intArray = { 0, 1, 2, 3, 9, 10, 99, 199, 300, 999, 1000, 1200 };
		for (int i = 0; i < intArray.length; i++) {
			pr(d(intArray[i], 3));
			pr(d(-intArray[i], 3));
			pr(d(intArray[i], 3, false));
			pr(d(-intArray[i], 3, false));
			pr(d(intArray[i], 3, true));
			pr(d(-intArray[i], 3, true));
		}

		pr("Integer array:");

		pr(d(intArray));
		int[] intArray2 = { 0, 1, 2, -20, -2, 199, -199, 2000, -1200 };
		pr(d(intArray2));

		pr("Double array:");
		pr(d(doubleArray));

		pr("Float array:");
		float[] floatArray = { 4.2f, 4.2222222f, 444.2222222f, 444.2256f,
				444.9994f, 444.9995f, 999.9994f, 999.9995f, 0f, 0.0000001f,
				9999.9994f, 99999.9994f, 999999.9994f, -9999.9994f,
				-99999.9994f, -999999.9994f, };
		pr(d(floatArray));

		IOSnapshot.close();
	}

}
