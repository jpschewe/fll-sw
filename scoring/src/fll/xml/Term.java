/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

public class Term extends AbstractTerm {

  public Term(final Element ele,
              final GoalScope scope) {
    super(ele);
    mScoreType = GoalScoreType.fromString(ele.getAttribute("scoreType"));

    final String goalName = ele.getAttribute("goal");
    mGoal = scope.getGoal(goalName);
  }

  private final AbstractGoal mGoal;

  public AbstractGoal getGoal() {
    return mGoal;
  }

  private final GoalScoreType mScoreType;

  public GoalScoreType getScoreType() {
    return mScoreType;
  }

}
