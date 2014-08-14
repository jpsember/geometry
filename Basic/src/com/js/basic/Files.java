package com.js.basic;

import java.io.*;

public class Files {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	public static String readTextFile(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		return readTextFile(reader);
	}

	private static String readTextFile(BufferedReader input) throws IOException {
		StringBuilder sb = new StringBuilder();
		try {
			String line = null;
			/*
			 * Readline strips newlines, and returns null only for the end of
			 * the stream.
			 */
			while ((line = input.readLine()) != null) {
				sb.append(line);
				sb.append(LINE_SEPARATOR);
			}
		} finally {
			input.close();
		}
		return sb.toString();
	}

	/**
	 * Read a file into a string
	 * 
	 * @param path
	 *            file to read
	 * @return String
	 */
	public static String readTextFile(File file) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(file));
		return readTextFile(input);
	}

	public static byte[] readBinaryFile(File file) throws IOException {
		RandomAccessFile f = new RandomAccessFile(file, "r");
		byte[] b = new byte[(int) f.length()];
		int bytesRead = f.read(b);
		if (bytesRead != b.length)
			throw new IOException("failed to read all bytes from " + file);
		return b;
	}

	public static byte[] readBytes(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = stream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();

		return buffer.toByteArray();
	}

	public static void writeBinaryFile(File file, byte[] contents)
			throws IOException {
		FileOutputStream f = new FileOutputStream(file);
		f.write(contents);
		f.close();
	}

	public static void writeTextFile(File file, String content,
			boolean onlyIfChanged) throws IOException {
		if (onlyIfChanged) {
			if (file.isFile()) {
				String currentContents = readTextFile(file);
				if (currentContents.equals(content))
					return;
			}
		}
		BufferedWriter w = new BufferedWriter(new FileWriter(file));
		w.write(content);
		w.close();
	}

	public static void writeTextFile(File file, String content)
			throws IOException {
		writeTextFile(file, content, false);
	}

	public static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
}
