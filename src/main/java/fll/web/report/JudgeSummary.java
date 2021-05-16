/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.Serializable;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Information about how many teams a judge has seen. Used for display when
 * summarizing scores.
 */
public final class JudgeSummary implements Serializable, Comparable<JudgeSummary> {

  private final @Nullable String mJudge;

  private final String mCategory;

  private final String mGroup;

  private final int mNumExpected;

  private final int mNumActual;

  /**
   * @return the judge, may be null
   */
  public @Nullable String getJudge() {
    return mJudge;
  }

  /**
   * @return the score category title
   */
  public String getCategory() {
    return mCategory;
  }

  /**
   * @return the judging group name
   */
  public String getGroup() {
    return mGroup;
  }

  /**
   * @return the number of scores expected
   */
  public int getNumExpected() {
    return mNumExpected;
  }

  /**
   * @return the number of scores seen
   */
  public int getNumActual() {
    return mNumActual;
  }

  /**
   * @param judge {@link #getJudge()}
   * @param category {@link #getCategory()}
   * @param group {@link #getGroup()}
   * @param numExpected {@link #getNumExpected()}
   * @param numActual {@link #getNumActual()}
   */
  public JudgeSummary(final @Nullable String judge,
                      final String category,
                      final String group,
                      final int numExpected,
                      final int numActual) {
    mJudge = judge;
    mCategory = category;
    mGroup = group;
    mNumExpected = numExpected;
    mNumActual = numActual;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getGroup(), getCategory(), getJudge());
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (getClass().equals(o.getClass())) {
      return compareTo((JudgeSummary) o) == 0;
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(final JudgeSummary o) {
    if (getGroup().equals(o.getGroup())) {
      if (getCategory().equals(o.getCategory())) {
        final String thisJudge = getJudge();
        final String otherJudge = o.getJudge();
        if (null == thisJudge) {
          if (null == otherJudge) {
            return 0;
          } else {
            return 1;
          }
        } else if (null == otherJudge) {
          return -1;
        } else {
          return thisJudge.compareTo(otherJudge);
        }
      } else {
        return getCategory().compareTo(o.getCategory());
      }
    } else {
      return getGroup().compareTo(o.getGroup());
    }
  }

}
