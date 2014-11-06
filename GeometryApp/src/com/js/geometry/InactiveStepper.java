package com.js.geometry;

/**
 * An AlgorithmStepper implementation that is never active
 */
public class InactiveStepper extends AlgorithmStepper {

	public InactiveStepper() {
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public void pushActive(boolean active) {
	}

	@Override
	public void popActive() {
	}

	@Override
	public void pushActive(String widgetId) {
	}

}
