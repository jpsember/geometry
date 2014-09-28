package com.js.geometryapp.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.js.geometry.MyMath;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import static com.js.basic.Tools.*;

public class ComboBoxWidget extends AbstractWidget {

	public static final Factory FACTORY = new AbstractWidget.Factory() {
		@Override
		public String getName() {
			doNothing();
			return "combobox";
		}

		@Override
		public AbstractWidget constructInstance(Context context, Map attributes) {
			return new ComboBoxWidget(context, attributes);
		}
	};

	static final String ATTR_OPTIONS = "options";

	private void prepareOptions(Map options) {
		mMap = options;
		for (Object key : options.keySet()) {
			mKeys.add(key);
		}
	}

	public ComboBoxWidget(Context context, Map attributes) {
		super(context, attributes);

		mSpinner = new Spinner(context);

		Object options = attributes.get(ATTR_OPTIONS);
		if (options instanceof Map) {
			prepareOptions((Map) options);
		} else if (options instanceof List) {
			mKeys.addAll((List) options);
		}

		getView().addView(buildLabelView(true));

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		getView().addView(mSpinner, p);
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
			warning("caught " + e);
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

	private boolean prepared() {
		return mAdapter != null;
	}

	private ArrayAdapter<String> mAdapter;
	private Spinner mSpinner;
	private Map mMap;
	private ArrayList mKeys = new ArrayList();

}
