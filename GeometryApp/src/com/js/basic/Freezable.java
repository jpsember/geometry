package com.js.basic;

/**
 * <pre>
 * 
 * Interface representing objects that can be frozen to an immutable state
 * 
 * </pre>
 */
public interface Freezable {

  /**
   * Get a copy of this object; if already frozen, returns this
   */
  public Freezable getCopy();

  /**
   * Get a mutable copy of this object
   */
  public Freezable getMutableCopy();

  /**
   * Get a frozen copy of this object; if already frozen, returns this
   */
  public Freezable getFrozenCopy();

  /**
   * Make this object frozen (if not already)
   */
  public void freeze();

  /**
   * Determine if this object is frozen
   */
  public boolean isFrozen();

  /**
   * Prepare for mutating this object; should throw IllegalMutationException if
   * object is frozen
   */
  public void mutate();

  public class IllegalMutationException extends UnsupportedOperationException {
  }

  /**
   * Concrete implementation of the Freezable interface, for objects that can be
   * mutable
   */
  public abstract class Mutable implements Freezable {

    @Override
    public abstract Freezable getMutableCopy();

    @Override
    public Freezable getFrozenCopy() {
      if (mFrozen)
        return this;
      Freezable copy = getMutableCopy();
      copy.freeze();
      return copy;
    }

    @Override
    public void freeze() {
      if (!mFrozen) {
        mFrozen = true;
      }
    }

    @Override
    public boolean isFrozen() {
      return mFrozen;
    }

    @Override
    public Freezable getCopy() {
      if (isFrozen())
        return this;
      return getMutableCopy();
    }

    @Override
    public void mutate() {
      if (isFrozen())
        throw new IllegalMutationException();
    }

    public boolean isMutable() {
      return !mFrozen;
    }

    private boolean mFrozen;
  }
}
