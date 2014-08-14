package com.js.basicTest;

import java.io.PrintStream;

import com.js.testUtils.*;

import static com.js.basic.Tools.*;

public class StringPrintStreamTest extends MyTest {

	public void testInterceptor() {

		StringPrintStream icept = StringPrintStream.build();
		PrintStream originalOut = System.out;
		System.setOut(icept);

		pr("aaa");
		System.out.print(15);
		pr("bbb");
		System.setOut(originalOut);

		String content = icept.content();
		assertStringsMatch("aaa\n15bbb\n", content);
	}

}
