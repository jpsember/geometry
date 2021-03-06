package com.js.geometryapp.widget;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.js.android.MyActivity;
import com.js.android.UITools;
import com.js.geometryapp.AlgorithmOptions;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;
import static com.js.android.UITools.*;

public abstract class AbstractWidget {

  /**
   * Set this to a widget id to print changes to its value
   */
  public static final String TRACE_WIDGET_VALUE = null;

  /**
   * Value of widget; widget-specific type
   */
  public static final String OPTION_VALUE = "value";

  /**
   * Flag for whether widget's value (if it exists) should be persisted
   */
  public static final String OPTION_PERSIST_VALUE = "hasvalue";

  /**
   * String-valued label; if missing, uses id as label
   */
  public static final String OPTION_LABEL = "label";

  /**
   * If true, no label will be added to the widget
   */
  public static final String OPTION_HAS_LABEL = "has_label";

  /**
   * If true (the default), any change to widget value triggers a refresh of the
   * algorithm view
   */
  public static final String OPTION_REFRESH_ALGORITHM = "refresh_algorithm";

  /**
   * If true, any change to widget value implies the total steps may change
   */
  public static final String OPTION_RECALC_ALGORITHM_STEPS = "recalc_algorithm";

  /**
   * If true, the widget's view will not be added to its group's view
   */
  public static final String OPTION_HIDDEN = "hidden";

  /**
   * If this option is true, the widget will not be added to a group. Its view
   * can be added to some other view if desired, and it can still be used to
   * store state.
   * 
   * This should be considered an internal flag; users probably want
   * OPTION_HIDDEN.
   */
  public static final String OPTION_DETACHED = "detached";

  /**
   * Integer-valued LayoutParams width argument
   */
  public static final String OPTION_LAYOUT_WIDTH = "layout_width";

  /**
   * Integer-valued LayoutParams height argument
   */
  public static final String OPTION_LAYOUT_HEIGHT = "layout_height";

  public static interface Validator {
    /**
     * Validate a value for correctness
     * 
     * @param widget
     *          widget containing the value
     * @param value
     * @return validated value; will be written back to widget
     */
    public String validate(AbstractWidget widget, String value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(nameOf(this));
    sb.append(" id:" + getId());
    sb.append(" value:" + mWidgetValue);
    return sb.toString();
  }

  public String getId() {
    return strAttr("id", "");
  }

  public double dblAttr(String key, Number defaultValue) {
    return ((Number) attr(key, defaultValue)).doubleValue();
  }

  public int intAttr(String key, Number defaultValue) {
    return ((Number) attr(key, defaultValue)).intValue();
  }

  public String strAttr(String key, String defaultValue) {
    return (String) attr(key, defaultValue);
  }

  public boolean boolAttr(String key, boolean defaultValue) {
    return (Boolean) attr(key, defaultValue);
  }

  private Object attr(String key, Object defaultValue) {
    Object value = mAttributes.get(key);
    if (value == null)
      value = defaultValue;
    if (value == null)
      throw new IllegalArgumentException("missing argument: " + key);
    return value;
  }

  protected AbstractWidget(AlgorithmOptions options, Map attributes) {
    mOptions = options;
    mAttributes = attributes;
    mWidgetValue = "";
    this.mPrimaryView = UITools.linearLayout(options.getContext(), false);
  }

  /**
   * Get value of widget as string
   */
  public final String getValue() {
    return parseUserValue();
  }

  public int getIntValue() {
    return Integer.parseInt(getValue());
  }

  public boolean getBooleanValue() {
    return Boolean.parseBoolean(getValue());
  }

  protected Context context() {
    return mPrimaryView.getContext();
  }

  public final void setIntValue(int internalValue) {
    setValue(Integer.toString(internalValue));
  }

  /**
   * Set value as string; calls updateUserValue(...) which subclasses should
   * override to convert 'internal' string representation to user-displayable
   * value. Also notifies listeners if value has changed
   * 
   * @param internalValue
   *          the new internal string representation of the value
   */
  public final void setValue(String internalValue) {
    if (internalValue == null)
      throw new IllegalArgumentException("value must not be null");

    // Update value, and notify listeners if it has actually changed
    updateUserValue(internalValue);
    String newUserValue = parseUserValue();
    String oldValue = mWidgetValue;
    boolean valueHasChanged = !newUserValue.equals(oldValue);
    if (!valueHasChanged)
      return;

    if (TRACE_WIDGET_VALUE != null) {
      if (TRACE_WIDGET_VALUE.equals(getId())) {
        pr("Widget '" + getId() + "' value changing from '" + oldValue
            + "' to '" + newUserValue + "'");
      }
    }

    mWidgetValue = newUserValue;
    notifyListeners();
  }

  /**
   * Set displayed value; subclasses should perform whatever translation /
   * parsing is appropriate to convert the internal value to something displayed
   * in the widget.
   * 
   * @param internalValue
   */
  public void updateUserValue(String internalValue) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get displayed value, and transform to 'internal' representation.
   * 
   * TODO: The parseUserValue() / updateUserValue() method names are confusing.
   */
  public String parseUserValue() {
    throw new UnsupportedOperationException();
  }

  public String getLabel(boolean addColon) {
    String labelValue = strAttr(OPTION_LABEL, getId());
    labelValue = applyStringSubstitution(context(), labelValue);
    if (addColon && !labelValue.endsWith(":"))
      labelValue += ":";
    return labelValue;
  }

  public AbstractWidget setAttribute(String name, Object value) {
    mAttributes.put(name, value);
    return this;
  }

  public void setEnabled(boolean state) {
  }

  public boolean isEnabled() {
    return true;
  }

  /**
   * Construct a label for this widget
   */
  protected void constructLabel(boolean addColon) {
    String labelText = getLabel(addColon);
    if (!labelText.isEmpty()) {
      TextView label = new TextView(context());
      label.setText(labelText);
      mPrimaryView.addView(label, new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT));
    }
  }

  public LinearLayout getView() {
    return mPrimaryView;
  }

  public void setOnClickListener(OnClickListener listener) {
    throw new UnsupportedOperationException();
  }

  public View buildLabelView(String labelText) {
    // Place the label within a separate container, following an expanding
    // view to achieve right-justified text
    LinearLayout container = linearLayout(context(), false);
    // Store the label container's parameters within it, so client doesn't have
    // to construct one when adding
    LinearLayout.LayoutParams p = UITools.layoutParams(container, 0);
    p.width = MyActivity.getResolutionInfo().inchesToPixelsUI(.8f);
    container.setLayoutParams(p);

    {
      p = layoutParams(container, 1.0f);
      // Give fill a fixed but small height, otherwise it will explode in size
      p.height = 1;
      View filler = new View(context());
      container.addView(filler, p);
    }
    {
      TextView label = new TextView(context());
      label.setText(labelText);
      container.addView(label, layoutParams(container, 0));
    }
    return container;
  }

  public static interface Listener {
    public void valueChanged(AbstractWidget widget);
  }

  public AbstractWidget addListener(Listener listener) {
    mListeners.add(listener);
    return this;
  }

  public AbstractWidget removeListener(Listener listener) {
    mListeners.remove(listener);
    return this;
  }

  public AbstractWidget setLabel(String label) {
    mAttributes.put(OPTION_LABEL, label);
    return this;
  }

  Map attributes() {
    return mAttributes;
  }

  protected void notifyListeners() {
    mOptions.processWidgetValue(this, this.mListeners);
  }

  /**
   * Have the gesture set, if one exists, display the gesture corresponding to
   * this widget (if there is one) within its panel
   */
  protected void displayGestureForWidget() {
    mOptions.displayGestureWithName(getId());
  }

  // View representing this widget. It probably contains subviews that include
  // one or more Android gadgets (e.g. CheckBox, TextView)
  private LinearLayout mPrimaryView;

  // Attributes defining widget type, name, etc
  private Map mAttributes;

  // This is the value this widget represents, if any. It is an 'internal'
  // string representation of the value
  private String mWidgetValue;

  private AlgorithmOptions mOptions;

  protected Set<Listener> mListeners = new HashSet();
}
