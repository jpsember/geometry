package com.js.geometryapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.js.android.MyActivity;
import com.js.json.JSONEncoder;
import com.js.json.JSONParser;

import static com.js.basic.Tools.*;

/**
 * Encapsulates the user-defined options. These appear in a SlidingPaneLayout,
 * to preserve screen real estate on small devices
 */
public class AlgorithmOptions {

	/**
	 * Construct an options object
	 * 
	 * @param context
	 * @param mainView
	 *            the main view (i.e. the original 'content' view), it will
	 *            actually be placed within the options SlidingPaneLayout, which
	 *            becomes the new content view
	 */
	public static AlgorithmOptions construct(Context context, View mainView) {
		AlgorithmOptions v = new AlgorithmOptions(context);
		v.buildSlidingPane(context);
		v.buildOptionsView();
		v.addChildViews(mainView);
		sAlgorithmOptions = v;
		return v;
	}

	/**
	 * Get the singleton instance of the options object
	 */
	public static AlgorithmOptions sharedInstance() {
		return sAlgorithmOptions;
	}

	/**
	 * Get the view that contains both the main view and the options
	 */
	public ViewGroup getView() {
		return mSlidingPane;
	}

	/**
	 * Private constructor
	 */
	private AlgorithmOptions(Context context) {
		sContext = context;
		for (int i = 0; i < basicWidgets.length; i++)
			registerWidget(basicWidgets[i]);
	}

	private void buildSlidingPane(Context context) {
		mSlidingPane = new SlidingPaneLayout(context);
	}

	/**
	 * Add the main content view and options views to the sliding pane
	 */
	private void addChildViews(View mainView) {

		// Determine if device in current orientation is large enough to display
		// both panes at once

		final float PREFERRED_MAIN_WIDTH_INCHES = 3;
		final float PREFERRED_OPTIONS_WIDTH_INCHES = 2;
		final float TOTAL_WIDTH_INCHES = (PREFERRED_MAIN_WIDTH_INCHES + PREFERRED_OPTIONS_WIDTH_INCHES);

		DisplayMetrics displayMetrics = MyActivity.displayMetrics();
		float mainWidth;
		float optionsWidth;

		boolean landscapeMode = displayMetrics.widthPixels > displayMetrics.heightPixels;
		float deviceWidthInInches = displayMetrics.widthPixels
				/ displayMetrics.xdpi;
		boolean bothFit = landscapeMode
				&& deviceWidthInInches >= PREFERRED_MAIN_WIDTH_INCHES
						+ PREFERRED_OPTIONS_WIDTH_INCHES;

		if (bothFit) {
			mainWidth = displayMetrics.widthPixels
					* (PREFERRED_MAIN_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
			optionsWidth = displayMetrics.widthPixels
					* (PREFERRED_OPTIONS_WIDTH_INCHES / TOTAL_WIDTH_INCHES);
		} else {
			mainWidth = LayoutParams.MATCH_PARENT;
			optionsWidth = LayoutParams.MATCH_PARENT;
		}

		SlidingPaneLayout.LayoutParams lp = new SlidingPaneLayout.LayoutParams(
				(int) mainWidth, LayoutParams.MATCH_PARENT);
		lp.weight = 1;
		mSlidingPane.addView(mainView, lp);

		lp = new SlidingPaneLayout.LayoutParams((int) optionsWidth,
				LayoutParams.MATCH_PARENT);
		mSlidingPane.addView(mOptionsView, lp);

		if (!bothFit)
			mSlidingPane.openPane();
	}

	private Context getContext() {
		return mSlidingPane.getContext();
	}

	/**
	 * Build a view to contain the various user controls (buttons, checkboxes,
	 * etc) that will appear in the sliding pane
	 */
	private void buildOptionsView() {
		LinearLayout options = new LinearLayout(getContext());
		options.setOrientation(LinearLayout.VERTICAL);
		mOptionsView = options;
	}

	private static Map<String, Object> buildAttributes(String identifier) {
		Map<String, Object> attributes = new HashMap();
		attributes.put("id", identifier);
		return attributes;
	}

	/**
	 * Add widgets defined by a JSON script
	 */
	public void addWidgets(String jsonString) {
		parseWidgets(new JSONParser(jsonString));
	}

	/**
	 * Add a checkbox widget
	 */
	public CheckBoxWidget addCheckBox(String id) {
		return addCheckBox(id, false);
	}

	/**
	 * Add a checkbox widget
	 */
	public CheckBoxWidget addCheckBox(String id, boolean selected) {
		Map<String, Object> attributes = buildAttributes(id);
		CheckBoxWidget w = new CheckBoxWidget(sContext, attributes);
		addWidget(w);
		w.setValue(selected);
		return w;
	}

	/**
	 * Add a combobox widget
	 */
	public ComboBoxWidget addComboBox(String id, String[] options) {
		Map<String, Object> attributes = buildAttributes(id);
		ArrayList<String> s = new ArrayList();
		for (int i = 0; i < options.length; i++)
			s.add(options[i]);
		attributes.put(ComboBoxWidget.ATTR_OPTIONS, s);
		ComboBoxWidget w = new ComboBoxWidget(sContext, attributes);
		addWidget(w);
		return w;
	}

	private void addWidget(AbstractWidget w) {
		// Add it to the map
		AbstractWidget previousMapping = mWidgetsMap.put(w.getId(), w);
		if (previousMapping != null)
			die("widget id " + w.getId() + " already exists");

		// Add it to the sequence as well
		mWidgetsList.add(w);

		// Add it to the options view
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mOptionsView.addView(w.getView(), p);
	}

	private static AbstractWidget.Factory[] basicWidgets = {
			CheckBoxWidget.FACTORY, ComboBoxWidget.FACTORY,
			SliderWidget.FACTORY };

	public void registerWidget(AbstractWidget.Factory factory) {
		mWidgetFactoryMap.put(factory.getName(), factory);
	}

	private void parseWidgets(JSONParser json) {
		json.enterList();
		while (json.hasNext()) {
			Map attributes = (Map) json.next();
			AbstractWidget item = build(attributes);
			String id = item.getId();
			if (!id.isEmpty()) {
				addWidget(item);
			}
		}
		json.exit();
	}

	/**
	 * Get value of widget (as a string)
	 */
	public String getValue(String widgetId) {
		return getWidget(widgetId).getValue();
	}

	/**
	 * Get value of widget as an integer
	 */
	public int getIntValue(String widgetId) {
		return getWidget(widgetId).getIntValue();
	}

	public void setValue(String widgetId, int intValue) {
		setValue(widgetId, Integer.valueOf(intValue), true);
	}

	/**
	 * Get value of widget as a boolean
	 */
	public boolean getBooleanValue(String widgetId) {
		return getWidget(widgetId).getBooleanValue();
	}

	/**
	 * Write value to widget
	 */
	public void setValue(String fieldName, Object value, boolean notifyListeners) {
		getWidget(fieldName).setValue(value.toString(), notifyListeners);
	}

	/**
	 * Find widget by name
	 */
	public AbstractWidget getWidget(String widgetName) {
		AbstractWidget field = mWidgetsMap.get(widgetName);
		if (field == null)
			throw new IllegalArgumentException("no widget found with name "
					+ widgetName);
		return field;
	}

	/**
	 * Construct a widget from a set of attributes, by using an appropriate
	 * factory constructor
	 */
	AbstractWidget build(Map attributes) {
		AbstractWidget widget;
		String type = (String) attributes.get("type");
		if (type == null)
			throw new IllegalArgumentException("no type found");
		AbstractWidget.Factory factory = mWidgetFactoryMap.get(type);
		if (factory == null)
			throw new IllegalArgumentException(
					"no factory found for widget type " + type);
		widget = factory.constructInstance(sContext, attributes);
		return widget;
	}

	void setPrepared(boolean f) {
		mPrepared = f;
	}

	boolean isPrepared() {
		return mPrepared;
	}

	/**
	 * Compile widget values to JSON string
	 */
	String saveValues() {
		Map<String, String> values = new HashMap();
		for (AbstractWidget w : mWidgetsList) {
			values.put(w.getId(), w.getValue());
		}
		return JSONEncoder.toJSON(values);
	}

	/**
	 * Restore widget values from JSON string
	 */
	void restoreValues(String jsonString) {
		JSONParser parser = new JSONParser(jsonString);
		Map<String, String> values = (Map) parser.next();
		for (String key : values.keySet()) {
			String value = values.get(key);
			AbstractWidget w = mWidgetsMap.get(key);
			if (w == null)
				continue;
			// TODO: catch exceptions that may get thrown here
			w.setValue(value);
		}
	}

	private static AlgorithmOptions sAlgorithmOptions;

	private boolean mPrepared;
	private SlidingPaneLayout mSlidingPane;
	private ViewGroup mOptionsView;
	private Context sContext;
	private Map<String, AbstractWidget.Factory> mWidgetFactoryMap = new HashMap();
	private List<AbstractWidget> mWidgetsList = new ArrayList();
	private Map<String, AbstractWidget> mWidgetsMap = new HashMap();
}
