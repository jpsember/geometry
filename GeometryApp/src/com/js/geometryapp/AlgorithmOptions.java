package com.js.geometryapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.js.android.AppPreferences;
import com.js.android.QuiescentDelayOperation;
import com.js.geometryapp.widget.AbstractWidget;
import com.js.geometryapp.widget.ButtonWidget;
import com.js.geometryapp.widget.CheckBoxWidget;
import com.js.geometryapp.widget.ComboBoxWidget;
import com.js.geometryapp.widget.SliderWidget;
import com.js.geometryapp.widget.AbstractWidget.Listener;
import com.js.geometryapp.widget.TextWidget;
import com.js.json.JSONEncoder;
import com.js.json.JSONParser;

import static com.js.basic.Tools.*;

public class AlgorithmOptions {

	static final String WIDGET_ID_TOTALSTEPS = "_steps_";
	static final String WIDGET_ID_TARGETSTEP = "_target_";

	// Algorithm-specific versions of the target & total steps
	private static final String WIDGET_ID_TOTALSTEPS_AUX = "_"
			+ WIDGET_ID_TOTALSTEPS;
	private static final String WIDGET_ID_TARGETSTEP_AUX = "_"
			+ WIDGET_ID_TARGETSTEP;

	private static final String WIDGET_ID_ALGORITHM = "_algorithm_";

	private static final String PERSIST_KEY_WIDGET_VALUES = "_widget_values";

	private static final boolean DIAGNOSE_PERSISTENCE = false;

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
		if (mAlgorithms.size() == 1) {
			AlgorithmRecord r = mAlgorithms.get(0);
			addHeader(r.delegate().getAlgorithmName());
		} else {
			ComboBoxWidget w = addComboBox(WIDGET_ID_ALGORITHM, "label",
					"Algorithm");
			for (AlgorithmRecord r : mAlgorithms) {
				w.addItem(r.delegate().getAlgorithmName());
			}
			w.prepare();
			w.addListener(new Listener() {
				@Override
				public void valueChanged(AbstractWidget widget) {
					int index = widget.getIntValue();
					selectAlgorithm(mAlgorithms.get(index));
				}
			});
		}
		// Add some vertical space by adding blank text; perhaps later we'll add
		// widgets for this purpose
		addStaticText("");
	}

	/**
	 * Add a slider widget
	 */
	public SliderWidget addSlider(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		SliderWidget w = new SliderWidget(this, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a checkbox widget
	 */
	public CheckBoxWidget addCheckBox(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		CheckBoxWidget w = new CheckBoxWidget(this, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a combobox widget
	 */
	public ComboBoxWidget addComboBox(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		ComboBoxWidget w = new ComboBoxWidget(this, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a button widget
	 */
	public ButtonWidget addButton(String id, Object... attributePairs) {
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		ButtonWidget w = new ButtonWidget(this, attributes);
		addWidget(w);
		return w;
	}

	/**
	 * Add a text widget; assigns it a unique id
	 * 
	 * @param content
	 *            the text appearing in the widget
	 */
	public TextWidget addStaticText(String content, Object... attributePairs) {
		mPreviousTextIndex++;
		String id = "__text_" + mPreviousTextIndex;
		Map<String, Object> attributes = buildAttributes(id, attributePairs);
		attributes.put("label", content);
		TextWidget w = new TextWidget(this, attributes);
		addWidget(w);
		return w;
	}

	public TextWidget addHeader(String content) {
		return addStaticText(content, TextWidget.OPTION_HEADER, true,
				TextWidget.OPTION_CENTER, true);
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
					+ widgetName + "; ids: " + d(mWidgetsMap.keySet(), false));
		return field;
	}

	AlgorithmOptions(Context context, AlgorithmStepper stepper) {
		mContext = context;
		mStepper = stepper;

		mWidgetsMap = new HashMap();
		mAlgorithms = new ArrayList();
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

		boolean algSpecific = (mSecondaryWidgetGroup != null);
		WidgetGroup destination = algSpecific ? mSecondaryWidgetGroup.widgets()
				: mPrimaryWidgetGroup;
		destination.add(w);

		// If this is being added to the secondary (i.e. algorithm-specific)
		// group, and it's not hidden, flag it as a widget that changes the
		// algorithm total steps
		if (algSpecific && !w.boolAttr(AbstractWidget.OPTION_HIDDEN, false)) {
			w.setAttribute(AbstractWidget.OPTION_RECALC_ALGORITHM_STEPS, true);
		}
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
			pr("\nRestoring JSON:\n" + script + "\n" + "Widgets:\n"
					+ d(mWidgetsMap) + "\n");

		if (script != null) {
			JSONParser parser = new JSONParser(script);
			Map<String, Map> values = (Map) parser.next();
			for (String algName : values.keySet()) {
				if (algName.equals(PRIMARY_GROUP_KEY)) {
					activateSecondaryWidgetGroup(null);
				} else {
					AlgorithmRecord rec = findAlgorithm(algName);
					if (rec == null) {
						warning("can't find algorithm '" + algName + "'");
						continue;
					}
					activateSecondaryWidgetGroup(rec);
				}
				Map<String, String> widgetValues = values.get(algName);
				for (String key : widgetValues.keySet()) {
					String value = widgetValues.get(key);
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
		mPrepared = true;
		int algNumber = 0;
		if (mAlgorithms.size() > 1)
			algNumber = getIntValue(WIDGET_ID_ALGORITHM);
		selectAlgorithm(mAlgorithms.get(algNumber));
	}

	/**
	 * Save total, target steps to algorithm-specific versions (if there's an
	 * active algorithm)
	 */
	private void saveStepsInformation() {
		if (mActiveAlgorithm != null) {
			setValue(WIDGET_ID_TOTALSTEPS_AUX, readTotalSteps());
			setValue(WIDGET_ID_TARGETSTEP_AUX, readTargetStep());
		}
	}

	private void selectAlgorithm(AlgorithmRecord ar) {
		if (mActiveAlgorithm == ar)
			return;
		QuiescentDelayOperation.cancelExisting(mPendingRecalculationOperation);

		mPrepared = false;

		saveStepsInformation();

		activateSecondaryWidgetGroup(ar);
		if (mActiveAlgorithm != null)
			mContainingView.removeView(mActiveAlgorithm.widgets().view());
		mActiveAlgorithm = ar;
		mContainingView.addView(mActiveAlgorithm.widgets().view());

		// Copy total, target steps from algorithm-specific versions
		setTotalSteps(getIntValue(WIDGET_ID_TOTALSTEPS_AUX));
		setTargetStep(getIntValue(WIDGET_ID_TARGETSTEP_AUX));

		mPrepared = true;

		// Bound the target step to the total step slider's value. We must do
		// this explicitly here, because
		// the listener that normally does this was disabled while restoring the
		// stepper state
		SliderWidget s = getWidget(WIDGET_ID_TARGETSTEP);
		s.setMaxValue(readTotalSteps());
	}

	private void persistStepperStateAux() {
		final boolean db = DIAGNOSE_PERSISTENCE;
		if (!mFlushRequired)
			return;

		// At present, it only saves widgets that appear in a WidgetGroup. This
		// omits the target step slider, but that's ok, because we have hidden
		// algorithm-specific versions that serve this purpose.
		// But we must now make sure those versions are up to date.
		// Disable recursive flush attempts while updating these (issue #82):
		mPrepared = false;
		saveStepsInformation();
		mPrepared = true;

		String newWidgetValuesScript = null;
		synchronized (mStepper.getLock()) {
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
		if (QuiescentDelayOperation.replaceExisting(mPendingFlushOperation)) {
			final float FLUSH_DELAY = 5.0f;
			mPendingFlushOperation = new QuiescentDelayOperation("flush",
					FLUSH_DELAY, new Runnable() {
						public void run() {
							persistStepperStateAux();
						}
					});
		}
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

		synchronized (mStepper.getLock()) {
			for (Listener listener : listeners) {
				listener.valueChanged(widget);
			}

			// Unless the 'refresh' option exists and is false,
			// trigger a refresh of the algorithm view.
			if (widget.boolAttr(AbstractWidget.OPTION_REFRESH_ALGORITHM, true)) {
				mStepper.refresh();
			}

			// If this is a widget that may change the algorithm total steps,
			// trigger a recalculation after a delay
			if (widget.boolAttr(AbstractWidget.OPTION_RECALC_ALGORITHM_STEPS,
					false)) {
				final float RECALC_DELAY = 2.0f;
				if (QuiescentDelayOperation
						.replaceExisting(mPendingRecalculationOperation)) {
					mPendingRecalculationOperation = new QuiescentDelayOperation(
							"calc steps", RECALC_DELAY, new Runnable() {
								public void run() {
									mStepper.calculateAlgorithmTotalSteps();
								}
							});
				}
			}
		}
		persistStepperState(true);
	}

	/**
	 * Construct a view that will be stacked vertically with others within the
	 * main container view
	 */
	private ViewGroup constructSubView() {
		LinearLayout view = new LinearLayout(mContext);
		view.setOrientation(LinearLayout.VERTICAL);
		OurGLTools.applyDebugColors(view);
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
	void activateSecondaryWidgetGroup(AlgorithmRecord algorithmRecord) {
		if (algorithmRecord == mSecondaryWidgetGroup)
			return;
		if (mSecondaryWidgetGroup != null) {
			for (AbstractWidget w : mSecondaryWidgetGroup.widgets().widgets()) {
				mWidgetsMap.remove(w.getId());
			}
			mSecondaryWidgetGroup = null;
		}
		mSecondaryWidgetGroup = algorithmRecord;
		if (algorithmRecord != null) {
			for (AbstractWidget w : algorithmRecord.widgets().widgets()) {
				mWidgetsMap.put(w.getId(), w);
			}
		}
	}

	void begin(ArrayList<Algorithm> algorithms) {
		// Create a WidgetGroup for each algorithm
		for (Algorithm algorithm : algorithms) {
			AlgorithmRecord algorithmRecord = new AlgorithmRecord();
			mAlgorithms.add(algorithmRecord);

			algorithmRecord.setDelegate(algorithm);
			algorithmRecord.setWidgetGroup(new WidgetGroup(constructSubView()));
			activateSecondaryWidgetGroup(algorithmRecord);
			// Add hidden values to represent total, target steps; these will be
			// copied to/from the main slider as the algorithm becomes
			// active/inactive
			addSlider(WIDGET_ID_TARGETSTEP_AUX, AbstractWidget.OPTION_HIDDEN,
					true);
			addSlider(WIDGET_ID_TOTALSTEPS_AUX, AbstractWidget.OPTION_HIDDEN,
					true);

			algorithm.prepareOptions(this);
		}
		activateSecondaryWidgetGroup(null);

		addPrimaryWidgets();
		mStepper.addStepperViewListeners();

		restoreStepperState();
	}

	/**
	 * Read total steps from widget
	 */
	int readTotalSteps() {
		return getIntValue(AlgorithmOptions.WIDGET_ID_TOTALSTEPS);
	}

	/**
	 * Read target step from widget
	 */
	int readTargetStep() {
		return getIntValue(AlgorithmOptions.WIDGET_ID_TARGETSTEP);
	}

	void setTargetStep(int targetStep) {
		setValue(AlgorithmOptions.WIDGET_ID_TARGETSTEP, targetStep);
	}

	void setTotalSteps(int totalSteps) {
		setValue(AlgorithmOptions.WIDGET_ID_TOTALSTEPS, totalSteps);
	}

	/**
	 * Call the active algorithm's run() method
	 */
	void runActiveAlgorithm() {
		mActiveAlgorithm.delegate().run(mStepper);
	}

	public Context getContext() {
		return mContext;
	}

	private AlgorithmStepper mStepper;
	private ViewGroup mContainingView;
	// Until this flag is true, no listeners are sent messages about widget
	// value changes
	private boolean mPrepared;
	private Context mContext;
	private Map<String, AbstractWidget> mWidgetsMap;
	private WidgetGroup mPrimaryWidgetGroup;
	private ArrayList<AlgorithmRecord> mAlgorithms;
	private AlgorithmRecord mActiveAlgorithm;
	private AlgorithmRecord mSecondaryWidgetGroup;

	private boolean mFlushRequired;
	private QuiescentDelayOperation mPendingFlushOperation;
	private QuiescentDelayOperation mPendingRecalculationOperation;

	// For generating unique text ids
	private int mPreviousTextIndex;
}
