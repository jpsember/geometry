package com.js.geometryapp;

public interface Algorithm {

	public String getAlgorithmName();

	public void prepareOptions(AlgorithmOptions options);

	public void run(AlgorithmStepper stepper);
}
