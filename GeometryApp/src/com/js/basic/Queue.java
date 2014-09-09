package com.js.basic;

import java.util.Collection;

import static com.js.basic.Tools.*;

public class Queue<T> {

	/**
	 * By default, items are pushed to the REAR of the queue, and popped from
	 * the FRONT. Also by default, peeking occurs relative to the FRONT of the
	 * queue.
	 */

	public Queue(int capacity) {
		mArray = construct(1 + capacity);
	}

	public Queue() {
		this(16);
	}

	public Queue(Collection<T> array) {
		this(array.size());
		for (T item : array) {
			push(item);
		}
	}


	public boolean isEmpty() {
		return size() == 0;
	}

	public int size() {
		int n = mTail - mHead;
		int c = mArray.length;
		if (n < 0)
			n += c;
		return n;
	}

	private int spaceRemaining() {
		return mArray.length - size();
	}

	public void push(T item) {
		push(item, false);
	}

	public void push(T item, boolean toFront) {
		if (spaceRemaining() <= 1) {
			expandBuffer();
		}
		if (!toFront) {
			mArray[mTail] = item;
			mTail++;
			if (mTail == mArray.length) {
				mTail = 0;
			}
		} else {
			if (mHead == 0)
				mHead = mArray.length;
			mHead--;
			mArray[mHead] = item;
		}
	}

	public T peek(boolean atFront) {
		return peek(atFront, 0);
	}

	public T peek(boolean atFront, int distance) {
		int count = size();
		if (distance >= count)
			throw new IllegalArgumentException("queue range error");
		if (!atFront) {
			distance = count - 1 - distance;
		}
		return (T) mArray[calcPos(distance)];
	}

	public T peek(int distance) {
		return peek(true, distance);
	}

	public T peek() {
		return peek(true);
	}

	public T pop(boolean fromFront) {
		T ret;
		if (size() == 0)
			throw new IllegalStateException("pop of empty queue");
		if (!fromFront) {
			if (mTail-- == 0)
				mTail = mArray.length - 1;
			ret = (T) mArray[mTail];
			mArray[mTail] = null;
		} else {
			ret = (T) mArray[mHead];
			mArray[mHead] = null;
			if (++mHead == mArray.length)
				mHead = 0;
		}
		return ret;
	}

	public void clear() {
		while (mHead != mTail)
			pop();
	}

	public T pop() {
		return pop(true);
	}

	private Object[] construct(int capacity) {
		return new Object[capacity];
	}

	private void expandBuffer() {
		if (db)
			pr("expanding buffer from size " + mArray.length + "; currently "
				+ this);
		Object a2[] = construct(mArray.length * 2);

		for (int i = 0, j = mHead; j != mTail; i++) {
			a2[i] = mArray[j];
			if (++j == mArray.length)
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
		if (k >= mArray.length)
			k -= mArray.length;
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

	private Object[] mArray;
	private int mHead, mTail;
}
