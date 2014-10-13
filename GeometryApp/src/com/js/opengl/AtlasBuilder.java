package com.js.opengl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.js.geometry.Point;
import com.js.geometry.Rect;

public class AtlasBuilder {

	/**
	 * Constructor
	 * 
	 * @param size
	 *            size of atlas, in pixels
	 */
	public AtlasBuilder(Point size) {
		setAtlasSize(size);
	}

	/**
	 * Set size of atlas
	 * 
	 * @param size
	 *            size of atlas, in pixels
	 */
	public void setAtlasSize(Point size) {
		mAtlasSize = size;
	}

	/**
	 * Set some padding to enforce. This amount of padding will be inserted
	 * between the atlas boundaries and the rectangles, as well as between pairs
	 * of rectangles
	 * 
	 * @param padding
	 *            number of pixels
	 */
	public void setPadding(int padding) {
		mPadding = padding;
	}

	/**
	 * Add a rectangle
	 */
	public void add(Rect rectangle) {
		mUserRects.add(new RectEnt(mUserRects.size(), rectangle));
	}

	/**
	 * Get number of rectangles in builder
	 */
	public int size() {
		return mUserRects.size();
	}

	/**
	 * Get rectangle
	 * 
	 * @param index
	 *            index of rectangle
	 */
	public Rect get(int index) {
		return mUserRects.get(index).mRect;
	}

	/**
	 * Attempt to arrange the rectangles within the atlas so they don't overlap,
	 * and satisfy the padding constraints
	 * 
	 * @return true if building was successful
	 */
	public boolean build() {
		List<RectEnt> workEnts = new ArrayList(mUserRects);
		sort(workEnts);
		for (RectEnt ent : workEnts) {
			Rect mr = ent.mRect;
			Point size = new Point(mr.width, mr.height);
			Rect r = findSpaceFor(size);
			if (r == null)
				return false;
			mr.x = r.x;
			mr.y = r.y;
			mEnts.add(r);
		}
		return true;
	}

	private int sortValue(Rect r) {
		return -(int) (r.width * r.height);
	}

	private void sort(List<RectEnt> rectEntList) {
		Collections.sort(rectEntList, new Comparator() {
			public int compare(Object e1, Object e2) {
				RectEnt ent1 = (RectEnt) e1;
				RectEnt ent2 = (RectEnt) e2;
				int s1 = sortValue(ent1.mRect);
				int s2 = sortValue(ent2.mRect);
				int val = s1 - s2;
				if (val == 0)
					val = ent1.mId - ent2.mId;
				return val;
			}
		});
	}

	/**
	 * Find space for a rectangle within texture
	 * 
	 * @return rectangle, or null if no space found
	 */
	private Rect findSpaceFor(Point rectSize) {
		float minX = mPadding;
		float minY = mPadding;
		float maxX = mAtlasSize.x;
		float maxY = mAtlasSize.y;

		Rect ri = new Rect(minX, minY, rectSize.x + mPadding, rectSize.y
				+ mPadding);

		float nextY = maxY;

		while (true) {
			Rect collidingRect = null;
			for (Rect s : mEnts) {
				if (!ri.intersects(s))
					continue;
				collidingRect = s;
				break;
			}

			if (collidingRect == null)
				break;

			ri.x = collidingRect.endX();
			nextY = Math.min(nextY, collidingRect.endY());
			if (ri.endX() > maxX) {
				ri.x = minX;
				ri.y = nextY;
				nextY = maxY;
			}
		}
		if (ri.endX() > maxX || ri.endY() > maxY)
			return null;
		return ri;
	}

	private static class RectEnt {
		public RectEnt(int id, Rect rect) {
			mRect = rect;
			mId = id;
		}

		int mId;
		Rect mRect;
	}

	private List<RectEnt> mUserRects = new ArrayList();
	private List<Rect> mEnts = new ArrayList();
	private Point mAtlasSize;
	private int mPadding;
}
