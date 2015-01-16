package com.js.geometryapp.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Polygon;
import com.js.geometry.Rect;

public class EditorTools {

  public static void plotRect(AlgorithmStepper s, Rect r) {
    Polygon p = new Polygon();
    for (int i = 0; i < 4; i++)
      p.add(r.corner(i));
    s.render(p);
  }

  /**
   * Sanitize a user-specified filename. It should NOT include any path
   * information or an extension
   * 
   * @param name
   * @return sanitized name (which may be empty)
   */
  public static String sanitizeFilename(String name) {
    Pattern p = Pattern.compile("[a-zA-Z_0-9 ]*");
    name = name.trim();
    Matcher m = p.matcher(name);
    if (!m.matches() || name.length() > 64) {
      name = "";
    }
    return name;
  }

}
