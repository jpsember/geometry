package com.js.geometryapp;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.js.android.AppPreferences;
import com.js.android.MyActivity;
import com.js.json.JSONEncoder;
import com.js.json.JSONParser;

import static com.js.basic.Tools.*;

/**
 * Encapsulates the user-defined options. These appear in a SlidingPaneLayout,
 * to preserve screen real estate on small devices
 */
public class AlgorithmOptions {

	private static final String PERSIST_KEY_WIDGET_VALUES = "_widget_values";

	/**
	 * Get the singleton instance of the options object
	 */
	public static AlgorithmOptions sharedInstance() {
		return sAlgorithmOptions;
	}

	/**
	 * Hide the options pane to reveal the main view (has no effect if both are
	 * always visible)
	 */
	public void hide() {
		mSlidingPane.openPane();
	}

	/**
	 * Add widgets defined by a JSON script
	 */
	public void addWidgets(String jsonString) {
		parseWidgets(new JSONParser(jsonString));
	}

	/**
	 * Build and add a slider widget
	 */
	public SliderWidget addSlider(String id, int min, int max) {
		SliderWidget w = buildSlider(id, min, max);
		addWidget(w);
		return w;
	}

	/**
	 * Build a slider widget (but don't add it)
	 */
	public SliderWidget buildSlider(String id, int min, int max) {
		Map<String, Object> attributes = buildAttributes(id);
		attributes.put("min", min);
		attributes.put("max", max);
		SliderWidget w = new SliderWidget(sContext, attributes);
		return w;
	}

	/**
	 * Add a checkbox widget
	 */
	public CheckBoxWidget addCheckBox(String id, boolean initialValue) {
		Map<String, Object> attributes = buildAttributes(id);
		CheckBoxWidget w = new CheckBoxWidget(sContext, attributes);
		addWidget(w);
		w.setValue(initialValue);
		return w;
	}

	/**
	 * Add a combobox widget
	 */
	public ComboBoxWidget addComboBox(String id) {
		Map<String, Object> attributes = buildAttributes(id);
		ComboBoxWidget w = new ComboBoxWidget(sContext, attributes);
		addWidget(w);
		return w;
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
		getWidget(widgetId).setValue(Integer.toString(intValue));
	}

	/**
	 * Get value of widget as a boolean
	 */
	public boolean getBooleanValue(String widgetId) {
		return getWidget(widgetId).getBooleanValue();
	}

	/**
	 * Find widget by name
	 */
	public <T extends AbstractWidget> T getWidget(String widgetName) {
		T field = (T) mWidgetsMap.get(widgetName);
		if (field == null)
			throw new IllegalArgumentException("no widget found with name "
					+ widgetName);
		return field;
	}

	/**
	 * Construct an options object
	 * 
	 * @param context
	 * @param mainView
	 *            the main view (i.e. the original 'content' view), it will
	 *            actually be placed within the options SlidingPaneLayout, which
	 *            becomes the new content view
	 */
	static AlgorithmOptions construct(Context context, View mainView) {
		AlgorithmOptions v = new AlgorithmOptions(context);
		v.buildSlidingPane(context);
		v.buildOptionsView();
		v.addChildViews(mainView);
		sAlgorithmOptions = v;
		return v;
	}

	/**
	 * Get the view that contains both the main view and the options
	 */
	ViewGroup getView() {
		return mSlidingPane;
	}

	/**
	 * Private constructor
	 */
	private AlgorithmOptions(Context context) {
		sContext = context;
		for (int i = 0; i < basicWidgets.length; i++) {
			AbstractWidget.Factory factory = basicWidgets[i];
			mWidgetFactoryMap.put(factory.getName(), factory);
		}
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

		// Add some padding around the actual options
		ViewGroup optionsContainer = mOptionsView;
		{
			FrameLayout frame = new FrameLayout(getContext());
			if (AbstractWidget.SET_DEBUG_COLORS) {
				frame.setBackgroundColor(OurGLTools.debugColor());
			}
			int padding = MyActivity.inchesToPixels(.05f);
			frame.setPadding(padding, padding, padding, padding);
			frame.addView(mOptionsView);
			optionsContainer = frame;
		}
		mSlidingPane.addView(optionsContainer, lp);

		if (!bothFit)
			hide();
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

	void addWidget(AbstractWidget w) {
		// Add it to the map
		AbstractWidget previousMapping = mWidgetsMap.put(w.getId(), w);
		if (previousMapping != null)
			die("widget id " + w.getId() + " already exists");

		// Add it to the options view
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mOptionsView.addView(w.getView(), p);
	}

	private static AbstractWidget.Factory[] basicWidgets = {
			CheckBoxWidget.FACTORY, ComboBoxWidget.FACTORY,
			SliderWidget.FACTORY };

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
	 * Construct a widget from a set of attributes, by using an appropriate
	 * factory constructor
	 */
	private AbstractWidget build(Map attributes) {
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

	boolean isPrepared() {
		return mPrepared;
	}

	/**
	 * Compile widget values to JSON string
	 */
	private String saveValues() {
		Map<String, String> values = new HashMap();
		for (String widgetId : mWidgetsMap.keySet()) {
			AbstractWidget w = mWidgetsMap.get(widgetId);
			values.put(widgetId, w.getValue());
		}
		return JSONEncoder.toJSON(values);
	}

	/**
	 * Determine if tracing for a particular detail is enabled
	 * 
	 * @param detailName
	 *            name of detail
	 * @return value of checkbox with id = detailName
	 */
	boolean detailTraceActive(String detailName) {
		AbstractWidget widget = mWidgetsMap.get(detailName);
		if (widget == null) {
			warning("no detail widget found: " + detailName);
			return true;
		}
		return widget.getBooleanValue();
	}

	void restoreStepperState() {
		String mCurrentWidgetValuesScript = AppPreferences.getString(
				PERSIST_KEY_WIDGET_VALUES, null);
		if (mCurrentWidgetValuesScript != null) {
			JSONParser parser = new JSONParser(mCurrentWidgetValuesScript);
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
		mPrepared = true;
	}

	private void persistStepperStateAux() {
		if (!mFlushRequired)
			return;

		String newWidgetValuesScript = null;
		synchronized (AlgorithmStepper.getLock()) {
			newWidgetValuesScript = saveValues();
		}
		AppPreferences.putString(PERSIST_KEY_WIDGET_VALUES,
				newWidgetValuesScript);

		mFlushRequired = false;
	}

	void persistStepperState(boolean withDelay) {
		mFlushRequired = true;
		if (!withDelay) {
			persistStepperStateAux();
			return;
		}

		// Make a delayed call to persist the values (on the UI thread)

		final long FLUSH_DELAY = 5000;

		// If there's already an active handler, don't replace it if it's far
		// enough in the future
		long currentTime = System.currentTimeMillis();
		if (mActiveFlushOperation != null
				&& mActiveFlushTime >= currentTime + FLUSH_DELAY / 2) {
			return;
		}

		mActiveFlushTime = currentTime + FLUSH_DELAY;
		Handler h = new Handler();
		mActiveFlushOperation = new Runnable() {
			@Override
			public void run() {
				if (this != mActiveFlushOperation) {
					return;
				}
				persistStepperStateAux();
			}
		};
		h.postDelayed(mActiveFlushOperation, FLUSH_DELAY);
	}

	private static AlgorithmOptions sAlgorithmOptions;

	private boolean mPrepared;
	private SlidingPaneLayout mSlidingPane;
	private ViewGroup mOptionsView;
	private Context sContext;
	private Map<String, AbstractWidget.Factory> mWidgetFactoryMap = new HashMap();
	private Map<String, AbstractWidget> mWidgetsMap = new HashMap();
	private boolean mFlushRequired;
	// The single valid pending flush operation, or null
	private Runnable mActiveFlushOperation;
	// Approximate time pending flush will occur at
	private long mActiveFlushTime;
}
