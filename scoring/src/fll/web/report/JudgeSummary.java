/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.Serializable;

/**
 * Information about how many teams a judge has seen. Used for display when
 * summarizing scores.
 */
public final class JudgeSummary implements Serializable {

  private final String mJudge;

  private final String mCategory;

  private final String mGroup;

  private final int mNumExpected;

  private final int mNumActual;

  public String getJudge() {
    return mJudge;
  }

  public String getCategory() {
    return mCategory;
  }

  public String getGroup() {
    return mGroup;
  }

  public int getNumExpected() {
    return mNumExpected;
  }

  public int getNumActual() {
    return mNumActual;
  }

  public JudgeSummary(final String judge,
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

}
