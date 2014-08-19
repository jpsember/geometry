package com.js.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.js.basic.Tools.*;

/**
 * 
 * 
 * 
 * Here's a list of JSON types and their parsed Java equivalents:
 * 
 * object => Map
 * 
 * array => List
 * 
 * string => String
 * 
 * number => Double
 * 
 * true => Boolean.TRUE
 * 
 * false => Boolean.FALSE
 * 
 * null => null
 * 
 * 
 */
public class JSONParser {

	private JSONParser() {
	}

	/**
	 * Construct a parser for a Java data structure that has already been parsed
	 * from a JSON string
	 * 
	 * @param javaObject
	 *            a Java object, corresponding to one of the JSON types; see
	 *            docs at start of class
	 * @return parser
	 */
	public static JSONParser parserFor(Object javaObject) {
		JSONParser p = new JSONParser();
		// Construct a list that contains this single object
		ArrayList topLevelList = new ArrayList();
		topLevelList.add(javaObject);
		p.startProcessing(topLevelList);
		return p;
	}

	/**
	 * Constructor. Given a JSON string, parses it into Java data structures,
	 * which subsequent method calls can iterate over.
	 * 
	 * @param jsonString
	 */
	public JSONParser(String jsonString) {
		if (db)
			pr("JSONParser constructed for:\n " + jsonString);
		setTrace(db);

		try {
			InputStream stream = new ByteArrayInputStream(
					jsonString.getBytes("UTF-8"));
			readValueFromStream(stream);
		} catch (UnsupportedEncodingException e) {
			throw new JSONException(e);
		}
	}

	/**
	 * Constructor. Given an InputStream containing a JSON-formatted string,
	 * parses it into Java data structures, which subsequent method calls can
	 * iterate over.
	 * 
	 * @param jsonString
	 */
	public JSONParser(InputStream stream) {
		readValueFromStream(stream);
	}

	/**
	 * Determine if the current list or map contains more elements. If not
	 * within a list or map, this will return true until next() (or one of its
	 * variants) is called.
	 * 
	 * @return
	 */
	public boolean hasNext() {
		return mPeekValueAvailable || this.mIterator.hasNext();
	}

	/**
	 * Determine the next value without consuming it
	 * 
	 * @return
	 */
	public Object peekNext() {
		if (!mPeekValueAvailable) {
			if (mValueForKeyReady) {
				mValueForKeyReady = false;
				mPeekValue = mValueForLastKey;
			} else {
				mPeekValue = this.mIterator.next();
			}
			mPeekValueAvailable = true;
		}
		return mPeekValue;
	}

	public Object next() {
		Object val = peekNext();
		mPeekValueAvailable = false;
		return val;
	}

	/**
	 * If next value is null, read it and return true; else don't read it and
	 * return false
	 * 
	 * @return
	 */
	public boolean nextIfNull() {
		boolean wasNull = peekNext() == null;
		if (wasNull)
			next();
		return wasNull;
	}

	/**
	 * Read next key from map, and prime its value as the next object returned
	 * by a call to next(), nextInt(), etc
	 * 
	 * @return
	 */
	public String nextKey() {
		if (mCurrentMap == null)
			throw new IllegalStateException("not iterating within map");
		// In case user didn't read the value for the previous key, dispose of
		// it
		this.mValueForKeyReady = false;
		String key = nextString();
		this.mValueForLastKey = mCurrentMap.get(key);
		this.mValueForKeyReady = true;
		return key;
	}

	public void setTrace(boolean t) {
		mTrace = t;
		if (mTrace)
			warning("enabling trace, called from " + stackTrace(1, 1));
	}

	public int nextInt() {
		return ((Double) next()).intValue();
	}

	public double nextDouble() {
		return ((Double) next()).doubleValue();
	}

	public boolean nextBoolean() {
		return (Boolean) next();
	}

	public String nextString() {
		String s = (String) next();
		return s;
	}

	/**
	 * Starts iterating through the current object, which is assumed to be a
	 * list
	 */
	public void enterList() {
		ArrayList list = (ArrayList) next();
		pushParseLocation();
		startProcessing(list);
	}

	/**
	 * Starts iterating through the current object, which is assumed to be a map
	 */
	public void enterMap() {
		Map map = (Map) next();
		pushParseLocation();
		startProcessing(map);
	}

	/**
	 * Stop iterating through the current object, and resume the previous
	 * object; throws exception if the current object's iteration is not
	 * complete
	 */
	public void exit() {
		exit(true);
	}

	/**
	 * Stop iterating through the current object, and resume the previous object
	 */
	public void exit(boolean verifyNoItemsRemain) {
		if (verifyNoItemsRemain) {
			if (mIterator.hasNext())
				throw new IllegalStateException("incomplete iteration");
		}
		if (mParseStack.isEmpty())
			throw new IllegalStateException("cannot exit from top level");
		this.mIterator = (Iterator) pop(mParseStack);
		this.mCurrentContainer = pop(mParseStack);
		if (this.mCurrentContainer instanceof Map) {
			this.mCurrentMap = (Map) this.mCurrentContainer;
		} else {
			this.mCurrentMap = null;
		}
		this.mValueForLastKey = null;
	}

	private void pushParseLocation() {
		mParseStack.add(this.mCurrentContainer);
		mParseStack.add(this.mIterator);
	}

	private void readValueFromStream(InputStream s) {
		this.mStream = s;
		Object topLevelValue = readValue();
		verifyDone();
		// Construct a list that contains this single object
		ArrayList topLevelList = new ArrayList();
		topLevelList.add(topLevelValue);
		startProcessing(topLevelList);
	}

	private void startProcessing(ArrayList list) {
		this.mCurrentContainer = list;
		this.mCurrentMap = null;
		this.mIterator = list.iterator();
	}

	private void startProcessing(Map map) {
		this.mCurrentContainer = map;
		this.mCurrentMap = map;
		this.mIterator = map.keySet().iterator();
	}

	private Object readValue() {
		int c = peek(true);
		switch (c) {
		case '"':
			return readString();
		case '{':
			return readObject();
		case '[':
			return readArray();
		case 't':
		case 'f':
			return readBoolean();
		case 'n':
			readNull();
			return null;
		default:
			return readNumber();
		}
	}

	private String readString() {
		mStringBuilder.setLength(0);
		read('"', true);
		while (true) {
			int c = readCharacter(false);
			switch (c) {
			case '"':
				return mStringBuilder.toString();
			case '\\': {
				c = readCharacter(false);
				switch (c) {
				case '"':
				case '/':
				case '\\':
					mStringBuilder.append((char) c);
					break;
				case 'b':
					mStringBuilder.append('\b');
					break;
				case 'f':
					mStringBuilder.append('\f');
					break;
				case 'n':
					mStringBuilder.append('\n');
					break;
				case 'r':
					mStringBuilder.append('\r');
					break;
				case 't':
					mStringBuilder.append('\t');
					break;
				case 'u':
					mStringBuilder.append(parseHex());
					break;
				}
			}
				break;
			default:
				mStringBuilder.append((char) c);
				break;
			}
		}
	}

	private int parseDigit() {
		char c = (char) readCharacter(false);
		if (c >= 'A' && c <= 'F') {
			return c - 'A' + 10;
		}
		if (c >= 'A' && c <= 'F') {
			return c - 'A' + 10;
		}
		if (c >= 'a' && c <= 'f') {
			return c - 'a' + 10;
		}
		if (c >= '0' && c <= '9') {
			return c - '0';
		}
		throw new JSONException("unexpected input");
	}

	private char parseHex() {
		return (char) ((parseDigit() << 12) | (parseDigit() << 8)
				| (parseDigit() << 4) | parseDigit());
	}

	private double readNumber() {
		final boolean db = false;
		if (db)
			pr("\n\nreadNumber");
		mStringBuilder.setLength(0);
		int state = 0;
		boolean done = false;
		while (true) {
			int c = peek(false);
			boolean isDigit = (c >= '0' && c <= '9');
			if (db)
				pr(" state=" + state + " c=" + (char) c + " buffer:"
						+ mStringBuilder);
			int oldState = state;
			state = -1;
			switch (oldState) {
			case 0:
				if (c == '-')
					state = 1;
				else if (isDigit) {
					state = (c == '0') ? 2 : 3;
				}
				break;
			case 1:
				if (isDigit) {
					state = (c == '0') ? 2 : 3;
				}
				break;
			case 2:
				if (c == '.')
					state = 4;
				else
					done = true;
				break;
			case 3:
				if (c == '.')
					state = 4;
				else if (isDigit)
					state = 3;
				else if (c == 'e' || c == 'E')
					state = 6;
				else
					done = true;
				break;
			case 4:
				if (isDigit)
					state = 5;
				break;
			case 5:
				if (isDigit)
					state = 5;
				else if (c == 'e' || c == 'E')
					state = 6;
				else
					done = true;
				break;
			case 6:
				if (c == '+' || c == '-')
					state = 7;
				else if (isDigit)
					state = 8;
				break;
			case 7:
				if (isDigit)
					state = 8;
				break;
			case 8:
				if (isDigit)
					state = 8;
				else
					done = true;
				break;
			}
			if (done)
				break;

			if (state < 0) {
				if (db)
					pr("   ...throwing exception");
				throw new JSONException("unexpected input");
			}
			mStringBuilder.append((char) c);
			readCharacter(false);
		}
		double value;
		try {
			value = Double.parseDouble(mStringBuilder.toString());
		} catch (NumberFormatException e) {
			throw new JSONException(e);
		}
		if (db)
			pr(" returning number: " + value);
		return value;
	}

	public Map readObject() {
		Map m = new HashMap();
		read('{', true);
		while (peek(true) != '}') {
			String key = readString();
			read(':', true);
			Object value = readValue();
			m.put(key, value);
			if (peek(true) != ',') {
				break;
			}
			readCharacter(false);
		}
		read('}', true);
		return m;
	}

	private ArrayList readArray() {
		ArrayList a = new ArrayList();
		read('[', true);
		while (peek(true) != ']') {
			Object value = readValue();
			a.add(value);
			if (peek(true) != ',')
				break;
			readCharacter(false);
		}
		read(']', true);
		return a;
	}

	private void readExpString(String s) {
		for (int i = 0; i < s.length(); i++)
			read(s.charAt(i), false);
	}

	private void readNull() {
		readExpString("null");
	}

	private Boolean readBoolean() {
		if (peek(false) == 't') {
			readExpString("true");
			return Boolean.TRUE;
		} else {
			readExpString("false");
			return Boolean.FALSE;
		}
	}

	private int peek(boolean ignoreWhitespace) {
		// If we are ignoring whitespace,
		// and peek value matches white space, consume it
		if (ignoreWhitespace) {
			if (mPeekCharacter <= ' ')
				mPeekCharacter = -1;
			else if (mPeekCharacter == '/') {
				// Peek value matches start of a comment, so read one
				readComment();
			}
		}

		if (mPeekCharacter < 0) {
			try {
				while (true) {
					mPeekCharacter = mStream.read();
					if (mTrace) {
						String s = (mPeekCharacter < 0) ? "EOF" : Character
								.toString((char) mPeekCharacter);
						boolean newline = (s == "\n");
						if (newline)
							s = "\\n";
						if (mTraceBuffer.length() > 110) {
							mTraceBuffer.replace(0, 4, "...");
						}
						mTraceBuffer.append(s);
						System.out.println("JSON: " + mTraceBuffer + "\n"
								+ stackTrace(1, 1));
						if (newline)
							mTraceBuffer.setLength(0);
					}
					if (!ignoreWhitespace)
						break;
					if (mPeekCharacter < 0)
						break;
					if (ignoreWhitespace) {
						if (mPeekCharacter == '/') {
							readComment();
							continue;
						} else if (mPeekCharacter <= ' ')
							continue;
					}
					break;
				}
			} catch (IOException e) {
				throw new JSONException(e);
			}
		}
		return mPeekCharacter;
	}

	private void readComment() {
		int c = readCharacter(false);
		int c2 = readCharacter(false);
		if (c != '/' || c2 != '/')
			throw new JSONException("malformed comment: c=" + (char) c + " c2="
					+ (char) c2);
		while (true) {
			c = peek(false);
			if (c < 0)
				break;
			mPeekCharacter = -1;
			if (c == '\n')
				break;
		}
	}

	private int readCharacter(boolean skipWhitespace) {
		int p = peek(skipWhitespace);
		if (p < 0)
			throw new JSONException("end of input");
		mPeekCharacter = -1;
		return p;
	}

	private void read(int expectedChar, boolean ignoreWhitespace) {
		int c = readCharacter(ignoreWhitespace);
		if (c != expectedChar)
			throw new JSONException("unexpected input");
	}

	private void verifyDone() {
		if (peek(true) >= 0)
			throw new JSONException("extra input");
	}

	private StringBuilder mStringBuilder = new StringBuilder();
	private int mPeekCharacter = -1;
	private InputStream mStream;
	private boolean mTrace;
	private StringBuilder mTraceBuffer = new StringBuilder();

	private Object mCurrentContainer;
	private Map mCurrentMap; // null if current container is not a map
	private Object mValueForLastKey;
	private boolean mValueForKeyReady;
	// iterator into current object (map or list)
	private Iterator mIterator;
	private ArrayList mParseStack = new ArrayList();
	private boolean mPeekValueAvailable;
	private Object mPeekValue;
}
