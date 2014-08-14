package com.js.basicTest;

import com.js.testUtils.*;

public class IOSnapshotTest extends MyTest {

	public void testStdOut() {
		IOSnapshot.open();
		System.out.println("This is printed to System.out");
		IOSnapshot.close();
	}
	
	public void testStdErr() {
		IOSnapshot.open();
		System.out.println("This is printed to System.out");
		System.err.println("This is printed to System.err");
		IOSnapshot.close();
	}

	public void testStdErrOnly() {
		IOSnapshot.open();
		System.err.println("This is printed to System.err");
		IOSnapshot.close();
	}

}
