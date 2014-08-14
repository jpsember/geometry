package com.js.android;

import static com.js.basic.Tools.*;

import android.app.FragmentManager;

public class FragmentReference<T extends MyFragment> {

	public FragmentReference(MyActivity activity, Class theClass) {
		mActivity = activity;
		mClass = theClass;
		mName = MyFragment.deriveFragmentName(theClass);
		activity.addReference(this);
	}

	public void refresh() {
		if (db)
			pr("refresh " + this);
		T fragment = mFragment;

		// If still null, see if there's one in the FragmentManager (which may
		// include items in the back stack)
		if (fragment == null) {
			FragmentManager manager = mActivity.getFragmentManager();
			fragment = (T) manager.findFragmentByTag(mName);
		}

		// If still null, construct a new instance and register it with the
		// activity
		if (fragment == null) {
			try {
				fragment = (T) mClass.newInstance();
			} catch (Throwable e) {
				die("failed to build instance of " + mName, e);
			}
			mActivity.registerFragment(fragment);
		}
		mFragment = fragment;
	}

	public T f() {
		ASSERT(mFragment != null);
		return mFragment;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(nameOf(this));
		sb.append(" mName:" + mName);
		sb.append(" mFragment:" + nameOf(mFragment));
		return sb.toString();
	}

	String getName() {
		return mName;
	}

	void setFragment(T fragment) {
		mFragment = fragment;
	}

	private MyActivity mActivity;
	private T mFragment;
	private String mName;
	private Class mClass;
}
