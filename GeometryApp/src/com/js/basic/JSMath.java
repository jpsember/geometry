package com.js.basic;

public final class JSMath {

	public static int clampInt(int value, int min, int max) {
		if (value < min)
			value = min;
		else if (value > max)
			value = max;
		return value;
	}

}
