package com.js.android;

import static com.js.android.Tools.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import static com.js.basic.Tools.*;

public abstract class MyActivity extends Activity {

  public void setLogging(boolean f) {
    doNothingAndroid();
    mLogging = f;
  }

  protected void log(Object message) {
    if (mLogging) {
      StringBuilder sb = new StringBuilder("===> ");
      sb.append(nameOf(this));
      sb.append(" : ");
      tab(sb, 30);
      sb.append(message);
      pr(sb);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (!testing()) {
      prepareSystemOut();
    }
    AppPreferences.prepare(this);
    addResourceMappings();
    prepareDisplayMetrics();
    log("onCreate savedInstanceState=" + nameOf(savedInstanceState));
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    log("onResume");
    super.onResume();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    log("onSaveInstanceState outState=" + nameOf(outState));
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onPause() {
    log("onPause");
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    log("onDestroy");
    super.onDestroy();
  }

  private void prepareSystemOut() {
    AndroidSystemOutFilter.install();
    if (sConsoleGreetingPrinted)
      return;
    sConsoleGreetingPrinted = true;

    // Print message about app starting. Print a bunch of newlines
    // to simulate clearing the console, and for convenience,
    // print the time of day so we can figure out if the
    // output is current or not.

    String strTime = "";
    {
      Calendar cal = Calendar.getInstance();
      java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
          "h:mm:ss", Locale.CANADA);
      strTime = sdf.format(cal.getTime());
    }
    for (int i = 0; i < 20; i++)
      pr("\n");
    pr("!!START!!--------------- Start of " + this.getClass().getSimpleName()
        + " ----- " + strTime + " -------------\n\n\n");
  }

  /**
   * Store the resource id associated with the resource's name, so we can refer
   * to them by name (for example, we want to be able to refer to them within
   * JSON strings).
   * 
   * There are some facilities to do this mapping using reflection, but
   * apparently it's really slow.
   * 
   * @param key
   * @param resourceId
   */
  public void addResource(String key, int resourceId) {
    mResourceMap.put(key, resourceId);
  }

  /**
   * Get the resource id associated with a resource name (added earlier).
   * 
   * @param key
   * @return resource id
   * @throws IllegalArgumentException
   *           if no mapping exists
   */
  public int getResource(String key) {
    Integer id = mResourceMap.get(key);
    if (id == null)
      throw new IllegalArgumentException("no resource id mapping found for "
          + key);
    return id.intValue();
  }

  private void prepareDisplayMetrics() {
    DisplayMetrics m = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(m);
    sResolutionInfo = new ResolutionInfo(m);
  }

  private void addResourceMappings() {
    addResource("photo", android.R.drawable.ic_menu_gallery);
    addResource("camera", android.R.drawable.ic_menu_camera);
    addResource("search", android.R.drawable.ic_menu_search);
  }

  public static ResolutionInfo getResolutionInfo() {
    return sResolutionInfo;
  }

  private boolean mLogging;
  private Map<String, Integer> mResourceMap = new HashMap();
  private static ResolutionInfo sResolutionInfo;
  private static boolean sConsoleGreetingPrinted;
}
