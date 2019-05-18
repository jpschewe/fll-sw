/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.Serializable;

import javax.annotation.Nonnull;

/**
 * Information about how many teams a judge has seen. Used for display when
 * summarizing scores.
 */
public final class JudgeSummary implements Serializable, Comparable<JudgeSummary> {

  private final String mJudge;

  private final String mCategory;

  private final String mGroup;

  private final int mNumExpected;

  private final int mNumActual;

  /**
   * @return the judge, may be null
   */
  public String getJudge() {
    return mJudge;
  }

  /**
   * @return the score category title
   */
  @Nonnull
  public String getCategory() {
    return mCategory;
  }

  /**
   * @return the judging group name
   */
  @Nonnull
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

  public JudgeSummary(final String judge,
                      @Nonnull final String category,
                      @Nonnull final String group,
                      final int numExpected,
                      final int numActual) {
    mJudge = judge;
    mCategory = category;
    mGroup = group;
    mNumExpected = numExpected;
    mNumActual = numActual;
  }

  @Override
  public boolean equals(final Object o) {
    if (getClass().equals(o.getClass())) {
      return compareTo((JudgeSummary) o) == 0;
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(final JudgeSummary o) {
    if (getGroup().equals(o.getGroup())) {
      if (getCategory().equals(o.getCategory())) {
        if (null == getJudge()) {
          if (null == o.getJudge()) {
            return 0;
          } else {
            // multiply by -1 to invert the result so that it's from the perspective of this
            // rather than o.
            return o.getJudge().compareTo(getJudge())
                * -1;
          }
        } else {
          return getJudge().compareTo(o.getJudge());
        }
      } else {
        return getCategory().compareTo(o.getCategory());
      }
    } else {
      return getGroup().compareTo(o.getGroup());
    }
  }

}
