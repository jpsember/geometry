package com.js.testUtils;

import java.util.ArrayList;
import java.util.Random;

public class MyTestUtils {

	public static void permute(Random random, ArrayList array) {
		for (int i = 0; i < array.size(); i++) {
			int j = i + random.nextInt(array.size() - i);
			Object tmp = array.get(i);
			array.set(i, array.get(j));
			array.set(j, tmp);
		}
	}

}
