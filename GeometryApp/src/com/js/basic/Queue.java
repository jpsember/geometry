package com.js.basic;

import java.util.ArrayList;
import static com.js.basic.Tools.*;

public class Queue {

	private static final Object NULL_OBJECT = Boolean.FALSE;

	/**
	 * By default, items are pushed to the REAR of the queue, and popped from
	 * the FRONT. Also by default, peeking occurs relative to the FRONT of the
	 * queue.
	 */

	public static Queue queue() {
		return queueWithCapacity(16);
	}

	public static Queue queueWithArray(ArrayList array) {
		Queue q = queueWithCapacity(array.size());
		for (Object item : array) {
			q.push(item);
		}
		return q;
	}

	public static Queue queueWithCapacity(int capacity) {
		return new Queue(capacity);
	}

	public Queue() {
		this(16);
	}

	public Queue(int capacity) {
		mArray = construct(1 + capacity);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public int size() {
		int n = mTail - mHead;
		int c = mArray.size();
		if (n < 0)
			n += c;
		return n;
	}

	private int spaceRemaining() {
		return mArray.size() - size();
	}

	public void push(Object item) {
		push(item, false);
	}

	public void push(Object item, boolean toFront) {
		if (spaceRemaining() <= 1) {
			expandBuffer();
		}
		if (!toFront) {
			mArray.set(mTail, item);
			mTail++;
			if (mTail == mArray.size()) {
				mTail = 0;
			}
		} else {
			if (mHead == 0)
				mHead = mArray.size();
			mHead--;
			mArray.set(mHead, item);
		}
	}

	public Object peek(boolean atFront) {
		return peek(atFront, 0);
	}

	public Object peek(boolean atFront, int distance) {
		int count = size();
		if (distance >= count)
			throw new IllegalArgumentException("queue range error");
		if (!atFront) {
			distance = count - 1 - distance;
		}
		return mArray.get(calcPos(distance));
	}

	public Object peek() {
		return peek(true);
	}

	public Object pop(boolean fromFront) {
		Object ret;
		if (size() == 0)
			throw new IllegalStateException("pop of empty queue");
		if (!fromFront) {
			if (mTail-- == 0)
				mTail = mArray.size() - 1;
			ret = mArray.get(mTail);
			mArray.set(mTail, NULL_OBJECT);
		} else {
			ret = mArray.get(mHead);
			mArray.set(mHead, NULL_OBJECT);
			if (++mHead == mArray.size())
				mHead = 0;
		}
		return ret;
	}

	public void clear() {
		while (mHead != mTail)
			pop();
	}

	public Object pop() {
		return pop(true);
	}

	private ArrayList construct(int capacity) {
		ArrayList a = new ArrayList(capacity);
		while (capacity-- > 0) {
			a.add(NULL_OBJECT);
		}
		return a;
	}

	private void expandBuffer() {
		if (db)
			pr("expanding buffer from size " + mArray.size() + "; currently "
				+ this);
		ArrayList a2 = construct(mArray.size() * 2);

		for (int i = 0, j = mHead; j != mTail; i++) {
			a2.set(i, mArray.get(j));
			if (++j == mArray.size())
				j = 0;
		}
		mTail = size();
		mHead = 0;
		mArray = a2;
		if (db)
			pr(" head " + mHead + " tail " + mTail + " now: " + this);
	}

	private int calcPos(int fromStart) {
		int k = mHead + fromStart;
		if (k >= mArray.size())
			k -= mArray.size();
		return k;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < size(); i++) {
			Object obj = peek(true, i);
			sb.append(" ");
			sb.append(obj);
		}
		sb.append(" ]");
		return sb.toString();
	}

	private ArrayList mArray;
	private int mHead, mTail;
}
