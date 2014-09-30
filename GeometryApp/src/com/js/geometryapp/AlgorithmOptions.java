package com.js.geometryapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
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

	private static final boolean DIAGNOSE_PERSISTENCE = false;

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

		mPrimaryWidgetGroup = new WidgetGroup(constructSubView());
		mContainingView.addView(mPrimaryWidgetGroup.view());
	}

	private void addPrimaryWidgets() {
		// tell addWidget() to add widgets to the primary group
		mAddingPrimaryWidgets = true;
		if (mAlgorithms.size() == 1) {
			unimp("add a label for the single algorithm");
		} else {
			ComboBoxWidget w = addComboBox("Algorithm");
			for (AlgorithmRecord r : mAlgorithms)
				w.addItem(r.delegate().getAlgorithmName());
			w.prepare();
			w.addListener(new Listener() {
				@Override
				public void valueChanged(AbstractWidget widget) {
					int index = widget.getIntValue();
					selectAlgorithm(mAlgorithms.get(index));
				}
			});
		}
		mAddingPrimaryWidgets = false;
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
		mStepper = AlgorithmStepper.sharedInstance();
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

		WidgetGroup destination;
		if (mAddingPrimaryWidgets) {
			destination = mPrimaryWidgetGroup;
		} else {
			destination = mSecondaryWidgetGroup;
		}
		if (destination == null)
			die("no widget group selected");

		destination.add(w);
	}

	private AlgorithmRecord findAlgorithm(String name) {
		for (AlgorithmRecord rec : mAlgorithms) {
			if (rec.delegate().getAlgorithmName().equals(name))
				return rec;
		}
		return null;
	}

	/**
	 * Compile widget values to JSON string
	 */
	private String saveValues() {
		Map<String, Map> groupValues = new HashMap();
		groupValues.put(PRIMARY_GROUP_KEY,
				getWidgetValueMap(mPrimaryWidgetGroup));

		for (AlgorithmRecord a : mAlgorithms) {
			groupValues.put(a.delegate().getAlgorithmName(),
					getWidgetValueMap(a.widgets()));
		}
		return JSONEncoder.toJSON(groupValues);
	}

	private Map getWidgetValueMap(WidgetGroup group) {
		Map<String, String> values = new HashMap();
		for (AbstractWidget w : group.widgets()) {
			if (!w.boolAttr("hasvalue", true))
				continue;
			values.put(w.getId(), w.getValue());
		}
		return values;
	}

	private static final String PRIMARY_GROUP_KEY = "_primarygroup_";

	private void restoreStepperState() {

		final boolean db = DIAGNOSE_PERSISTENCE;

		String script = AppPreferences.getString(PERSIST_KEY_WIDGET_VALUES,
				null);
		if (db)
			pr("\nRestoring JSON:\n" + script + "\n");

		if (script != null) {
			JSONParser parser = new JSONParser(script);
			Map<String, Map> values = (Map) parser.next();
			for (String algName : values.keySet()) {
				if (algName.equals(PRIMARY_GROUP_KEY)) {
					selectWidgetGroup(mPrimaryWidgetGroup);
				} else {
					AlgorithmRecord rec = findAlgorithm(algName);
					if (rec == null) {
						warning("can't find algorithm '" + algName + "'");
						continue;
					}
					selectWidgetGroup(rec.widgets());
				}
				Map<String, String> widgetValues = values.get(algName);
				for (String key : widgetValues.keySet()) {
					String value = widgetValues.get(key);
					// ..remove widgets from global map whenever we select a
					// widget group?
					AbstractWidget w = mWidgetsMap.get(key);
					if (w == null) {
						warning("can't find widget named '" + key + "'");
						continue;
					}
					// TODO: catch exceptions that may get thrown here
					w.setValue(value);
				}
			}
		}
		selectWidgetGroup(null);
		mPrepared = true;

		selectAlgorithm(mAlgorithms.get(0));
	}

	private void selectAlgorithm(AlgorithmRecord ar) {
		selectWidgetGroup(ar.widgets());
		if (mPlottedSecondaryGroup != ar.widgets()) {
			if (mPlottedSecondaryGroup != null)
				mContainingView.removeView(mPlottedSecondaryGroup.view());
			mPlottedSecondaryGroup = ar.widgets();
			mContainingView.addView(mPlottedSecondaryGroup.view());
		}
		mStepper.setDelegate(ar.delegate());
	}

	private void persistStepperStateAux() {
		final boolean db = DIAGNOSE_PERSISTENCE;
		if (!mFlushRequired)
			return;

		// At present, it only saves widgets that appear in a WidgetGroup. This
		// omits the target step slider, but that's probably ok, because we'll
		// use other methods to persist its value (probably by having dedicated
		// hidden controls for each AlgorithmRecord)
		String newWidgetValuesScript = null;
		synchronized (AlgorithmStepper.getLock()) {
			newWidgetValuesScript = saveValues();
		}
		if (db) {
			pr("\nSaving JSON:\n" + newWidgetValuesScript + "\n"
					+ "Widget map:\n" + d(mWidgetsMap) + "\n");
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
				mStepper.refresh();
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

	/**
	 * Select the active secondary widget group. Any old auxilliary widget
	 * group's widgets are removed from the map, and the new one's are added.
	 * This means only one secondary group's widgets are accessible to the
	 * options at a time, and allows the same name to be used for different
	 * widgets (as long as they are in different secondary groups).
	 * 
	 * @param group
	 *            secondary group, or null
	 */
	void selectWidgetGroup(WidgetGroup group) {
		warning("what about installing / removing this view from the container?");
		if (group == mSecondaryWidgetGroup)
			return;
		if (mSecondaryWidgetGroup != null) {
			// Remove old secondary widgets from master map
			for (AbstractWidget w : mSecondaryWidgetGroup.widgets()) {
				mWidgetsMap.remove(w.getId());
			}
			mSecondaryWidgetGroup = null;
		}
		mSecondaryWidgetGroup = group;
		if (group != null) {
			// Remove old secondary widgets from master map
			for (AbstractWidget w : group.widgets()) {
				mWidgetsMap.put(w.getId(), w);
			}
		}
	}

	void resume(ArrayList<Algorithm> algorithms) {
		// Create a WidgetGroup for each algorithm
		mAlgorithms = new ArrayList();
		for (Algorithm a : algorithms) {
			pr("Options.resume, alg " + a.getAlgorithmName());
			AlgorithmRecord arec = new AlgorithmRecord();
			arec.setDelegate(a);
			WidgetGroup g = new WidgetGroup(constructSubView());
			arec.setWidgetGroup(g);
			selectWidgetGroup(g);
			a.prepareOptions();
			mAlgorithms.add(arec);
		}
		selectWidgetGroup(null);

		addPrimaryWidgets();

		restoreStepperState();
	}

	private static AlgorithmOptions sAlgorithmOptions;

	private AlgorithmStepper mStepper;
	private ViewGroup mContainingView;
	private boolean mPrepared;
	private Context sContext;
	private Map<String, AbstractWidget> mWidgetsMap = new HashMap();
	private WidgetGroup mPrimaryWidgetGroup;
	private WidgetGroup mSecondaryWidgetGroup;
	// Which widget group's views, if any, are visible
	private WidgetGroup mPlottedSecondaryGroup;
	private ArrayList<AlgorithmRecord> mAlgorithms;
	private boolean mAddingPrimaryWidgets;

	private boolean mFlushRequired;
	// The single valid pending flush operation, or null
	private Runnable mActiveFlushOperation;
	// Approximate time pending flush will occur at
	private long mActiveFlushTime;

}
