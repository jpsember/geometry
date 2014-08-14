package com.js.basic;

import static com.js.basic.JSMath.*;
import static com.js.basic.Tools.*;

public class StringUtil {

	/**
	 * Generate a random string
	 * 
	 * @param maxLength
	 *            maximum length of string
	 * @return random string
	 */
	public static String randomString(int maxLength) {

		StringBuilder sb = new StringBuilder();
		if (maxLength > 0) {
			int ln = rnd.nextInt(maxLength);
			ln = clampInt(ln, 1, maxLength);
			for (int i = 0; i < ln; i++) {
				if (rnd.nextInt(6) == 0 && i > 0 && i < ln - 1
						&& sb.charAt(i - 1) != ' ')
					sb.append(' ');
				else
					sb.append((char) (rnd.nextInt(26) + 'a'));
			}
		}
		return sb.toString();
	}

	public static StringBuilder encode(String s) {
		return encode(s, null);
	}

	/**
	 * Encode a string so some characters are escaped. Converts ASCII 0 => "\0";
	 * linefeeds => "\n"; "\" => "\\"; "|" => "\}"
	 * 
	 * @param s
	 * @return
	 */
	public static StringBuilder encode(String s, StringBuilder sb) {
		if (sb == null)
			sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case 0:
				sb.append("\\0");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '|':
				sb.append("\\}");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb;
	}

	public static StringBuilder decode(CharSequence s) {
		return decode(s, null);
	}

	public static StringBuilder decode(CharSequence s, StringBuilder sb) {
		if (sb == null)
			sb = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (db)
				pr("Processing character " + (int) c + ", buffer now:\n" + sb);
			if (c == '\\') {
				i += 1;
				if (i == s.length())
					throw new IllegalArgumentException("could not decode \""
							+ s + "\"");
				c = s.charAt(i);
				switch (c) {
				case '0':
					sb.append((char) 0);
					break;
				case 'n':
					sb.append('\n');
					break;
				case '}':
					sb.append('|');
					break;
				default:
					sb.append(c);
					break;
				}
			} else {
				sb.append(c);
			}
			i += 1;
		}
		return sb;
	}
}
