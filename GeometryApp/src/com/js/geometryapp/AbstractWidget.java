package com.js.geometryapp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.js.android.MyActivity;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import static com.js.basic.Tools.*;
import static com.js.android.Tools.*;

public abstract class AbstractWidget {

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(nameOf(this));
		sb.append(" value:" + mWidgetValue);
		return sb.toString();
	}

	protected String getId() {
		return strAttr("id", "");
	}

	protected double dblAttr(String key, Number defaultValue) {
		return ((Number) attr(key, defaultValue)).doubleValue();
	}

	protected int intAttr(String key, Number defaultValue) {
		return ((Number) attr(key, defaultValue)).intValue();
	}

	protected String strAttr(String key, String defaultValue) {
		return (String) attr(key, defaultValue);
	}

	protected boolean boolAttr(String key, boolean defaultValue) {
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

	protected AbstractWidget(Context context, Map attributes) {
		this.mAttributes = attributes;
		this.mWidgetValue = "";

		LinearLayout widgetContainer = new LinearLayout(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		widgetContainer.setLayoutParams(params);
		int hPadding = MyActivity.inchesToPixels(.12f);
		int vPadding = MyActivity.inchesToPixels(.05f);
		widgetContainer.setPadding(hPadding, vPadding, hPadding, vPadding);

		this.mPrimaryView = widgetContainer;
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

	protected Context context() {
		return mPrimaryView.getContext();
	}

	/**
	 * Set value as string; calls updateUserValue(...) which subclasses should
	 * override to convert 'internal' string representation to user-displayable
	 * value. Also notifies listeners if value has changed
	 * 
	 * @param internalValue
	 *            the new internal string representation of the value
	 */
	public final void setValue(String internalValue) {
		setValue(internalValue, true);
	}

	/**
	 * Same as {@link #setValue(String)}, but with option to notify listeners
	 * 
	 * @param notifyListeners
	 *            if value has changed, listeners are notified only if this is
	 *            true
	 */
	final void setValue(String internalValue, boolean notifyListeners) {
		if (internalValue == null)
			throw new IllegalArgumentException("value must not be null");

		// Update value, and notify listeners if it has actually changed
		updateUserValue(internalValue);
		String newUserValue = parseUserValue();
		if (notifyListeners) {
			if (newUserValue.equals(mWidgetValue)) {
				notifyListeners = false;
			}
		}
		mWidgetValue = newUserValue;

		if (notifyListeners) {
			for (Listener listener : mListeners) {
				listener.valueChanged(this);
			}
		}
	}

	/**
	 * Set displayed value; subclasses should perform whatever translation /
	 * parsing is appropriate to convert the internal value to something
	 * displayed in the widget.
	 * 
	 * @param internalValue
	 */
	public void updateUserValue(String internalValue) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get displayed value, and transform to 'internal' representation.
	 */
	public String parseUserValue() {
		throw new UnsupportedOperationException();
	}

	public String getLabel(boolean addColon) {
		String labelValue = strAttr("label", getId());
		labelValue = applyStringSubstitution(context(), labelValue);
		if (addColon && !labelValue.endsWith(":"))
			labelValue += ":";
		return labelValue;
	}

	/**
	 * Construct a label for this widget
	 */
	protected void constructLabel(boolean addColon) {
		String labelText = getLabel(addColon);
		if (!labelText.isEmpty()) {
			TextView label = new TextView(context());
			label.setText(labelText);
			label.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			mPrimaryView.addView(label);
		}
	}

	public ViewGroup getView() {
		return mPrimaryView;
	}

	public void setOnClickListener(OnClickListener listener) {
		throw new UnsupportedOperationException();
	}

	public View buildLabelView(boolean addColon) {

		// Place the label within a separate container, following an expanding
		// view to achieve right-justified text

		String labelText = getLabel(addColon);
		TextView label = new TextView(context());
		label.setText(labelText);

		LinearLayout container = new LinearLayout(context());

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				MyActivity.inchesToPixels(.8f), LayoutParams.WRAP_CONTENT);
		p.gravity = Gravity.BOTTOM;
		container.setLayoutParams(p);

		p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		p.weight = 1;
		container.addView(new FrameLayout(context()), p);

		p = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		p.gravity = Gravity.RIGHT;
		container.addView(label, p);

		return container;
	}

	/**
	 * Factory that each widget type must supply
	 */
	public static interface Factory {
		String getName();

		AbstractWidget constructInstance(Context context, Map attributes);
	}

	public static interface Listener {
		public void valueChanged(AbstractWidget widget);
	}

	public void addListener(Listener listener) {
		mListeners.add(listener);
	}

	public void removeListener(Listener listener) {
		mListeners.remove(listener);
	}

	// View representing this widget. It probably contains subviews that include
	// one or more Android gadgets (e.g. CheckBox, TextView)
	private ViewGroup mPrimaryView;

	// Attributes defining widget type, name, etc
	private Map mAttributes;

	// This is the value this widget represents, if any. It is an 'internal'
	// string representation of the value
	private String mWidgetValue;

	private Set<Listener> mListeners = new HashSet();
}