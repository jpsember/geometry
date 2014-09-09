package com.js.basicTest;

import java.util.ArrayList;

import com.js.basic.Queue;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class QueueTest extends MyTestCase {

	private static String s(int charVal) {
		return Character.toString((char) charVal);
	}

	private Queue queue;

	@Override
	protected void setUp() {
		super.setUp();
		ArrayList a = new ArrayList();
		for (int i = 0; i < 5; i++)
			a.add(s('a' + i));
		queue = Queue.queueWithArray(a);
	}

	void testConstructor() {
		Queue q = Queue.queue();
		assertTrue(q.isEmpty());
	}

	void testWrap() {
		IOSnapshot.open();
		Queue q = new Queue();

		for (int i = 0; i < 5; i++) {
			q.push(s('A' + i));
		}

		for (int i = 0; i < 100; i++) {
			String n = (String) q.pop();
			q.push(n);
			pr(q);
		}
		while (!q.isEmpty()) {
			q.pop();
			pr(q);
		}
		IOSnapshot.close();
	}

	void testGrow() {
		IOSnapshot.open();
		Queue q = new Queue();
		for (int i = 0; i < 20; i++) {
			q.push(s('A' + i));
			pr(q);
		}
		while (!q.isEmpty()) {
			q.pop();
			pr(q);
		}
		IOSnapshot.close();
	}

	public void testPeek() {
		int reps = 100;
		int maxSize = 70;

		Queue q = new Queue();
		for (int i = 0; i < reps; i++) {
			q.push(i);
			if (q.size() > maxSize)
				q.pop();
		}
		for (int j = 0; j < maxSize; j++) {
			Integer v = (Integer) q.peek(true, j);
			assertEquals(j + (reps - maxSize), v.intValue());
			v = (Integer) q.peek(false, j);
			assertEquals(reps - 1 - j, v.intValue());
		}
	}

	public void testPushAndPop() {
		int nIter = 120;
		IOSnapshot.open();
		Queue q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(f(i, 3) + ": ");
			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						Integer v = (Integer) q.pop();
						pr("popped " + f(v.intValue(), 3) + "; " + q);
						continue;
					}
				}
				q.push(i);
				pr("pushed " + f(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				Integer v = (Integer) q.pop();
				pr("popped " + f(v.intValue(), 3) + "; " + q);
			}
		}
		IOSnapshot.close();
	}

	public void testPushAndPopRear() {
		int nIter = 120;
		IOSnapshot.open();
		Queue q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(f(i, 3) + ": ");

			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						Integer v = (Integer) q.pop(false);
						pr("popped " + f(v.intValue(), 3) + "; " + q);
						continue;
					}
				}
				q.push(i);
				pr("pushed " + f(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				Integer v = (Integer) q.pop();
				pr("popped " + f(v.intValue(), 3) + "; " + q);
			}
		}
		IOSnapshot.close();
	}

	public void testPushFrontAndPopRear() {
		int nIter = 120;
		IOSnapshot.open();
		Queue q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(f(i, 3) + ": ");

			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						Integer v = (Integer) q.pop(false);
						pr("popped " + f(v.intValue(), 3) + "; " + q);
						continue;
					}
				}
				q.push(i, true);
				pr("pushed " + f(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				Integer v = (Integer) q.pop(false);
				pr("popped " + f(v.intValue(), 3) + "; " + q);
			}
		}
		IOSnapshot.close();
	}

	public void testPopEmpty() {
		ArrayList a = new ArrayList();
		a.add("a");
		a.add("b");
		a.add("c");
		a.add("d");
		Queue q = Queue.queueWithArray(a);
		for (int i = 0; i < 4; i++)
			q.pop();
		try {
			q.pop();
			fail();
		} catch (Throwable t) {
		}
	}

	public void testPushToRearByDefault() {
		Queue q = queue;
		q.push("zz");
		assertStringsMatch("zz", q.peek(false));
	}

	public void testPopFromFrontByDefault() {
		Queue q = queue;
		assertStringsMatch("a", q.pop());
	}

	public void testPeekAtFrontByDefault() {
		Queue q = queue;
		assertStringsMatch("a", q.peek());
	}

}
