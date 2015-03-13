package com.js.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.js.geometry.IPoint;
import com.js.basic.Rect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import static com.js.basic.Tools.*;

public class BitmapUtil {

	private static boolean pixelIsTransparent(int color) {
		if (db)
			pr("pixel color: " + dh(color));

		return Color.alpha(color) == 0;
	}

	public static Bitmap readFromResource(Context context, int resourceId) {
		InputStream is = context.getResources().openRawResource(resourceId);
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		return bitmap;
	}

	private static boolean rowUsed(Bitmap b, int rowNumber) {
		int w = b.getWidth();
		for (int x = 0; x < w; x++)
			if (!pixelIsTransparent(b.getPixel(x, rowNumber)))
				return true;
		return false;
	}

	private static boolean colUsed(Bitmap b, int yStart, int yEnd, int colNumber) {
		for (int y = yStart; y < yEnd; y++)
			if (!pixelIsTransparent(b.getPixel(colNumber, y)))
				return true;
		return false;
	}

	public static Bitmap trimPadding(Bitmap b) {
		int y0 = 0;
		while (y0 + 1 < b.getHeight() && !rowUsed(b, y0)) {
			y0++;
		}
		int y1 = b.getHeight();
		while (y1 - 1 > y0 && !rowUsed(b, y1 - 1)) {
			y1--;
		}

		int x0 = 0;
		while (x0 + 1 < b.getWidth() && !colUsed(b, y0, y1, x0)) {
			x0++;
		}
		int x1 = b.getWidth();
		while (x1 - 1 > x0 && !colUsed(b, y0, y1, x1 - 1)) {
			x1--;
		}

		Bitmap b2 = Bitmap.createBitmap(b, x0, y0, x1 - x0, y1 - y0);
		if (db)
			pr("Trimmed bitmap from: "
					+ new Rect(0, 0, b.getWidth(), b.getHeight()) + "\n "
					+ d(b) + "\nto: " + new Rect(x0, y0, x1 - x0, y1 - y0)
					+ "\n " + d(b2));
		return b2;
	}

	public static String d(Bitmap b) {
		StringBuilder sb = new StringBuilder("Bitmap ");
		sb.append("size:" + new IPoint(b.getWidth(), b.getHeight()));
		return sb.toString();
	}

	private static void prepareDynamicSnapshotsDir() {
		if (sPNGSaveDirectory == null) {
			File directory = Environment.getExternalStorageDirectory();
			if (directory == null)
				die("external files dir is null");
			sPNGSaveDirectory = new File(directory, "png");
			sPNGSaveDirectory.mkdirs();
			if (!sPNGSaveDirectory.exists())
				die("unable to create " + sPNGSaveDirectory);
		}
	}

	public static void saveBitmapAsPNG(Bitmap bitmap, String pngName) {
		String pngNameWithExtension = pngName + ".png";
		try {
			prepareDynamicSnapshotsDir();
			File f = new File(sPNGSaveDirectory, pngNameWithExtension);
			OutputStream stream = new FileOutputStream(f);
			bitmap.compress(CompressFormat.PNG, 80, stream);
			stream.close();
			pr("Saved bitmap; use 'adb pull /sdcard/png/"
					+ pngNameWithExtension + " ~/Desktop/"
					+ pngNameWithExtension + "'");
		} catch (IOException e) {
			die(e);
		}
	}

	private static File sPNGSaveDirectory;

}
