package com.js.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.js.basic.Tools.*;

public class JSONEncoder {

	private StringBuilder sb = new StringBuilder();

	/**
	 * Utility method that constructs an encoder, uses it to encode an object,
	 * and returns the resulting JSON string
	 * 
	 * @param encodableObject
	 *            object that implements the IJSONEncoder interface
	 * @return JSON string
	 */
	public static String toJSON(IJSONEncoder encodableObject) {
		JSONEncoder encoder = new JSONEncoder();
		encoder.encode(encodableObject);
		return encoder.toString();
	}

	public String toString() {
		return sb.toString();
	}

	public void encode(IJSONEncoder jsonInstance) {
		if (jsonInstance == null) {
			encodeNull();
			return;
		}
		jsonInstance.encode(this);
	}

	public void encode(Object value) {
		if (value == null)
			encodeNull();
		else if (value instanceof IJSONEncoder)
			encode((IJSONEncoder)value);
		else if (value instanceof Number)
			encode(((Number) value).doubleValue());
		else if (value instanceof Boolean)
			encode(((Boolean) value).booleanValue());
		else if (value instanceof Map)
			encode((Map) value);
		else if (value instanceof List)
			encode((List) value);
		else if (value instanceof Set)
			encode((Set) value);
		else if (value instanceof Object[])
			encode((Object[]) value);
		else if (value instanceof int[])
			encode(cvtArray((int[]) value));
		else if (value instanceof double[])
			encode(cvtArray((double[]) value));
		else if (value instanceof String)
			encode((String) value);
		else
			throw new JSONException("unknown value type " + value + " : "
					+ value.getClass());
	}

	private static Object[] cvtArray(double[] value) {
		Object[] array = new Object[value.length];
		int i = 0;
		for (double x : value) {
			array[i++] = x;
		}
		return array;
	}

	private static Object[] cvtArray(int[] value) {
		Object[] array = new Object[value.length];
		int i = 0;
		for (int x : value) {
			array[i++] = x;
		}
		return array;
	}

	public void encode(Map map2) {
		if (map2 == null) {
			encodeNull();
			return;
		}
		enterMap();

		Map<String, Object> map = (Map<String, Object>) map2;

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			encode(entry.getKey());
			Object value = entry.getValue();
			encode(value);
		}
		exit();
	}

	public void encode(List list) {
		if (list == null) {
			encodeNull();
			return;
		}
		encodeAsList(list.iterator());
	}

	private void encodeAsList(Iterator iter) {

		enterList();
		while (iter.hasNext()) {
			encode(iter.next());
		}
		exit();
	}

	public void encode(Set set) {
		if (set == null) {
			encodeNull();
			return;
		}

		encodeAsList(set.iterator());
	}

	public void encode(Object[] array) {
		if (array == null) {
			encodeNull();
			return;
		}

		enterList();
		for (int i = 0; i < array.length; i++) {
			encode(array[i]);
		}
		exit();
	}

	public void encode(Number number) {
		encode(number.doubleValue());
	}

	public void encode(double d) {
		prepareForNextValue();
		long intValue = Math.round(d);
		if (d == intValue) {
			sb.append(intValue);
		} else {
			sb.append(d);
		}
	}

	public void encode(String s) {
		if (s == null) {
			encodeNull();
			return;
		}
		prepareForNextValue();
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\n':
				sb.append("\\n");
				break;
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c >= ' ' && c < 0x7f)
					sb.append(c);
				else {
					sb.append(String.format("\\u%04x", (int) c));
				}
				break;
			}
		}
		sb.append('"');
	}

	public void encode(boolean b) {
		prepareForNextValue();
		sb.append(b ? "true" : "false");
	}

	public void encodeNull() {
		prepareForNextValue();
		sb.append("null");
	}

	public void encodePair(String key, Object value) {
		if (collectionType != COLLECTION_MAP)
			throw new IllegalStateException();
	encode(key);
		encode(value);
	}
	
	public void clear() {
		sb.setLength(0);
	}

	public void enterList() {
		prepareForNextValue();
		pushState();
		collectionType = COLLECTION_LIST;
		collectionLength = 0;
		valueIsNext = false;
		sb.append('[');
	}

	private void popState() {
		valueIsNext = (Boolean) pop(stateStack);
		collectionLength = (Integer) pop(stateStack);
		collectionType = (Integer) pop(stateStack);

	}

	private void pushState() {
		stateStack.add(collectionType);
		stateStack.add(collectionLength);
		stateStack.add(valueIsNext);
	}

	public void enterMap() {
		prepareForNextValue();
		pushState();
		collectionType = COLLECTION_MAP;
		collectionLength = 0;
		valueIsNext = false;
		sb.append('{');
	}

	public void exit() {
		if (collectionType == COLLECTION_MAP) {
			sb.append('}');
			popState();
		} else if (collectionType == COLLECTION_LIST) {
			sb.append(']');
			popState();
		} else
			throw new IllegalStateException();
	}

	private void prepareForNextValue() {
		switch (collectionType) {
		case COLLECTION_NONE:
			if (collectionLength != 0)
				throw new IllegalStateException(
						"multiple items while not within list or map");
			collectionLength++;
			break;
		case COLLECTION_LIST:
			if (collectionLength != 0)
				sb.append(',');
			collectionLength++;
			break;
		case COLLECTION_MAP:
			if (valueIsNext) {
				sb.append(':');
				valueIsNext = false;
			} else {
				valueIsNext = true;
				if (collectionLength != 0)
					sb.append(',');
				collectionLength++;
			}
			break;
		}
	}

	private static final int COLLECTION_NONE = 0, COLLECTION_LIST = 1,
			COLLECTION_MAP = 2;

	private int collectionType = COLLECTION_NONE;
	private int collectionLength;
	private boolean valueIsNext;
	private ArrayList stateStack = new ArrayList();

}
