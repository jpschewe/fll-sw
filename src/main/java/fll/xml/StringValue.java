/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import fll.web.playoff.TeamScore;

/**
 * Interface used for {@link EnumConditionStatement#getLeft()} and
 * {@link EnumConditionStatement#getRight()}.
 */
public interface StringValue extends Serializable {

  /**
   * The value to be used when evaluating a score.
   * 
   * @param score used to get the string value if a goal reference
   * @return the string value
   */
  String getStringValue(TeamScore score);

  /**
   * The string to store.
   * 
   * @return the raw string value
   */
  String getRawStringValue();

  /**
   * @return true if an instance of {@link GoalRef}
   */
  boolean isGoalRef();

  /**
   * @return true if an instance of {@link StringConstant}
   */
  boolean isStringConstant();

}
