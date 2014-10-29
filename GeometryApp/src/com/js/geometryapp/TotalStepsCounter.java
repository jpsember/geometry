package com.js.geometryapp;

import static com.js.basic.Tools.*;

import java.util.ArrayList;

import com.js.geometry.DefaultStepper;
import com.js.geometry.Rect;

class TotalStepsCounter extends DefaultStepper {

	TotalStepsCounter(ConcreteStepper original) {
		mOriginalStepper = original;
	}

	@Override
	public Rect algorithmRect() {
		return mOriginalStepper.algorithmRect();
	}

	@Override
	public boolean isActive() {
		return mActive;
	}

	@Override
	public void pushActive(boolean active) {
		mActiveStack.add(mActive);
		mActive &= active;
	}

	@Override
	public void popActive() {
		if (mActiveStack.isEmpty())
			throw new IllegalStateException("active stack is empty");
		mActive = pop(mActiveStack);
	}

	@Override
	public void pushActive(String widgetId) {
		boolean value = mActive;
		if (value) {
			value = mOriginalStepper.getOptions().getBooleanValue(widgetId);
		}
		pushActive(value);
	}

	@Override
	public boolean step() {
		return stepAux();
	}

	@Override
	public boolean bigStep() {
		return stepAux();
	}

	private boolean stepAux() {
		if (isActive()) {
			mCurrentStep++;
		}
		return false;
	}

	@Override
	public void show(String message) {
		throw new DesiredStepReachedException();
	}

	@Override
	public boolean openLayer(String key) {
		return isActive();
	}

	public int countSteps() {
		mActive = true;
		mCurrentStep = 0;
		mActiveStack.clear();
		try {
			Algorithm algorithm = mOriginalStepper.getOptions()
					.getActiveAlgorithm();
			algorithm.run(this);
			// We completed the algorithm without halting.
			// Increment the step, since we would normally show a 'done'
			// milestone at this point
			mCurrentStep++;
		} catch (RuntimeException t) {
			warning("TotalStepsCounter caught: "+t);
		}
		return mCurrentStep;
	}

	private static class DesiredStepReachedException extends RuntimeException {
	}

	private ConcreteStepper mOriginalStepper;
	private boolean mActive;
	private ArrayList<Boolean> mActiveStack = new ArrayList();
	private int mCurrentStep;
}
