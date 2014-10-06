package com.js.basicTest;

import java.util.ArrayList;

import com.js.basic.Queue;
import com.js.testUtils.IOSnapshot;
import com.js.testUtils.MyTestCase;
import static com.js.basic.Tools.*;

public class QueueTest extends MyTestCase {

	@Override
	protected void setUp() {
		super.setUp();
		mQueue = new Queue();
	}

	private static String buildElement(int characterNumber) {
		return Character.toString((char) ('A' + characterNumber));
	}

	private void populateQueue() {
		for (int i = 0; i < 5; i++) {
			mQueue.push(buildElement(i));
		}
	}

	void testConstructor() {
		assertTrue(mQueue.isEmpty());
	}

	void testWrap() {
		IOSnapshot.open();

		populateQueue();

		for (int i = 0; i < 100; i++) {
			String n = mQueue.pop();
			mQueue.push(n);
			pr(mQueue);
		}
		while (!mQueue.isEmpty()) {
			mQueue.pop();
			pr(mQueue);
		}
		IOSnapshot.close();
	}

	void testGrow() {
		IOSnapshot.open();
		for (int i = 0; i < 20; i++) {
			mQueue.push(buildElement(i));
			pr(mQueue);
		}
		while (!mQueue.isEmpty()) {
			mQueue.pop();
			pr(mQueue);
		}
		IOSnapshot.close();
	}

	public void testPeek() {
		int reps = 100;
		int maxSize = 70;

		Queue<Integer> q = new Queue();
		for (int i = 0; i < reps; i++) {
			q.push(i);
			if (q.size() > maxSize)
				q.pop();
		}
		for (int j = 0; j < maxSize; j++) {
			int v = q.peek(true, j);
			assertEquals(j + (reps - maxSize), v);
			v = q.peek(false, j);
			assertEquals(reps - 1 - j, v);
		}
	}

	public void testPushAndPop() {
		int nIter = 120;
		IOSnapshot.open();
		Queue<Integer> q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(d(i, 3) + ": ");
			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						int v = q.pop();
						pr("popped " + d(v, 3) + "; " + q);
						continue;
					}
				}
				q.push(i);
				pr("pushed " + d(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				int v = q.pop();
				pr("popped " + d(v, 3) + "; " + q);
			}
		}
		IOSnapshot.close();
	}

	public void testPushAndPopRear() {
		int nIter = 120;
		IOSnapshot.open();
		Queue<Integer> q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(d(i, 3) + ": ");

			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						int v = q.pop(false);
						pr("popped " + d(v, 3) + "; " + q);
						continue;
					}
				}
				q.push(i);
				pr("pushed " + d(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				int v = q.pop();
				pr("popped " + d(v, 3) + "; " + q);
			}
		}
		IOSnapshot.close();
	}

	public void testPushFrontAndPopRear() {
		int nIter = 120;
		IOSnapshot.open();
		Queue<Integer> q = new Queue();
		for (int i = 0; i < nIter; i++) {
			prr(d(i, 3) + ": ");

			if (i < nIter / 2) {
				if (random().nextInt(80) > 60) {
					if (!q.isEmpty()) {
						int v = q.pop(false);
						pr("popped " + d(v, 3) + "; " + q);
						continue;
					}
				}
				q.push(i, true);
				pr("pushed " + d(i, 3) + "; " + q);

			} else {
				if (q.isEmpty()) {
					pr("queue empty, stopping");
					break;
				}
				int v = q.pop(false);
				pr("popped " + d(v, 3) + "; " + q);
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
		Queue<String> q = new Queue(a);
		for (int i = 0; i < 4; i++)
			q.pop();
		try {
			q.pop();
			fail();
		} catch (Throwable t) {
		}
	}

	public void testPushToRearByDefault() {
		populateQueue();
		mQueue.push("zz");
		assertStringsMatch("zz", mQueue.peek(false));
	}

	public void testPopFromFrontByDefault() {
		populateQueue();
		assertStringsMatch("A", mQueue.pop());
	}

	public void testPeekAtFrontByDefault() {
		populateQueue();
		assertStringsMatch("A", mQueue.peek());
	}

	private Queue<String> mQueue;

}
