package com.js.android;

import java.io.*;

import com.js.basic.Tools;

/**
 * Install a filter on System.out so multiple linefeeds are NOT filtered
 * by the Android logger.  
 */
public class AndroidSystemOutFilter {

	private static boolean alreadyInstalled;

	public static void install() {
		if (!alreadyInstalled) {
			if (Tools.isAndroid())
				System.setOut(new OurFilter());
			alreadyInstalled = true;
		}
	}

	private static class OurFilter extends PrintStream {
		public OurFilter() {
			super(System.out, true);
		}

		// This seems to be the only method I need to override...
		@Override
		public void write(byte[] buf, int off, int len) {
			for (int i = 0; i < len; i++) {
				byte k = buf[off + i];
				if (k == '\n') {
					super.write(' ');
				}
				super.write(k);
			}
		}
	}
}
