package com.js.geometryapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.js.android.AppPreferences;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.CheckBoxWidget;
import com.js.geometryapp.widget.ComboBoxWidget;
import com.js.geometryapp.widget.SliderWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;
import com.js.json.JSONEncoder;
import com.js.json.JSONParser;

import static com.js.basic.Tools.*;

public class AlgorithmOptions {

	private static final String PERSIST_KEY_WIDGET_VALUES = "_widget_values";

	static AlgorithmOptions construct(Context context) {
		sAlgorithmOptions = new AlgorithmOptions(context);
		return sharedInstance();
	}

	/**
	 * Prepare the options views
	 * 
	 * @param containingView
	 *            the view into which the option views should be placed
	 */
	void prepareViews(ViewGroup containingView) {
		mContainingView = containingView;

		// Construct primary and secondary views within this one
		mPrimaryWidgetGroup = new WidgetGroup(constructSubView());
		mSecondaryWidgetGroup = new WidgetGroup(constructSubView());

		addPrimaryWidgets();

		mContainingView.addView(mPrimaryWidgetGroup.view());
		mContainingView.addView(mSecondaryWidgetGroup.view());

		selectWidgetGroup(mSecondaryWidgetGroup);
	}

	private void addPrimaryWidgets() {
		selectWidgetGroup(mPrimaryWidgetGroup);
		addButton("_testprimary_");
	}

	/**
	 * Get the singleton instance of the options object
	 */
	public static AlgorithmOptions sharedInstance() {
		ASSERT(sAlgorithmOptions != null);
		return sAlgorithmOptions;
	}

	/**
	 * Add a slider widget
	 */
	public SliderWidget addSlider(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		SliderWidget w = new SliderWidget(sContext, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a checkbox widget
	 */
	public CheckBoxWidget addCheckBox(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		CheckBoxWidget w = new CheckBoxWidget(sContext, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a combobox widget
	 */
	public ComboBoxWidget addComboBox(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		ComboBoxWidget w = new ComboBoxWidget(sContext, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a button widget
	 */
	public ButtonWidget addButton(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		ButtonWidget w = new ButtonWidget(sContext, attributes);
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
	 * Private constructor
	 */
	private AlgorithmOptions(Context context) {
		sContext = context;
	}

	private static Map<String, Object> buildAttributes(String identifier,
			Object attrPairs[]) {
		if (attrPairs.length % 2 != 0)
			throw new IllegalArgumentException();
		Map<String, Object> attributes = new HashMap();
		attributes.put("id", identifier);
		for (int i = 0; i < attrPairs.length; i += 2) {
			attributes.put(attrPairs[i + 0].toString(), attrPairs[i + 1]);
		}
		return attributes;
	}

	void addWidget(AbstractWidget w) {
		// Add it to the map
		AbstractWidget previousMapping = mWidgetsMap.put(w.getId(), w);
		if (previousMapping != null)
			die("widget id " + w.getId() + " already exists");

		// Add it to the options view, if it's not detached
		if (w.boolAttr(AbstractWidget.OPTION_DETACHED, false))
			return;

		if (mWidgetGroup == null)
			die("no widget group selected");

		mWidgetGroup.add(w);
	}

	/**
	 * Compile widget values to JSON string
	 */
	private String saveValues() {
		Map<String, String> values = new HashMap();
		for (String widgetId : mWidgetsMap.keySet()) {
			AbstractWidget w = mWidgetsMap.get(widgetId);
			if (!w.boolAttr("hasvalue", true))
				continue;
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

	/**
	 * Process a change in a widget's value (or a button click, if it's a
	 * button); notify any listeners, and trigger a refresh of the algorithm
	 * view
	 * 
	 * @param widget
	 * @param listeners
	 *            listeners to notify
	 */
	public void processWidgetValue(AbstractWidget widget,
			Collection<Listener> listeners) {

		if (!mPrepared)
			return;

		synchronized (AlgorithmStepper.getLock()) {
			for (Listener listener : listeners) {
				listener.valueChanged(widget);
			}
			// Unless the 'refresh' option exists and is false,
			// trigger a refresh of the algorithm view.
			if (widget.boolAttr(AbstractWidget.OPTION_REFRESH_ALGORITHM, true)) {
				AlgorithmStepper.sharedInstance().refresh();
			}
		}
		persistStepperState(true);
	}

	/**
	 * Construct a view that will be stacked vertically with others within the
	 * main container view
	 */
	private ViewGroup constructSubView() {
		LinearLayout view = new LinearLayout(sContext);
		view.setOrientation(LinearLayout.VERTICAL);
		if (AbstractWidget.SET_DEBUG_COLORS) {
			view.setBackgroundColor(OurGLTools.debugColor());
		}
		return view;
	}

	void selectWidgetGroup(WidgetGroup group) {
		mWidgetGroup = group;
	}

	private static class WidgetGroup {
		public WidgetGroup(ViewGroup view) {
			mView = view;
			mWidgets = new ArrayList();
		}

		public ViewGroup view() {
			return mView;
		}

		public ArrayList<AbstractWidget> widgets() {
			return mWidgets;
		}

		public void add(AbstractWidget widget) {
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			mView.addView(widget.getView(), p);
			mWidgets.add(widget);
		}

		private ViewGroup mView;
		private ArrayList<AbstractWidget> mWidgets;
	}

	private static AlgorithmOptions sAlgorithmOptions;

	private ViewGroup mContainingView;
	private boolean mPrepared;
	private Context sContext;
	private Map<String, AbstractWidget> mWidgetsMap = new HashMap();
	private WidgetGroup mPrimaryWidgetGroup, mSecondaryWidgetGroup;
	private WidgetGroup mWidgetGroup;

	private boolean mFlushRequired;
	// The single valid pending flush operation, or null
	private Runnable mActiveFlushOperation;
	// Approximate time pending flush will occur at
	private long mActiveFlushTime;

}
