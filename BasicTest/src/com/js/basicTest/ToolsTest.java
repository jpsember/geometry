package com.js.basicTest;

import com.js.testUtils.*;

import static com.js.basic.Tools.*;

public class ToolsTest extends MyTest {

	public void testASSERT() {
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
		assertStringsMatch("010001", fBits(17, "6"));
		assertStringsMatch(".1...1", fBits(17, "6d"));
		assertStringsMatch(" 1...1", fBits(17, "6dz"));
		assertStringsMatch("                               .", fBits(0, "32dz"));
		assertStringsMatch("   1...1", fBits(17));
	}

	public void testFormatHex() {
		assertStringsMatch("$       40", fh(64));
		assertStringsMatch("$        0", fh(0));
		assertStringsMatch("00000000", toHex(null, 0, 8, false, false,false));
		assertStringsMatch("0000_0000", toHex(null, 0, 8, false, true,false));
	}

	public void testFormatHexWithSnapshots() {
		IOSnapshot.open();
		for (int i = 0; i < 32; i++) {
			int v = 1 << i;
			System.out.println(fh(v));
			pr(fh(v, "8$zg"));
			pr(fh(v, "8$g"));
			pr(fh(v, "8$z"));
			pr(fh(v, "8$"));
			v = ~v;
			pr(fh(v, "8zg"));
			pr(fh(v, "8$g"));
			pr(fh(v, "8z"));
			pr(fh(v, "8$"));
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
		double[] v = { 4.2, 4.2222222, 444.2222222, 444.2256, 444.9994,
				444.9995, 999.9994, 999.9995, 0, 0.0000001,

		};
		IOSnapshot.open();
		pr("Floating point:");
		for (int i = 0; i < v.length; i++) {
			pr(f(v[i], 3, 3));
			pr(f(-v[i], 3, 3));
		}
		pr(f(4.2f, 1, 0));
		pr(f(4.2f, 0, 0));
		pr(f(44.2f, 1, 0));
		pr(f(44.2f, 2, 0));
		pr("Integers:");
		int[] iv = { 0, 1, 2, 3, 9, 10, 99, 199, 300, 999, 1000, 1200 };
		for (int i = 0; i < iv.length; i++) {
			pr(f(iv[i], 3));
			pr(f(-iv[i], 3));
			pr(f(iv[i], 3,false));
			pr(f(-iv[i], 3,false));
			pr(f(iv[i], 3,true));
			pr(f(-iv[i], 3,true));
	}

		pr("Integer array:");
		
		int[] iv2 = {0,1,2,-20,-2,199,-199,2000,-1200};
		pr(f(iv2));
		
		IOSnapshot.close();
	}

}
