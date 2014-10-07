package com.js.geometryapp.editor;

import java.util.Map;

import com.js.geometry.Point;
import static com.js.basic.Tools.*;

public abstract class EdObjectFactory {

	/**
	 * Utility function for writing object to file: Add FPoint2 to
	 * StringBuilder, with spaces as necessary
	 * 
	 * @param sb
	 *            StringBuilder to add to
	 * @param pt
	 *            point
	 */
	public static void toString(StringBuilder sb, Point pt) {
		sb.append(" ");
		toString(sb, pt.x);
		toString(sb, pt.y);
	}

	/**
	 * Utility function for writing object to file: Add double value to
	 * StringBuilder, with spaces as necessary
	 * 
	 * @param sb
	 *            StringBuilder to add to
	 * @param v
	 *            value
	 */
	public static void toString(StringBuilder sb, float v) {
		sb.append(d(v));
	}

	/**
	 * Utility function for writing object to file: Add integer value to
	 * StringBuilder, with spaces as necessary
	 * 
	 * @param sb
	 *            StringBuilder to add to
	 * @param v
	 *            value
	 */
	public static void toString(StringBuilder sb, int v) {
		sb.append(d(v));
	}

	/**
	 * Utility function for writing object to file: Add boolean flag to
	 * StringBuilder, with spaces as necessary
	 * 
	 * @param sb
	 *            StringBuilder to add to
	 * @param f
	 *            boolean flag
	 */
	public static void toString(StringBuilder sb, boolean f) {
		sb.append(' ');
		sb.append(d(f));
	}

	/**
	 * Get name of this object. This is an identifier that is written to text
	 * files to identify this object.
	 * 
	 * @return String
	 */
	public abstract String getTag();

	/**
	 * Get editor menu text for adding items of this type
	 * 
	 * @return text to put in menu label, or null if user can't add these types.
	 */
	public abstract String getMenuLabel();

	/**
	 * Construct an EditObj of this type. Used when user wants to add a new
	 * object in the editor.
	 * 
	 * @return EditObj
	 */
	public abstract EdObject construct();

	/**
	 * Parse EditObj from a map
	 * 
	 * @return EditObj
	 */
	public abstract EdObject parse(Map map, int flags);

	/**
	 * Construct Map from EdObject
	 */
	public abstract Map write(EdObject obj);
}
