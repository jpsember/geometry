package com.js.geometryapp;

import com.js.geometry.AlgorithmStepper;

public interface Algorithm {

	/**
	 * Get name of the algorithm. This will be displayed in the 'Operation:'
	 * combobox
	 */
	public String getAlgorithmName();

	/**
	 * Add widgets for user to customize this algorithm's operation
	 */
	public void prepareOptions(AlgorithmOptions options);

	/**
	 * Prepare inputs for the algorithm
	 * 
	 * @param input
	 *            a structure containing geometric objects generated by the
	 *            editor
	 */
	public void prepareInput(AlgorithmInput input);

	/**
	 * Execute the algorithm
	 * 
	 * @param stepper
	 */
	public void run(AlgorithmStepper stepper);
}
