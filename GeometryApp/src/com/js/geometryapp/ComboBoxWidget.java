package com.js.geometryapp;

import java.util.ArrayList;
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

	public ComboBoxWidget(Context context, Map attributes) {
		super(context, attributes);

		mSpinner = new Spinner(context);

		mOptions = (ArrayList<String>) attributes.get(ATTR_OPTIONS);
		if (mOptions == null || mOptions.size() == 0)
			die("no options defined for combobox " + getId());
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, mOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);
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

		getView().addView(buildLabelView(true));

		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		getView().addView(mSpinner, p);
	}

	/**
	 * Set displayed value; subclasses should perform whatever translation /
	 * parsing is appropriate to convert the internal value to something
	 * displayed in the widget.
	 * 
	 * @param internalValue
	 */
	public void updateUserValue(String internalValue) {
		int index = 0;
		try {
			index = Integer.parseInt(internalValue);
		} catch (NumberFormatException e) {
			warning("caught " + e);
		}
		index = MyMath.clamp(index, 0, mOptions.size() - 1);
		mSpinner.setSelection(index, false);
	}

	public void setValue(boolean value) {
		setValue(Boolean.toString(value));
	}

	/**
	 * Get displayed value, and transform to 'internal' representation.
	 */
	public String parseUserValue() {
		return Integer.toString(mSpinner.getSelectedItemPosition());
	}

	private Spinner mSpinner;
	private ArrayList<String> mOptions;

}
