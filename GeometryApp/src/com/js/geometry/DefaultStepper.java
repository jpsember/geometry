package com.js.geometry;

/**
 * An AlgorithmStepper implementation that does nothing
 */
public class DefaultStepper extends AlgorithmStepper {

	public DefaultStepper() {
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
