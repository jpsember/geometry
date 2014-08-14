package com.js.android;

import static com.js.basic.Tools.pr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import static com.js.basic.Tools.*;

public class BitmapUtil {

	public static final int JPEG_QUALITY_DEFAULT = 80;
	public static final String JPEG_EXTENSION = ".jpg";

	/**
	 * Construct a filename for a receipt's photo. This is derived from the
	 * receipt's id, and has no path component
	 * 
	 * @return filename
	 */
	public static String constructReceiptImageFilename(int receiptId) {
		return receiptId + BitmapUtil.JPEG_EXTENSION;
	}

	public static void writeJPEG(Bitmap bitmap, File destinationFile)
			throws IOException {
		writeJPEG(bitmap, destinationFile, JPEG_QUALITY_DEFAULT);
	}

	public static void writeJPEG(Bitmap bitmap, File destinationFile,
			int quality) throws IOException {
		FileOutputStream fOut = new FileOutputStream(destinationFile);
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fOut);
		fOut.flush();
		fOut.close();

		if (db)
			pr("writeJPEG bitmap " + dump(bitmap) + " to " + destinationFile
					+ ", length " + destinationFile.length());
	}

	/**
	 * Rotate a bitmap if its orientation (as recorded by the camera) indicates
	 * it needs rotation; optionally rotate bitmap as well. These are combined
	 * for efficiency, since it's quicker to rotate the smaller of the pre/post
	 * scaled images.
	 * 
	 * @param file
	 *            file containing image
	 * @param scaledDimension
	 *            size of largest scaled dimension to scale to, or -1 if no
	 *            scaling desired
	 * @param allowScalingUp
	 */
	public static void orientAndScaleBitmap(File file, int scaledDimension,
			boolean allowScalingUp) {
		if (db)
			pr("\n\norientAndScaleBitmap " + file);

		try {
			float rotationAngle = 0;

			ExifInterface exif = null;
			exif = new ExifInterface(file.getPath());

			if (exif != null) {
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION, 0);
				if (db)
					pr(" orientation=" + orientation);
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotationAngle = 270;
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotationAngle = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotationAngle = 180;
					break;
				}
			}
			// Rotates the image according to the orientation
			Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
			Bitmap originalBitmap = bitmap;
			if (scaledDimension < 0) {
				bitmap = rotateBitmap(bitmap, rotationAngle);
				if (db)
					pr(" rotated; " + dump(bitmap));
			} else {

				int[] sdim = getScaledDimensions(bitmap, scaledDimension,
						allowScalingUp);
				if (db)
					pr(" " + dump(bitmap) + " scaled dimensions " + sdim[0]
							+ " x " + sdim[1]);

				if (sdim[0] > bitmap.getWidth()) {
					// We're scaling up, so do the rotation before scaling
					bitmap = rotateBitmap(bitmap, rotationAngle);
					if (db)
						pr(" rotated; " + dump(bitmap));
					bitmap = scaleBitmap(bitmap, scaledDimension,
							allowScalingUp);
					if (db)
						pr(" scaled; " + dump(bitmap));
				} else {
					// We're scaling down (or not scaling at all), so do the
					// rotation afterward
					bitmap = scaleBitmap(bitmap, scaledDimension,
							allowScalingUp);
					if (db)
						pr(" scaled; " + dump(bitmap));
					bitmap = rotateBitmap(bitmap, rotationAngle);
					if (db)
						pr(" rotated; " + dump(bitmap));
				}
			}
			if (bitmap != originalBitmap) {
				writeJPEG(bitmap, file);
			}
		} catch (IOException e) {
			die(e);
		}
	}

	public static Bitmap rotateBitmap(Bitmap sourceBitmap, float angleInDegrees) {
		if (angleInDegrees == 0)
			return sourceBitmap;
		Matrix matrix = new Matrix();
		matrix.postRotate(angleInDegrees);
		return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(),
				sourceBitmap.getHeight(), matrix, true);
	}

	/**
	 * Construct a File for an xxxx.jpg image in the external storage directory
	 * 
	 * @param name
	 *            the 'xxxx'
	 * @return File
	 */
	public static File constructExternalImageFile(String name) {
		File storageDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File imageFile = new File(storageDir, name + JPEG_EXTENSION);
		imageFile = imageFile.getAbsoluteFile();
		return imageFile;
	}

	private static int[] getScaledDimensions(Bitmap bitmap,
			int desiredScaledDimension, boolean allowScalingUp) {
		double scaleFactor = Math.min(
				desiredScaledDimension / (double) bitmap.getWidth(),
				desiredScaledDimension / (double) bitmap.getHeight());
		if (db)
			pr(" original size " + bitmap.getWidth() + " x "
					+ bitmap.getHeight() + "  scale factor " + scaleFactor);

		if (!allowScalingUp)
			scaleFactor = Math.min(scaleFactor, 1.0);
		int actualScaledWidth = (int) Math.round(bitmap.getWidth()
				* scaleFactor);
		int actualScaledHeight = (int) Math.round(bitmap.getHeight()
				* scaleFactor);
		int[] out = { actualScaledWidth, actualScaledHeight };
		return out;
	}

	public static Bitmap scaleBitmap(Bitmap myBitmap,
			int desiredScaledDimension, boolean allowScalingUp) {
		int[] sdim = getScaledDimensions(myBitmap, desiredScaledDimension,
				allowScalingUp);

		boolean useFilter = sdim[0] > desiredScaledDimension;
		return Bitmap.createScaledBitmap(myBitmap, sdim[0], sdim[1], useFilter);
	}

	public static String dump(Bitmap b) {
		return "bitmap " + b.getWidth() + " x " + b.getHeight();
	}

	public static String dumpJPEG(byte[] jpeg) {
		StringBuilder sb = new StringBuilder("jpeg ");
		if (jpeg == null) {
			sb.append("<null>");
		} else {
			sb.append("length=" + jpeg.length);
		}
		return sb.toString();

	}

}
