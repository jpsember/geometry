package com.js.geometryapp.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.js.android.UITools;
import com.js.basic.MyMath;
import com.js.geometryapp.AlgorithmOptions;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import static com.js.android.Tools.*;
import static com.js.basic.Tools.*;

public class ComboBoxWidget extends AbstractWidget {

	static final String ATTR_OPTIONS = "options";

	private void prepareOptions(Map options) {
		mMap = options;
		for (Object key : options.keySet()) {
			mKeys.add(key);
		}
	}

	public ComboBoxWidget(AlgorithmOptions options, Map attributes) {
		super(options, attributes);

		mSpinner = new Spinner(options.getContext());

		Object comboOptions = attributes.get(ATTR_OPTIONS);
		if (comboOptions instanceof Map) {
			prepareOptions((Map) comboOptions);
		} else if (comboOptions instanceof List) {
			mKeys.addAll((List) comboOptions);
		}

		getView().addView(buildLabelView(true));
		getView().addView(mSpinner, UITools.layoutParams(false));
	}

	public ComboBoxWidget addItem(Object item) {
		if (mMap != null)
			die("must supply key/value pair");
		mKeys.add(item);
		return this;
	}

	public ComboBoxWidget addItem(Object key, Object value) {
		if (mMap == null) {
			if (mKeys.size() != 0)
				die("only keys are supported");
			mMap = new HashMap();
		}
		mKeys.add(key);
		mMap.put(key, value);
		return this;
	}

	public ComboBoxWidget prepare() {
		if (prepared())
			die("already prepared");
		mAdapter = new ArrayAdapter<String>(mSpinner.getContext(),
				android.R.layout.simple_spinner_item, mKeys);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(mAdapter);
		mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				setValue(Integer.toString(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		return this;
	}

	public void updateUserValue(String internalValue) {
		int index = 0;
		try {
			index = Integer.parseInt(internalValue);
		} catch (NumberFormatException e) {
			showException(context(), e, null);
		}
		index = MyMath.clamp(index, 0, mKeys.size() - 1);
		mSpinner.setSelection(index, false);
	}

	public String parseUserValue() {
		return Integer.toString(mSpinner.getSelectedItemPosition());
	}

	/**
	 * Get the label corresponding to the current item
	 */
	public Object getSelectedKey() {
		if (!prepared())
			die("not prepared: " + getId());
		int index = getIntValue();
		if (index < 0 || index >= mKeys.size()) {
			warning("index " + index + " out of range of keys: " + d(mKeys));
			index = 0;
		}
		return mKeys.get(index);
	}

	/**
	 * Get the value corresponding to the current item
	 */
	public Object getSelectedValue() {
		if (mMap == null)
			die("values are not key/value pairs " + getId());
		return mMap.get(getSelectedKey());
	}

	@Override
	public void setEnabled(boolean enabled) {
		mSpinner.setEnabled(enabled);
	}

	private boolean prepared() {
		return mAdapter != null;
	}

	private ArrayAdapter<String> mAdapter;
	private Spinner mSpinner;
	private Map mMap;
	private ArrayList mKeys = new ArrayList();

}
