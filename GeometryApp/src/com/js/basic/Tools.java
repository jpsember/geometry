package com.js.basic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class Tools {

  public static class DieException extends RuntimeException {
    public DieException(String detailMessage, Throwable throwable) {
      super(detailMessage, throwable);
    }

    public DieException(String detailMessage) {
      super(detailMessage);
    }
  }

  /**
   * Generate code that should be 'debug only', i.e., preproduction?
   */
  public static final boolean DEBUG_ONLY_FEATURES = true;

  public static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;

  /**
   * A do-nothing method that can be called to avoid 'unused import' warnings
   * related to this class
   */
  public final static void doNothing() {
  }

  /**
   * Default value for 'db' conditional compilation, in case not provided within
   * a particular method. It is always false, so the following idiom within a
   * method will not generate code:
   * 
   * <pre>
   * void foo() {
   *   if (db)
   *     pr(&quot;This will not print anything&quot;);
   * }
   * </pre>
   * 
   * By contrast, this will print to the console:
   * 
   * <pre>
   * void foo() {
   *   final boolean db = true;
   *   if (db)
   *     pr(&quot;This will print&quot;);
   * }
   * </pre>
   * 
   */
  public static final boolean db = false;

  /**
   * Have current thread sleep for some number of milliseconds
   * 
   * @param timeInMilliseconds
   */
  public static void sleepFor(int timeInMilliseconds) {
    try {
      Thread.sleep(timeInMilliseconds);
    } catch (InterruptedException e) {
      pr("sleep interrupted: " + e);
    }
  }

  public static String stackTrace() {
    return stackTrace(1, 1, null);
  }

  /**
   * Construct stack trace for a throwable
   * 
   * @param t
   *          throwable
   * @return String
   */
  public static String stackTrace(Throwable t) {
    return stackTrace(0, 10, t);
  }

  /**
   * Construct string describing stack trace
   * 
   * @param skipCount
   *          number of stack frames to skip
   * @param displayCount
   *          maximum # stack frames to display
   * @param t
   *          Throwable containing stack trace, or null to generate one
   * @return String; iff displayCount > 1, cr's inserted after every item
   */
  public static String stackTrace(int skipCount, int displayCount, Throwable t) {
    if (t == null) {
      t = new Throwable();
      skipCount++;
    }
    StringBuilder sb = new StringBuilder();
    // sb.append(" {Thread" + nameOf(Thread.currentThread(), false) + "} ");

    StackTraceElement[] elist = t.getStackTrace();

    int s0 = skipCount;
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
   * Construct string describing current stack frame
   * 
   * @see stackTrace(int,int,Throwable)
   */
  public static String stackTrace(int skipCount, int displayCount) {
    return stackTrace(1 + skipCount, displayCount, null);
  }

  /**
   * Convenience method equivalent to stackTrace(1,1,null)
   */
  public static String stackTrace(int skipCount) {
    return stackTrace(1 + skipCount, 1, null);
  }

  /**
   * Simple assertion mechanism, throws a DieException if flag is false; does
   * nothing if DEBUG_ONLY_FEATURES is false
   * 
   * @param flag
   *          flag to test
   * @param message
   *          die with this message if flag is false
   */
  public static void ASSERT(boolean flag, String message) {
    if (DEBUG_ONLY_FEATURES) {
      if (!flag)
        die("ASSERTION FAILED (" + message + ")");
    }
  }

  /**
   * Same as {@link #ASSERT(boolean,String) ASSERT(boolean,String)}, but with
   * generic message
   */
  public static void ASSERT(boolean flag) {
    if (DEBUG_ONLY_FEATURES) {
      if (!flag) {
        die("ASSERTION FAILED");
      }
    }
  }

  /**
   * Throw a DieException
   */
  public static void die() {
    die(null, null);
  }

  /**
   * Same as {@link #die() die}, but with a particular message
   */
  public static void die(String message) {
    die(message, null);
  }

  /**
   * Same as {@link #die() die}, but with a Throwable
   */
  public static void die(Throwable t) {
    die(null, t);
  }

  /**
   * Same as {@link #die() die()}, but with optional message and Throwable
   */
  public static void die(String detailMessage, Throwable throwable) {
    if (detailMessage == null)
      detailMessage = "(no reason given)";
    detailMessage = "Dying; " + detailMessage;
    if (throwable == null)
      throw new DieException(detailMessage);
    throw new DieException(detailMessage, throwable);
  }

  /**
   * Print message that code is unimplemented at current line; prints a specific
   * string only once. Thread safe.
   */
  public static void unimp() {
    reportOnce("TODO", null, 1);
  }

  /**
   * Same as {@link #unimp() unimp}, but with optional additional message
   * 
   * @param msg
   *          additional message, or null
   */
  public static void unimp(String msg) {
    reportOnce("TODO", msg, 1);
  }

  private static void reportOnce(String type, String optionalMessage,
      int stackFrameSkipCount) {
    String st = stackTrace(1 + stackFrameSkipCount, 1, null);
    st = sanitizeStackTrace(st);
    StringBuilder sb = new StringBuilder();
    sb.append("*** ");
    if (type == null) {
      type = "WARNING";
    }
    sb.append(type);
    if (optionalMessage != null && optionalMessage.length() > 0) {
      sb.append(": ");
      sb.append(optionalMessage);
    }
    sb.append(" (");
    sb.append(st);
    sb.append(")");
    String message = sb.toString();

    boolean wasAdded = false;
    synchronized (sWarningStrings) {
      wasAdded = sWarningStrings.add(message);
    }
    if (wasAdded) {
      pr(message);
    }
  }

  /**
   * Print message that code is unimplemented at current line; prints a specific
   * string only once. Thread safe.
   * 
   * @param warningMessage
   *          message to display with warning
   */
  public static void warning(String warningMessage) {
    warning(warningMessage, 1);
  }

  /**
   * Same as {@link #warning() warning}, but with skip count indicating how far
   * back in stack to look for the caller entry to display
   * 
   * @param warningMessage
   *          message to display
   * @param stackFrameSkipCount
   *          number of calls on call stack to skip when looking for caller
   *          entry
   */
  public static void warning(String warningMessage, int stackFrameSkipCount) {
    reportOnce(null, warningMessage, 1 + stackFrameSkipCount);
  }

  /**
   * Convert integer to a string representing its binary equivalent
   * 
   * @param intValue
   *          integer to display
   * @param format
   *          "D+F*" where D is decimal digit (number of significant bits), and
   *          F is zero or more of { skip leading (z)eros; display (d)ots
   *          instead of zeros }
   */
  public static String dBits(int intValue, String format) {

    StringBuilder sb = new StringBuilder();

    Object[] parts = parseOptionsString(format);
    int nBits = (Integer) parts[0];
    format = (String) parts[1];
    boolean filteringLeadingZeros = format.contains("z");
    boolean useDots = format.contains("d");
    char zeroChar = useDots ? '.' : '0';

    for (int j = nBits - 1; j >= 0; j--) {
      boolean nonzero = ((intValue & (1 << j)) != 0);
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
   * Same as {@link #dBits(int,String) dBits(int,String)}, but with default
   * format
   * 
   * @param intValue
   *          integer to display
   */
  public static String dBits(int intValue) {
    return dBits(intValue, "8zd");
  }

  /**
   * Parse an options expression which has the format "(\d+)(.*)"
   * 
   * @param options
   *          string
   * @return array [value(Integer), remainder(String)] where value is (\d+), and
   *         remainder is (.*)
   */
  public static Object[] parseOptionsString(String options) {
    Object[] output = { null, null };

    int cursor = 0;
    while (cursor < options.length()) {
      char c = options.charAt(cursor);
      if (c < '0' || c > '9')
        break;
      cursor++;
    }
    String digitsString = options.substring(0, cursor);
    int nDigits = Integer.parseInt(digitsString);
    output[0] = nDigits;
    output[1] = options.substring(cursor);
    return output;
  }

  /**
   * Describe a throwable, including its message and much of its stack trace
   * 
   * @param throwable
   */
  public static String d(Throwable throwable) {
    return throwable.getMessage() + "\n" + stackTrace(0, 15, throwable);
  }

  /**
   * Describe a Boolean as either "T" or "F"
   * 
   * @param b
   */
  public static String d(Boolean b) {
    return b.booleanValue() ? "T" : "F";
  }

  /**
   * Describe an integer by formatting it to a string
   * 
   * @param v
   *          value
   * @param width
   *          max number of digits to display
   * @param spaceLeadZeros
   *          if true, right-justifies string
   * @return String, with format siiii where s = sign (' ' or '-'), if overflow,
   *         returns s********* of same size
   */
  public static String d(int v, int width, boolean spaceLeadZeros) {

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
   * Describe a double by formatting it to a string, without scientific notation
   * 
   * @param v
   *          value
   * @param iDig
   *          number of integer digits to display
   * @param fDig
   *          number of fractional digits to display
   * @return String, with format siiii.fff where s = sign (' ' or '-'), . is
   *         present only if fDig > 0 if overflow, returns s********* of same
   *         size
   */
  public static String d(double v, int iDig, int fDig) {

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
      boolean fractionalNonZero = false;
      int decIndex = -1;
      for (int i = 0; i < iDig + fDig; i++) {
        int digit = dig[i];
        if (!leadZero) {
          if (digit != 0 || i == iDig - 1) {
            leadZero = true;
            signPos = sb.length() - 1;
          }
        }
        if (i == iDig) {
          decIndex = sb.length();
          sb.append('.');
        }

        if (digit == 0 && !leadZero) {
          sb.append(' ');
        } else {
          sb.append((char) ('0' + digit));
          if (i >= iDig && digit != 0)
            fractionalNonZero = true;
        }
      }
      if (neg)
        sb.setCharAt(signPos, '-');
      if (!fractionalNonZero && decIndex >= 0) {
        for (int i = decIndex; i < sb.length(); i++)
          sb.setCharAt(i, ' ');
      }
    }
    return sb.toString();
  }

  public static String d(double f) {
    int fractionalDigits = 4;
    return d(f, 5, fractionalDigits);
  }

  /**
   * Convert an angle, in radians, to one in degrees and dump
   */
  public static String da(double angleInRadians) {
    final float PI = (float) Math.PI;
    final float M_DEG = PI / 180.0f;
    return d(angleInRadians / M_DEG, 3, 2);
  }

  public static String d(int f) {
    return d(f, 6, true);
  }

  public static String d(int val, int width) {
    return d(val, width, true);
  }

  /**
   * Describe an integer as a hex value
   * 
   * @param intValue
   */
  public static CharSequence dh(int intValue) {
    return dh(intValue, "8$zg");
  }

  /**
   * Convert an integer to its hex string representation
   * 
   * @param n
   * @param format
   *          "D+[F]*" where D is decimal digit, representing number of hex
   *          digits to display, and F is zero or more of: skip lead (z)eros;
   *          insert underscores to display digits in (g)roups of four; prefix
   *          with ($)
   * @return string
   */
  public static StringBuilder dh(int n, String format) {
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
   *          string to format
   * @param length
   *          minimum size to pad to; negative to insert leading spaces
   * @return blank-padded string
   */
  public static String d(String s, int length) {
    return d(s, length, null).toString();
  }

  /**
   * Format a string to be at least a certain size
   * 
   * @param string
   *          string to format
   * @param length
   *          minimum size to padd to; negative to insert leading spaces
   * @param sb
   *          StringBuilder to receive formatted string, or null to construct
   *          one
   * @return StringBuilder that received the string
   */
  public static StringBuilder d(String string, int length, StringBuilder sb) {
    if (sb == null)
      sb = new StringBuilder();
    int origLen = sb.length();
    if (length >= 0) {
      sb.append(string);
      if (length > string.length())
        tab(sb, length + origLen);
    } else {
      length = -length;
      if (string.length() < length)
        tab(sb, length - string.length());
      sb.append(string);
    }
    return sb;
  }

  /**
   * Describe a float array
   * 
   * @param floatArray
   *          float array
   */
  public static String d(float[] floatArray) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < floatArray.length; i++) {
      sb.append(d(floatArray[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Describe a double array
   */
  public static String d(double[] doubleArray) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < doubleArray.length; i++) {
      sb.append(d(doubleArray[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Describe an int array
   */
  public static String d(int[] intArray) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < intArray.length; i++) {
      sb.append(d(intArray[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Describe an array of strings
   */
  public static String d(String[] strArray) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < strArray.length; i++) {
      if (i > 0)
        sb.append(',');
      sb.append(d(strArray[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Describe an object (which may be null)
   * 
   * @param obj
   */
  public static String d(Object obj) {
    String s = null;
    if (obj != null)
      s = obj.toString();
    return d(s);
  }

  public static String d(JSONObject map) {
    try {
      return map.toString(2);
    } catch (JSONException e) {
      warning("caught:" + e);
      return map.toString();
    }
  }

  public static String d(JSONArray array) {
    try {
      return array.toString(2);
    } catch (JSONException e) {
      warning("caught:" + e);
      return array.toString();
    }
  }

  /**
   * Describe a Map
   * 
   * @param map
   *          Map, or null
   * @return description of map
   */
  public static String d(Map map) {
    if (map == null)
      return "null";
    StringBuilder sb = new StringBuilder("Map[\n");
    Iterator it = map.keySet().iterator();
    while (it.hasNext()) {
      Object k = it.next();
      sb.append(" ....Key '");
      sb.append(d(k.toString(), 50));
      sb.append("' -> ");
      Object v = map.get(k);
      String s = "";
      if (v != null) {
        s = chomp(v.toString());
        // sb.append(v.getClass().getSimpleName() + ":");
      }
      sb.append(d(s));
      sb.append("\n");
    }
    sb.append("]\n");
    return sb.toString();
  }

  public static String d(Collection c, boolean withNewlines) {
    if (c == null)
      return "<null>";
    StringBuilder sb = new StringBuilder();
    char newlineChar = withNewlines ? '\n' : ' ';
    sb.append("[");
    sb.append(newlineChar);
    Iterator it = c.iterator();
    while (it.hasNext()) {
      Object obj = it.next();
      sb.append(chomp(obj.toString()));
      sb.append(newlineChar);
    }
    sb.append("]");
    sb.append(newlineChar);
    return sb.toString();
  }

  public static String d(Collection c) {
    return d(c, (c != null && c.size() > 6));
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
   *          String
   * @param options
   *          string with format "D+F*" where D is maximum length of string (in
   *          decimal), and F is one or more of: { add (e)scape sequences to
   *          display nonprintables as Java escape sequences; (p)ad string to
   *          maximum length; surround with double (q)uotes; (t)rim length to
   *          fit maximum length by replacing substring with '...' }
   * @return String in form [xxxxxx...xxx], with nonprintables converted to
   *         unicode or escape sequences, and ... inserted if length is greater
   *         than about the width of a line
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
      } else {
        sb.append(orig);
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
   *          String, or null
   * @return String
   */
  public static String d(CharSequence s) {
    return d(s, "80eqt");
  }

  /**
   * Describe an object, which may be null, by converting it to a string and
   * appending its symbolic name
   * 
   * @param obj
   * @return String description of object
   */
  public static String describe(Object obj) {
    if (obj == null)
      return "<null>";
    return d(obj.toString(), "80et") + " (" + nameOf(obj) + ")";
  }

  public static String nameOf(Object obj) {
    return nameOf(obj, true);
  }

  /**
   * Get the symbolic name of an object, optionally including its class name
   * 
   * @param obj
   * @param includeClassName
   * @return String name of object
   */
  public static String nameOf(Object obj, boolean includeClassName) {
    if (obj == null)
      return "<null>";
    String identifier = "";
    if (DEBUG_ONLY_FEATURES)
      identifier = UniqueIdentifier.nameFor(obj);
    if (!includeClassName)
      return identifier;
    String s = obj.getClass().getSimpleName() + ":" + identifier;
    if (obj instanceof Freezable) {
      if (((Freezable) obj).isFrozen())
        s += " (frozen) ";
      else
        s += " (mutable)";
    }
    return s;
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
   * Get a string consisting of zero or more spaces
   */
  public static CharSequence spaces(int numberOfSpaces) {
    numberOfSpaces = Math.max(numberOfSpaces, 0);
    if (numberOfSpaces < SPACES.length())
      return SPACES.substring(0, numberOfSpaces);
    StringBuilder sb = new StringBuilder(numberOfSpaces);
    while (sb.length() < numberOfSpaces) {
      int chunk = Math.min(numberOfSpaces - sb.length(), SPACES.length());
      sb.append(SPACES.substring(0, chunk));
    }
    return sb;
  }

  public static boolean isAndroid() {
    if (!sAndroidKnown) {
      synchronized (Tools.class) {
        sIsAndroid = System.getProperties().getProperty("java.vendor", "other")
            .equals("The Android Project");
        sAndroidKnown = true;
      }
    }
    return sIsAndroid;
  }

  public static boolean alwaysFalse() {
    return false;
  }

  /**
   * Add spaces to a StringBuilder until its length is at some value. Sort of a
   * 'tab' feature, useful for aligning output.
   * 
   * @param sb
   *          : StringBuilder to pad out
   * @param len
   *          : desired length of StringBuilder; if it is already past this
   *          point, nothing is added to it
   */
  public static StringBuilder tab(StringBuilder sb, int len) {
    sb.append(spaces(len - sb.length()));
    return sb;
  }

  /**
   * Print an object to System.out, with a newline
   * 
   * @param obj
   */
  public static void pr(Object obj) {
    System.out.println(obj);
  }

  /**
   * Print an object to System.out, without a newline
   * 
   * @param obj
   */
  public static void prr(Object obj) {
    System.out.print(obj);
  }

  /**
   * Trim trailing linefeeds from string
   * 
   * @param s
   *          input
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
   *          where to store result, or null
   * @param value
   *          value to convert
   * @param digits
   *          number of hex digits to output
   * @return result
   */
  public static StringBuilder toHex(StringBuilder sb, int value, int digits,
      boolean stripLeadingZeros, boolean groupsOfFour, boolean withDollarSign) {
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
   * Optionally replace literal line numbers that appear in warnings (and
   * 'unimp' messages) with constant placeholders ('xxx') so that old snapshots
   * remain valid even if the line numbers have changed.
   */
  public static void setSanitizeLineNumbers(boolean f) {
    sSanitizeLineNumbersFlag = f;
  }

  /**
   * Replace all line numbers within a stack trace with "xxx" so they are
   * ignored within snapshots; has no effect if sanitize is not active
   * 
   * @param s
   *          string containing stack trace
   * @return possibly modified stack trace
   */
  private static String sanitizeStackTrace(String s) {
    if (sSanitizeLineNumbersFlag) {
      if (sLineNumbersPattern == null)
        sLineNumbersPattern = Pattern.compile(":(\\d+)($|\\n)");
      Matcher m = sLineNumbersPattern.matcher(s);
      s = m.replaceAll("_XXX");
    }
    return s;
  }

  /**
   * Generate a time stamp to the console, indicating how much time has elapsed
   * since the last such time stamp was requested
   */
  public static void timeStamp() {
    timeStamp(null, 1);
  }

  /**
   * Same as {@link #timeStamp() timeStamp}, but with optional message
   * 
   * @param message
   *          object to derive message from (via toString()), or null
   */
  public static void timeStamp(Object message) {
    timeStamp(message, 1);
  }

  /**
   * Same as {@link #timeStamp(Object) timeStamp}, but with stack frame skip
   * count
   * 
   * @param message
   *          object to derive message from (via toString()), or null
   * @param stackFrameSkipCount
   *          depth of desired entry with stack frame
   */
  private static void timeStamp(Object message, int stackFrameSkipCount) {
    long newTime = System.currentTimeMillis();
    String location = stackTrace(1 + stackFrameSkipCount, 1, null);
    long delta;
    synchronized (Tools.class) {
      if (sTimeStampPreviousTime == 0) {
        sTimeStampBaseTime = newTime;
        sTimeStampPreviousTime = newTime;
      }
      delta = newTime - sTimeStampPreviousTime;
      sTimeStampPreviousTime = newTime;
    }
    StringBuilder sb = new StringBuilder();
    if (delta >= 0.1) {
      sb.append(String.format("%5.2f ", delta / 1000.0f));
    } else
      sb.append(spaces(6));
    sb.append(String
        .format("%6.2f  ", (newTime - sTimeStampBaseTime) / 1000.0f));
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
   */
  public static <T> T pop(List<T> list) {
    return list.remove(list.size() - 1);
  }

  /**
   * Look at last item in a list, without removing it
   */
  public static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  /**
   * Remove a contiguous sequence of elements from a list
   */
  public static <T> void remove(List<T> list, int start, int count) {
    list.subList(start, start + count).clear();
  }

  public static <T> T getMod(List<T> list, int index) {
    return list.get(myMod(index, list.size()));
  }

  public static int myMod(int value, int divisor) {
    if (divisor <= 0)
      throw new IllegalArgumentException();
    int k = value % divisor;
    if (value < 0) {
      if (k != 0)
        k = divisor + k;
    }
    return k;
  }

  /**
   * Remove an item from a list, and fill gap with last element
   * 
   * @param list
   * @param index
   *          index of item to remove
   * @return the removed item
   */
  public static <T> T removeAndFill(List<T> list, int index) {
    int size = list.size();
    T element = list.get(index);
    int lastIndex = size - 1;
    if (index != lastIndex)
      list.set(index, list.get(lastIndex));
    list.remove(lastIndex);
    return element;
  }

  public static void swap(List list, int aIndex, int bIndex) {
    if (aIndex == bIndex)
      return;
    Object temp = list.get(aIndex);
    list.set(aIndex, list.get(bIndex));
    list.set(bIndex, temp);
  }

  /**
   * Determine if two objects are equal; returns true if both are null
   */
  public static <T> boolean equal(T obj1, T obj2) {
    if (obj1 == null || obj2 == null)
      return obj1 == obj2;
    return obj1.equals(obj2);
  }

  /**
   * Determine if program is running unit tests. Thread safe.
   */
  public static boolean testing() {
    if (!sTestingKnown) {
      synchronized (Tools.class) {
        try {
          Class.forName("com.js.testUtils.MyTest");
          sTesting = true;
        } catch (Throwable e) {
        }
      }
      sTestingKnown = true;
    }
    return sTesting;
  }

  public static int[] toArray(List<Integer> list) {
    int[] ret = new int[list.size()];
    Iterator<Integer> iterator = list.iterator();
    for (int i = 0; i < ret.length; i++) {
      ret[i] = iterator.next().intValue();
    }
    return ret;
  }

  /**
   * Get a copy of a Freezable object. A generic class method to eliminate
   * casting that is sometimes required when using the interface method. This
   * has a different name than that method to avoid confusion
   */
  public static <T extends Freezable> T copyOf(T orig) {
    return (T) orig.getCopy();
  }

  /**
   * Get a mutable copy of a Freezable object. A generic class method to
   * eliminate casting that is sometimes required when using the interface
   * method. This has a different name than that method to avoid confusion
   */
  public static <T extends Freezable> T mutableCopyOf(T orig) {
    return (T) orig.getMutableCopy();
  }

  /**
   * Get a frozen copy of a Freezable object. A generic class method to
   * eliminate casting that is sometimes required when using the interface
   * method. This has a different name than that method to avoid confusion
   */
  public static <T extends Freezable> T frozen(T orig) {
    return (T) orig.getFrozenCopy();
  }

  /**
   * If item is frozen, construct a mutable copy; otherwise, return original
   */
  public static <T extends Freezable> T mutable(T orig) {
    T out;
    if (!orig.isFrozen())
      out = orig;
    else
      out = (T) orig.getMutableCopy();
    return (T) out;
  }

  /**
   * Make a Freezable object frozen, if not already
   */
  public static <T extends Freezable> T freeze(T obj) {
    obj.freeze();
    return obj;
  }

  private static boolean sSanitizeLineNumbersFlag;
  private static Pattern sLineNumbersPattern;
  private static long sTimeStampPreviousTime;
  private static long sTimeStampBaseTime;
  private static final Set sWarningStrings = new HashSet();
  private static boolean sTestingKnown;
  private static boolean sTesting;
  private static boolean sAndroidKnown;
  private static boolean sIsAndroid;

}
