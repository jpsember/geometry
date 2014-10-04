package com.js.android;

import android.graphics.Color;
import android.view.View;
import static com.js.basic.Tools.*;

import com.js.geometry.MyMath;

public final class UITools {

	public static final boolean SET_DEBUG_COLORS = (false && DEBUG_ONLY_FEATURES);

	private static int debugColors[] = {
			//
			// check out http://www.colorpicker.com/
			//
			0x10, 0x10, 0xe0, // dark blue
			0x37, 0x87, 0x3E, // dark green
			0x73, 0x5E, 0x22, // brown
			0xC7, 0x32, 0x00, // dark red
			0x8C, 0x26, 0xBF, // purple
			0x82, 0xB6, 0xBA, // blue/gray
			0xA3, 0x62, 0x84, // plum
			0xC7, 0x92, 0x00, // burnt orange
	};

	public static int debugColor() {
		return debugColor(sDebugColorIndex++);
	}

	public static int debugColor(int index) {
		index = MyMath.myMod(index, debugColors.length / 3) * 3;
		return Color.argb(255, debugColors[index], debugColors[index + 1],
				debugColors[index + 2]);
	}

	public static void applyDebugColors(View view) {
		if (SET_DEBUG_COLORS)
			view.setBackgroundColor(debugColor());
	}

	private static int sDebugColorIndex;

}
