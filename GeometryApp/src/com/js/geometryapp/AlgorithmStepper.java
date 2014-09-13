package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.js.geometry.MyMath;
import com.js.geometry.Point;

public class AlgorithmStepper {

	public static interface Delegate {
		public void runAlgorithm();

		public void displayResults();
	}

	private static final String PERSIST_KEY_TARGET_STEP = "_alg_step";

	/**
	 * Get the singleton instance of the stepper
	 */
	public static AlgorithmStepper sharedInstance() {
		if (sStepper == null) {
			sStepper = new AlgorithmStepper();
		}
		return sStepper;
	}

	public void restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mTargetStep = savedInstanceState.getInt(PERSIST_KEY_TARGET_STEP);
		}
	}

	public void saveInstanceState(Bundle outState) {
		outState.putInt(PERSIST_KEY_TARGET_STEP, mTargetStep);
	}

	/**
	 * Set the delegate, which actually performs the algorithm, and displays it
	 * 
	 */
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate;
		// Run algorithm to completion to determine the number of steps
		mTotalStepsKnown = false;
		performAlgorithm();
		mTotalStepsKnown = true;
		mTotalSteps = mCurrentStep;
		updateStepperView();
	}

	/**
	 * Print a warning if no delegate has been defined
	 */
	protected void verifyDelegateDefined() {
		if (mDelegate == null) {
			warning("no algorithm delegate defined");
		}
	}

	/**
	 * Get the stepper controller view, constructing it if necessary
	 * 
	 * @param context
	 *            context to use, in case construction necessary
	 * @return view
	 */
	public View controllerView(Context context) {
		if (mStepperView == null) {
			mStepperView = new AlgorithmStepperView(context, this);
		}
		return mStepperView.view();
	}

	/**
	 * Determine if we should stop and display this frame of the current
	 * algorithm; should be followed by a call to update() if this returns true
	 */
	public boolean update() {
		if (!mTotalStepsKnown) {
			// We need to see what the message is, to determine if it's a
			// milestone. We won't throw an exception to end the algorithm
			// in this case.
			mCurrentStep++;
			return true;
		} else {
			if (mCurrentStep == mTargetStep) {
				clearDisplayList();
				return true;
			}
		}
		mCurrentStep++;
		return false;
	}

	/**
	 * Generate an algorithm step. For efficiency, should only be called if
	 * update() returns true
	 * 
	 * @param message
	 *            message to display, which may cause other elements to be
	 *            displayed via side effects (not yet implemented)
	 */
	public void show(Object message) {
		String messageString = message.toString();
		String displayedMessageString = trimMilestonePrefix(messageString);
		if (!mTotalStepsKnown) {
			// We're only examining the message to see if it's a milestone
			if (displayedMessageString != messageString) {
				addMilestone(mCurrentStep - 1);
			}
		} else {
			mFrameTitle = displayedMessageString;
			throw new DesiredStepReachedException("reached desired step; "
					+ displayedMessageString);
		}
	}

	/**
	 * Dispose of the controller's view; should be called when activity's
	 * onDestroy() is called
	 */
	public void destroy() {
		mTotalStepsKnown = false;
		mStepperView = null;
	}

	/**
	 * Add an element to be displayed with this algorithm frame
	 * 
	 * @param element
	 * @return an empty string, as a convenience so elements can be added as a
	 *         side effect of constructing show(...) message arguments
	 */
	public String plotElement(AlgDisplayElement element) {
		mDisplayElements.add(element);
		return "";
	}

	/**
	 * Render algorithm frame, by plotting (and disposing of) any added
	 * elements, as well as the frame's title
	 */
	public void render() {
		for (AlgDisplayElement element : mDisplayElements) {
			element.render();
		}
		mDisplayElements.clear();
		if (mFrameTitle != null) {
			AlgDisplayElement.renderFrameTitle(mFrameTitle);
			mFrameTitle = null;
		}
	}

	public String plotRay(Point p1, Point p2) {
		return plotElement(new AlgDisplayRay(p1, p2));
	}

	/**
	 * An exception of this type is thrown by the algorithm stepper when the
	 * target step is reached
	 */
	protected static class DesiredStepReachedException extends RuntimeException {
		public DesiredStepReachedException(String message) {
			super(message);
		}
	}

	private void performAlgorithm() {
		mCurrentStep = 0;

		if (!mTotalStepsKnown) {
			mMilestones.clear();
			addMilestone(mCurrentStep);
		}

		try {
			mDelegate.runAlgorithm();
			if (!mTotalStepsKnown) {
				addMilestone(mCurrentStep);
			}
		} catch (DesiredStepReachedException e) {
		}
	}

	protected void setStepperView(AlgorithmStepperView view) {
		mStepperView = view;
		updateStepperView();
	}

	private AlgorithmStepper() {
	}

	private String trimMilestonePrefix(String message) {
		if (!message.startsWith("*"))
			return message;
		return message.substring(1);
	}

	/**
	 * Propagate values from this (the controller) to the controller's view.
	 */
	private void updateStepperView() {
		if (mStepperView != null) {
			if (mTotalStepsKnown) {
				// While changing the total steps, the controller view may try
				// to change the target step on us; ignore such events
				mIgnoreStepperView = true;
				mStepperView.setTotalSteps(mTotalSteps);
				mIgnoreStepperView = false;
				mStepperView.setTargetStep(mTargetStep);
				if (mTargetStep != mCurrentStep) {
					performAlgorithm();
					mDelegate.displayResults();
				}
			}
		}
	}

	protected void setTargetStep(int step) {
		if (mIgnoreStepperView) {
			return;
		}
		mTargetStep = MyMath.clamp(step, 0, mTotalSteps - 1);
		updateStepperView();
	}

	protected void adjustTargetStep(int delta) {
		int seekStep = mTargetStep + delta;
		setTargetStep(seekStep);
	}

	protected void adjustDisplayedMilestone(int delta) {
		int prev = -1, next = -1;
		for (int i = 0; i < mMilestones.size(); i++) {
			int k = mMilestones.get(i);
			if (k < mTargetStep)
				prev = k;
			if (k > mTargetStep && next < 0)
				next = k;
		}
		int seekStep = (delta < 0) ? prev : next;
		if (seekStep >= 0) {
			setTargetStep(seekStep);
		}
	}

	private void addMilestone(int n) {
		int lastMilestone = -1;
		if (!mMilestones.isEmpty()) {
			lastMilestone = mMilestones.get(mMilestones.size() - 1);
		}
		ASSERT(n >= lastMilestone, "addMilestone " + n + ", last "
				+ lastMilestone + ", list " + mMilestones);
		if (n != lastMilestone)
			mMilestones.add(n);
	}

	protected int targetStep() {
		return mTargetStep;
	}

	protected int totalSteps() {
		return mTotalSteps;
	}

	private void clearDisplayList() {
		mDisplayElements.clear();
	}

	// The singleton instance of this class
	private static AlgorithmStepper sStepper;

	private ArrayList<AlgDisplayElement> mDisplayElements = new ArrayList();
	private String mFrameTitle;
	private boolean mIgnoreStepperView;
	private ArrayList<Integer> mMilestones = new ArrayList();
	private int mTargetStep;
	private int mCurrentStep;
	private int mTotalSteps;
	private boolean mTotalStepsKnown;
	private AlgorithmStepperView mStepperView;
	private Delegate mDelegate;
}
