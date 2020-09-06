/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Common interface for {@link CaseStatement#getResult()}.
 */
public interface CaseStatementResult extends Evaluatable {

  /**
   * @return score type for the result
   */
  ScoreType getScoreType();

}
