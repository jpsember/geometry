package com.js.basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {

	/**
	 * Default value for 'db' conditional compilation, in case not provided
	 * within a particular method
	 */
	public static final boolean db = false;

	/**
	 * Have current thread sleep for some number of milliseconds
	 * 
	 * @param ms
	 */
	public static void sleepFor(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			report(e, "sleep interrupted");
		}
	}

	/**
	 * Construct a string describing a stack trace
	 * 
	 * @param skipCount
	 *            # stack frames to skip (actually skips 1 + skipCount, to skip
	 *            the call to this method)
	 * @param displayCount
	 *            maximum # stack frames to display
	 * @return String; iff displayCount > 1, cr's inserted after every item
	 */
	public static String stackTrace(int skipCount, int displayCount) {
		return stackTrace(1 + skipCount, displayCount, null);
	}

	public static String stackTrace() {
		return stackTrace(1, 1, null);
	}

	public static String hey(Object object) {
		return "\n" + nameOf(object) + " (" + stackTrace(1, 1, null) + ") ";
	}

	public static String hey() {
		return "\n" + stackTrace(1, 1, null) + ": ";
	}

	/**
	 * Construct stack trace for a throwable
	 * 
	 * @param t
	 *            throwable
	 * @return String
	 */
	public static String stackTrace(Throwable t) {
		return stackTrace(1, 10, t);
	}

	/**
	 * Construct string describing stack trace
	 * 
	 * @param skipCount
	 *            # stack frames to skip (actually skips 1 + skipCount, to skip
	 *            the call to this method)
	 * @param displayCount
	 *            maximum # stack frames to display
	 * @param tThrowable
	 *            containing stack trace
	 * @return String; iff displayCount > 1, cr's inserted after every item
	 */
	private static String stackTrace(int skipCount, int displayCount,
			Throwable t) {
		if (t == null)
			t = new Throwable();
		StringBuilder sb = new StringBuilder();

		StackTraceElement[] elist = t.getStackTrace();

		int s0 = 1 + skipCount;
		int s1 = s0 + displayCount;

		for (int i = s0; i < s1; i++) {
			if (i >= elist.length) {
				break;
			}
			StackTraceElement e = elist[i];
			String cn = e.getClassName();
			cn = cn.substring(cn.lastIndexOf('.') + 1);
			sb.append(cn);
			sb.append(".");
			sb.append(e.getMethodName());
			sb.append(":");
			sb.append(e.getLineNumber());
			if (displayCount > 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Simple assertion mechanism, dies if flag is false
	 * 
	 * @param flag
	 *            flag to test
	 * @param message
	 *            die with this message if flag is false
	 */
	public static void ASSERT(boolean flag, String message) {
		if (!flag)
			die("ASSERTION FAILED (" + message + ")");
	}

	/**
	 * Simple assertion mechanism, throws RuntimeException if flag is false
	 * 
	 * @param flag
	 *            flag to test
	 */
	public static void ASSERT(boolean flag) {
		if (!flag) {
			die("ASSERTION FAILED");
		}
	}

	/**
	 * Throw a RuntimeException
	 */
	public static void die() {
		die(null, null);
	}

	/**
	 * Throw a RuntimeException with a particular message
	 */
	public static void die(String message) {
		die(message, null);
	}

	/**
	 * Throw a RuntimeException containing a particular throwable
	 * 
	 * @param t
	 */
	public static void die(Throwable t) {
		die(null, t);
	}

	public static void die(String message, Throwable t) {
		if (message == null)
			message = "(no reason given)";
		message = "Dying; " + message;
		if (t == null)
			throw new RuntimeException(message);
		throw new RuntimeException(message, t);
	}

	/**
	 * Print message that code is unimplemented at current line; prints only the
	 * first time through
	 */
	public static void unimp() {
		reportOnce("TODO", null, 1);
	}

	/**
	 * Print message that code is unimplemented at current line; prints only the
	 * first time through
	 * 
	 * @param msg
	 *            additional message to display
	 */
	public static void unimp(String msg) {
		reportOnce("TODO", msg, 1);
	}

	private static final HashMap warningStrings = new HashMap();
	private static final Set oneTimeFlags = new HashSet();

	public static boolean oneTimeOnly(String flag) {
		return oneTimeFlags.add(flag);
	}

	private static void reportOnce(String type, String s, int skipCount) {
		String st = stackTrace(1 + skipCount, 1);
		st = sanitizeStackTrace(st);
		StringBuilder sb = new StringBuilder();
		sb.append("*** ");
		if (type == null) {
			type = "WARNING";
		}
		sb.append(type);
		if (s != null && s.length() > 0) {
			sb.append(": ");
			sb.append(s);
		}
		sb.append(" (");
		sb.append(st);
		sb.append(")");
		String keyString = sb.toString();

		{
			Object wr = warningStrings.get(keyString);
			if (wr == null) {
				warningStrings.put(keyString, Boolean.TRUE);
				pr(keyString);
			}
		}
	}

	/**
	 * Print warning message (first time only)
	 * 
	 * @param s
	 *            message to display
	 */
	public static void warning(String s) {
		warning(s, 1);
	}

	/**
	 * Print warning message (first time only)
	 * 
	 * @param s
	 *            message to display
	 * @param skipCount
	 *            number of calls on call stack to skip when looking for line
	 *            number to display
	 */
	public static void warning(String s, int skipCount) {
		reportOnce(null, s, 1 + skipCount);
	}

	/**
	 * Convert a boolean to "T" or "F"
	 * 
	 * @param b
	 *            boolean value
	 * @return
	 */
	public static String f(boolean b) {
		return b ? "T" : "F";
	}

	/**
	 * Convert (unsigned) integer to string representing its binary equivalent
	 * 
	 * @param word
	 * @param format
	 *            "D+F*" where D is decimal digit (number of significant bits),
	 *            and F is zero or more of { skip leading (z)eros; display
	 *            (d)ots instead of zeros }
	 * @return String
	 */
	public static String fBits(int word, String format) {

		StringBuilder sb = new StringBuilder();

		Object[] parts = parseOptionsString(format);
		int nBits = (Integer) parts[0];
		format = (String) parts[1];
		boolean filteringLeadingZeros = format.contains("z");
		boolean useDots = format.contains("d");
		char zeroChar = useDots ? '.' : '0';

		for (int j = nBits - 1; j >= 0; j--) {
			boolean nonzero = ((word & (1 << j)) != 0);
			if (j == 0 || nonzero)
				filteringLeadingZeros = false;

			if (nonzero) {
				sb.append('1');
			} else {
				sb.append(filteringLeadingZeros ? ' ' : zeroChar);
			}
		}
		return sb.toString();
	}

	/**
	 * Convert (unsigned) integer to string representing its binary equivalent,
	 * using default options
	 * 
	 * @param word
	 *            integer to display
	 * @return
	 */
	public static String fBits(int word) {
		return fBits(word, "8zd");
	}

	/**
	 * Parse an options expression which has the format "(\d+)(.*)"
	 * 
	 * @param s
	 *            string
	 * @return array [value(Integer), remainder(String)] where value is (\d+),
	 *         and remainder is (.*)
	 */
	private static Object[] parseOptionsString(String s) {
		Object[] output = { null, null };

		int cursor = 0;
		while (cursor < s.length()) {
			char c = s.charAt(cursor);
			if (c < '0' || c > '9')
				break;
			cursor++;
		}
		String digitsString = s.substring(0, cursor);
		int nDigits = Integer.parseInt(digitsString);
		output[0] = nDigits;
		output[1] = s.substring(cursor);
		return output;
	}

	public static String d(Throwable t) {
		return t.getMessage() + "\n" + stackTrace(0, 15, t);
	}

	public static String d(Boolean b) {
		return b.booleanValue() ? "T" : "F";
	}

	public static String d(Object obj) {
		String s = null;
		if (obj != null)
			s = obj.toString();
		return d(s);
	}

	public static String describe(Object obj) {
		if (obj == null)
			return "<null>";
		return d(obj.toString(), "80et") + " (" + nameOf(obj) + ")";
	}

	public static String nameOf(Object obj) {
		if (obj == null)
			return "<null>";
		return obj.getClass().getSimpleName() + ":"
				+ UniqueIdentifier.nameFor(obj);
	}

	public static String d(Map m) {
		if (m == null)
			return "null";
		StringBuilder sb = new StringBuilder("Map[\n");
		Iterator it = m.keySet().iterator();
		while (it.hasNext()) {
			Object k = it.next();
			sb.append(" ....Key '");
			sb.append(f(k.toString(), 50));
			sb.append("' -> ");
			Object v = m.get(k);
			String s = "";
			if (v != null)
				s = chomp(v.toString());

			sb.append(d(s));
			sb.append("\n");
		}
		sb.append("]\n");
		return sb.toString();
	}

	public static String d(Collection c) {
		if (c == null)
			return "<null>";
		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		Iterator it = c.iterator();
		while (it.hasNext()) {
			Object obj = it.next();
			// sb.append(' ');
			sb.append(chomp(obj.toString()));
			sb.append('\n');
		}
		sb.append("]\n");
		return sb.toString();
	}

	public static String d(char c) {
		StringBuilder sb = new StringBuilder();
		sb.append('\'');
		encodeCharacterAsSource(c, sb);
		sb.append('\'');
		return sb.toString();
	}

	/**
	 * Convert string to debug display
	 * 
	 * @param orig
	 *            String
	 * @param options
	 *            string with format "D+F*" where D is maximum length of string
	 *            (in decimal), and F is one or more of: { add (e)scape
	 *            sequences to display nonprintables as Java escape sequences;
	 *            (p)ad string to maximum length; surround with double (q)uotes;
	 *            (t)rim length to fit maximum length by replacing substring
	 *            with '...' }
	 * @return String in form [xxxxxx...xxx], with nonprintables converted to
	 *         unicode or escape sequences, and ... inserted if length is
	 *         greater than about the width of a line
	 */
	public static String d(CharSequence orig, String options) {
		Object[] parts = parseOptionsString(options);
		int maxLen = (Integer) parts[0];
		String format = (String) parts[1];

		StringBuilder sb = new StringBuilder();
		if (orig == null) {
			sb.append("<null>");
		} else {
			boolean quoted = format.contains("q");
			if (quoted)
				sb.append('"');
			if (format.contains("e")) {
				encodeStringAsJavaSource(orig, sb);
			}
			if (quoted)
				sb.append('"');
			if (format.contains("t")) {
				if (sb.length() > maxLen) {
					sb.replace(maxLen - 7, sb.length() - 4, "...");
				}
			}
		}
		if (format.contains("p")) {
			tab(sb, maxLen);
		}

		return sb.toString();
	}

	/**
	 * Convert string to debug display, using default options
	 * 
	 * @param s
	 *            String, or null
	 * @return String
	 */
	public static String d(CharSequence s) {
		return d(s, "80eqt");
	}

	/**
	 * Convert a character to a printable string by inserting Java escape
	 * sequences as necessary
	 * 
	 * @param c
	 * @param dest
	 */
	private static void encodeCharacterAsSource(char c, StringBuilder dest) {
		switch (c) {
		case '\n':
			dest.append("\\n");
			break;
		case '\t':
			dest.append("\\t");
			break;
		case '\b':
			dest.append("\\b");
			break;
		case '\r':
			dest.append("\\r");
			break;
		case '\f':
			dest.append("\\f");
			break;
		case '\'':
			dest.append("\\'");
			break;
		case '\"':
			dest.append("\"");
			break;
		case '\\':
			dest.append("\\");
			break;
		default:
			if (c >= ' ' && c < (char) 0x80) {
				dest.append(c);
			} else {
				dest.append("\\u");
				toHex(dest, c, 4, false, false, false);
			}
			break;
		}
	}

	private static void encodeStringAsJavaSource(CharSequence orig,
			StringBuilder sb) {
		for (int i = 0; i < orig.length(); i++) {
			encodeCharacterAsSource(orig.charAt(i), sb);
		}
	}

	private static final String SPACES = "                             ";

	/**
	 * Get a string consisting of n spaces
	 */
	public static CharSequence sp(int n) {
		n = Math.max(n, 0);
		if (n < SPACES.length())
			return SPACES.substring(0, n);
		StringBuilder sb = new StringBuilder(n);
		while (sb.length() < n) {
			int chunk = Math.min(n - sb.length(), SPACES.length());
			sb.append(SPACES.substring(0, chunk));
		}
		return sb;
	}

	private static final boolean isAndroid = System.getProperties()
			.getProperty("java.vendor", "other").equals("The Android Project");

	public static boolean isAndroid() {
		return isAndroid;
	}

	/**
	 * Format an int into a string
	 * 
	 * @param v
	 *            value
	 * @param width
	 *            max number of digits to display
	 * @param spaceLeadZeros
	 *            if true, right-justifies string
	 * @return String, with format siiii where s = sign (' ' or '-'), if
	 *         overflow, returns s********* of same size
	 */
	public static String f(int v, int width, boolean spaceLeadZeros) {

		// get string representation of absolute value
		String s = Integer.toString(Math.abs(v));

		// get number of spaces to pad
		int pad = width - s.length();

		StringBuilder sb = new StringBuilder();

		// if it won't fit, print stars
		if (pad < 0) {
			sb.append(v < 0 ? '-' : ' ');
			while (sb.length() < width + 1)
				sb.append('*');
		} else {
			// print padding spaces before or after number
			if (spaceLeadZeros) {
				while (pad-- > 0)
					sb.append(' ');
			}
			sb.append(v < 0 ? '-' : ' ');
			sb.append(s);
			// print trailing padding, if any required
			while (pad-- > 0)
				sb.append(' ');
		}
		return sb.toString();
	}

	/**
	 * Format a double into a string, without scientific notation
	 * 
	 * @param v
	 *            value
	 * @param iDig
	 *            number of integer digits to display
	 * @param fDig
	 *            number of fractional digits to display
	 * @return String, with format siiii.fff where s = sign (' ' or '-'), . is
	 *         present only if fDig > 0 if overflow, returns s********* of same
	 *         size
	 */
	public static String f(double v, int iDig, int fDig) {

		StringBuilder sb = new StringBuilder();

		boolean neg = false;
		if (v < 0) {
			neg = true;
			v = -v;
		}

		int[] dig = new int[iDig + fDig];

		boolean overflow = false;

		// Determine which digits will be displayed.
		// Round last digit and propagate leftward.
		{
			double n = Math.pow(10, iDig);
			if (v >= n) {
				overflow = true;
			} else {
				double v2 = v;
				for (int i = 0; i < iDig + fDig; i++) {
					n /= 10.0;
					double d = Math.floor(v2 / n);
					dig[i] = (int) d;
					v2 -= d * n;
				}
				double d2 = Math.floor(v2 * 10 / n);
				if (d2 >= 5) {
					for (int k = dig.length - 1;; k--) {
						if (k < 0) {
							overflow = true;
							break;
						}
						if (++dig[k] == 10) {
							dig[k] = 0;
						} else
							break;
					}
				}
			}
		}

		if (overflow) {
			int nDig = iDig + fDig + 1;
			if (fDig != 0)
				nDig++;
			for (int k = 0; k < nDig; k++)
				sb.append("*");
		} else {

			sb.append(' ');
			int signPos = 0;
			boolean leadZero = false;
			for (int i = 0; i < iDig + fDig; i++) {
				int digit = dig[i];
				if (!leadZero) {
					if (digit != 0 || i == iDig || (i == iDig - 1 && fDig == 0)) {
						leadZero = true;
						signPos = sb.length() - 1;
					}
				}
				if (i == iDig) {
					sb.append('.');
				}

				if (digit == 0 && !leadZero) {
					sb.append(' ');
				} else {
					sb.append((char) ('0' + digit));
				}
			}
			if (neg)
				sb.setCharAt(signPos, '-');
		}
		return sb.toString();
	}

	public static String f(double f) {
		return f(f, 5, 3);
	}

	public static String f(int f) {
		return f(f, 6, true);
	}

	public static String f(int[] ia) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < ia.length; i++) {
			if (i > 0)
				sb.append(' ');
			sb.append(ia[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	public static String f(int val, int width) {
		return f(val, width, true);
	}

	/**
	 * Add spaces to a StringBuilder until its length is at some value. Sort of
	 * a 'tab' feature, useful for aligning output.
	 * 
	 * @param sb
	 *            : StringBuilder to pad out
	 * @param len
	 *            : desired length of StringBuilder; if it is already past this
	 *            point, nothing is added to it
	 */
	public static StringBuilder tab(StringBuilder sb, int len) {
		sb.append(sp(len - sb.length()));
		return sb;
	}

	public static CharSequence fh(int n) {
		return fh(n, "8$zg");
	}

	/**
	 * Convert an unsigned integer to its hex string representation
	 * 
	 * @param n
	 * @param format
	 *            "D+[F]*" where D is decimal digit, representing number of hex
	 *            digits to display, and F is zero or more of: skip lead
	 *            (z)eros; insert underscores to display digits in (g)roups of
	 *            four; prefix with ($)
	 * @return string
	 */
	public static StringBuilder fh(int n, String format) {
		Object[] fmt = parseOptionsString(format);
		int nDig = (Integer) fmt[0];
		String flags = (String) fmt[1];
		return toHex(null, n, nDig, flags.contains("z"), flags.contains("g"),
				flags.contains("$"));
	}

	/**
	 * Format a string to be at least a certain size
	 * 
	 * @param s
	 *            string to format
	 * @param length
	 *            minimum size to pad to; negative to insert leading spaces
	 * @return blank-padded string
	 */
	public static String f(String s, int length) {
		return f(s, length, null).toString();
	}

	public static StringBuilder f(String s, int length, StringBuilder sb) {
		if (sb == null)
			sb = new StringBuilder();
		int origLen = sb.length();
		if (length >= 0) {
			sb.append(s);
			if (length > s.length())
				tab(sb, length + origLen);
		} else {
			length = -length;
			if (s.length() < length)
				tab(sb, length - s.length());
			sb.append(s);
		}
		return sb;
	}

	public static void pr(Object obj) {
		System.out.println(obj);
	}

	/**
	 * Trim trailing linefeeds from string
	 * 
	 * @param s
	 *            input
	 * @return trimmed string
	 */
	public static String chomp(String s) {
		int i = s.length();

		while (i > 0 && s.charAt(i - 1) == '\n')
			i--;
		return s.substring(0, i);
	}

	/**
	 * Convert value to hex, store in StringBuilder
	 * 
	 * @param sb
	 *            where to store result, or null
	 * @param value
	 *            value to convert
	 * @param digits
	 *            number of hex digits to output
	 * @return result
	 */
	public static StringBuilder toHex(StringBuilder sb, int value, int digits,
			boolean stripLeadingZeros, boolean groupsOfFour,
			boolean withDollarSign) {
		if (sb == null)
			sb = new StringBuilder();
		if (withDollarSign)
			sb.append('$');

		boolean nonZeroSeen = !stripLeadingZeros;

		long workValue = value;

		int shift = (digits - 1) << 2;
		while (digits-- > 0) {
			shift = digits << 2;
			int v = (int) ((workValue >> shift)) & 0xf;
			if (v != 0 || digits == 0)
				nonZeroSeen = true;

			char c;
			if (!nonZeroSeen) {
				c = ' ';

			} else {
				if (v < 10) {
					c = (char) ('0' + v);
				} else {
					c = (char) ('a' + (v - 10));
				}
			}
			sb.append(c);
			if (groupsOfFour && (digits & 3) == 0 && digits != 0) {
				if (!nonZeroSeen)
					sb.append(' ');
				else
					sb.append('_');
			}
		}
		return sb;
	}

	/**
	 * If a Throwable is non-null, display it along with a stack trace to
	 * System.out
	 * 
	 * @param t
	 *            throwable
	 * @param msg
	 *            optional message to display
	 */
	public static void report(Throwable t, String msg) {
		if (t != null) {
			pr("*** Problem; " + t + " (" + msg + ")");
			t.printStackTrace();
		}
	}

	/**
	 * Execute a system command
	 * 
	 * @param command
	 * @return array of two strings; [0]=stdout, [1]=stderr; empty strings are
	 *         replaced with null; stderr will always be null, since an
	 *         exeception is thrown if the command fails
	 */
	public static String[] systemCommand(String command) {
		String[] ret = null;
		try {
			ret = systemCommand(command, true);
		} catch (IOException e) {
			die(e);
		}
		return ret;
	}

	/**
	 * Execute a system command
	 * 
	 * @param command
	 * @param failIfError
	 *            if true, and an error occurs, throws an exception
	 * @return array of two strings; [0]=stdout, [1]=stderr; empty strings are
	 *         replaced with null
	 */
	public static String[] systemCommand(String command, boolean failIfError)
			throws IOException {
		String[] out = { null, null };
		Process p = Runtime.getRuntime().exec(command);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		StringBuilder sb = new StringBuilder();
		String s;
		while ((s = stdInput.readLine()) != null) {
			sb.append(s);
			sb.append("\n");
		}
		out[0] = sb.toString();
		sb = new StringBuilder();
		while ((s = stdError.readLine()) != null) {
			sb.append(s);
			sb.append("\n");
		}
		out[1] = sb.toString();
		for (int i = 0; i < out.length; i++) {
			if (out[i].length() == 0)
				out[i] = null;
		}
		if (failIfError && out[1] != null)
			die("Failed executing system command '" + command + "';\nstdout:\n"
					+ out[0] + "\nstderr:\n" + out[1]);
		return out;
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

	private static boolean sanitizeLineNumbers;
	private static Pattern lineNumbersPattern;

	/**
	 * Optionally replace literal line numbers that appear in warnings (and
	 * 'unimp' messages) with constant placeholders ('xxx') so that old
	 * snapshots remain valid even if the line numbers have changed.
	 */
	public static void setSanitizeLineNumbers(boolean f) {
		sanitizeLineNumbers = f;
	}

	/**
	 * Replace all line numbers within a stack trace with "xxx" so they are
	 * ignored within snapshots; has no effect if sanitize is not active
	 * 
	 * @param s
	 *            string containing stack trace
	 * @return possibly modified stack trace
	 */
	private static String sanitizeStackTrace(String s) {
		if (sanitizeLineNumbers) {
			if (lineNumbersPattern == null)
				lineNumbersPattern = Pattern.compile(":(\\d+)($|\\n)");
			Matcher m = lineNumbersPattern.matcher(s);
			s = m.replaceAll("_XXX");
		}
		return s;
	}

	/**
	 * This main() method is provided for running quick tests
	 */
	public static void main(String[] args) {
		warning("Tools.main executing.");
	}

	public static Random rnd = new Random(1965);

	/**
	 * Seed the random number generator
	 * 
	 * @param seed
	 *            seed to use; if 0, uses system clock value
	 */
	public static void seedRandom(long seed) {
		if (seed == 0)
			seed = System.currentTimeMillis();
		rnd = new Random(seed);
	}

	/**
	 * Construct a new timer
	 */
	public static void timeStamp() {
		timeStamp(null, 1);
	}

	public static void timeStamp(Object message) {
		timeStamp(message, 1);
	}

	private static long previousTime;
	private static long baseTime;

	private static void timeStamp(Object message, int skip) {
		long newTime = System.currentTimeMillis();
		String location = stackTrace(2 + skip, 1);
		if (previousTime == 0) {
			baseTime = newTime;
			previousTime = newTime;
		}
		long delta = newTime - previousTime;
		previousTime = newTime;
		StringBuilder sb = new StringBuilder();
		if (delta >= 0.1) {
			sb.append(String.format("%5.2f ", delta / 1000.0f));
		} else
			sb.append(sp(6));
		sb.append(String.format("%6.2f  ", (newTime - baseTime) / 1000.0f));
		int len = sb.length();
		sb.append(location);
		tab(sb, len + 50);
		if (message != null) {
			sb.append(' ');
			sb.append(message);
		}
		pr(sb);
	}

	/**
	 * Remove the last item from a list and return it
	 * 
	 * @param list
	 * @return
	 */
	public static Object pop(List list) {
		Object obj = list.remove(list.size() - 1);
		return obj;
	}

	private static boolean testingKnown;
	private static boolean testing;

	public static boolean testing() {
		if (!testingKnown) {
			try {
				Class.forName("com.js.testUtils.MyTest");
				testing = true;
			} catch (ClassNotFoundException e) {
			}
			testingKnown = true;
		}
		return testing;
	}

	/**
	 * Find index of string within an array of strings
	 * 
	 * @param array
	 *            array of strings
	 * @param target
	 *            string to look for
	 * @return index of string; throws IllegalArgumentException if not found
	 */
	public static int indexOfString(String[] array, String target) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(target))
				return i;
		}
		throw new IllegalArgumentException("string '" + target + "' not found");
	}

}
