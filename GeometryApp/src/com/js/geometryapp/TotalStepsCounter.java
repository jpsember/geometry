package com.js.geometryapp;

import static com.js.basic.Tools.*;

import com.js.geometry.AlgorithmStepper;
import com.js.geometry.Rect;

class TotalStepsCounter extends AlgorithmStepper {

	TotalStepsCounter(AlgorithmStepper original, AlgorithmOptions options) {
		mOriginalStepper = original;
		mOptions = options;
	}

	@Override
	public Rect algorithmRect() {
		return mOriginalStepper.algorithmRect();
	}

	@Override
	public void pushActive(String widgetId) {
		boolean value = isActive();
		if (value) {
			value = mOptions.getBooleanValue(widgetId);
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

	public int countSteps(AlgorithmInput input) {
		initializeActiveState(true);
		mCurrentStep = 0;
		try {
			Algorithm algorithm = mOptions.getActiveAlgorithm();
			algorithm.run(this, input);
			// We completed the algorithm without halting.
			// Increment the step, since we would normally show a 'done'
			// milestone at this point
			mCurrentStep++;
		} catch (RuntimeException t) {
			warning("TotalStepsCounter caught: " + t);
		}
		return mCurrentStep;
	}

	private static class DesiredStepReachedException extends RuntimeException {
	}

	private AlgorithmStepper mOriginalStepper;
	private int mCurrentStep;
	private AlgorithmOptions mOptions;
}
