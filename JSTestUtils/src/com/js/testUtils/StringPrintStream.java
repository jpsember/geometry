package com.js.testUtils;

import static com.js.basic.Tools.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * PrintStream that writes to an internal string; used by IOSnapshot to
 * temporarily redirect System.out and System.err
 */
public class StringPrintStream extends PrintStream {

	public static StringPrintStream build() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		return new StringPrintStream(byteArrayOutputStream);
	}
	
	/**
	 * Get contents printed to this stream
	 * @return string
	 */
	public String content() {
		String content = null;
		try {
			content = byteArrayOutputStream.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			die(e);
		}
		return content;
	}

	private StringPrintStream(ByteArrayOutputStream ba) {
		super(ba,true);
		byteArrayOutputStream = ba;
	}
	
	// Byte array this stream will wrap
	private ByteArrayOutputStream byteArrayOutputStream;
}
