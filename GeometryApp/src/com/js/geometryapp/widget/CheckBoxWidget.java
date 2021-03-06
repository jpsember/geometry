package com.js.geometryapp.widget;

import java.util.Map;

import com.js.android.MyActivity;
import com.js.geometryapp.AlgorithmOptions;

import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import static com.js.android.UITools.*;

public class CheckBoxWidget extends AbstractWidget {

	public CheckBoxWidget(AlgorithmOptions options, Map attributes) {
		super(options, attributes);

		mCheckBox = new CheckBox(options.getContext());
		mCheckBox.setChecked(boolAttr(OPTION_VALUE, false));
		mCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						setValue(isChecked);
					}
				});
		getView().addView(mCheckBox);

		// We generate our own label, since we want it wider and on the
		// right,
		// unlike the usual widget labels
		TextView label;
		{
			label = new TextView(context());
			label.setText(getLabel(false));
			label.setPadding(
					MyActivity.getResolutionInfo().inchesToPixelsUI(.08f), 0,
					0, 0);
      LinearLayout.LayoutParams labelParams = layoutParams(false, 0);
			labelParams.gravity = Gravity.BOTTOM;
			label.setLayoutParams(labelParams);
		}
		getView().addView(label);
	}

	/**
	 * Set displayed value; subclasses should perform whatever translation /
	 * parsing is appropriate to convert the internal value to something
	 * displayed in the widget.
	 * 
	 * @param internalValue
	 */
	public void updateUserValue(String internalValue) {
		mChecked = internalValue.equals("true");
		if (mCheckBox != null)
			mCheckBox.setChecked(mChecked);
	}

	public CheckBoxWidget setValue(boolean value) {
		setValue(Boolean.toString(value));
		return this;
	}

	@Override
	public void setEnabled(boolean enabled) {
		mCheckBox.setEnabled(enabled);
	}

	/**
	 * Get displayed value, and transform to 'internal' representation.
	 */
	public String parseUserValue() {
		if (mCheckBox != null) {
			mChecked = mCheckBox.isChecked();
		}
		return Boolean.toString(mChecked);
	}

	// value of checkbox, which will be read from CheckBox only if not hidden
	private boolean mChecked;
	// null if widget is hidden
	private CheckBox mCheckBox;
}
