/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * A reference to a goal.
 */
public class GoalRef implements Evaluatable, Serializable {

  public static final String TAG_NAME = "goalRef";

  public static final String SCORE_TYPE_ATTRIBUTE = "scoreType";

  public static final String GOAL_ATTRIBUTE = "goal";

  public GoalRef(@Nonnull final Element ele,
                 @Nonnull final GoalScope scope) {
    mScoreType = GoalScoreType.fromString(ele.getAttribute(SCORE_TYPE_ATTRIBUTE));

    mGoalScope = scope;
    mGoalName = Objects.requireNonNull(ele.getAttribute(GOAL_ATTRIBUTE));
  }

  /**
   * @param goalName see {@link #getGoalName()}
   * @param scope the scope to use to find the goal at evaluation time
   * @param scoreType see {@link #getScoreType()}
   */
  public GoalRef(@Nonnull final String goalName,
                 @Nonnull final GoalScope scope,
                 @Nonnull final GoalScoreType scoreType) {
    mScoreType = scoreType;
    mGoalScope = scope;
    mGoalName = goalName;
  }

  private String mGoalName;

  /**
   * @return the name of the goal referenced
   */
  @Nonnull
  public String getGoalName() {
    return mGoalName;
  }

  /**
   * @param v see {@link #getGoalName()}
   */
  public void setGoalName(@Nonnull final String v) {
    mGoalName = v;
  }

  private final GoalScope mGoalScope;

  /**
   * @return the scope used to lookup goals
   */
  @Nonnull
  public GoalScope getGoalScope() {
    return mGoalScope;
  }

  /**
   * Resolve the goal name against the goal scope
   * 
   * @return the goal
   * @throws ScopeException if the goal cannot be found
   */
  @Nonnull
  public AbstractGoal getGoal() {
    return mGoalScope.getGoal(mGoalName);
  }

  private GoalScoreType mScoreType;

  /**
   * @return how the goal value should be interpreted
   */
  @Nonnull
  public GoalScoreType getScoreType() {
    return mScoreType;
  }

  /**
   * @param v see {@link #getScoreType()}
   */
  public void setScoreType(@Nonnull final GoalScoreType v) {
    mScoreType = v;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    double value;
    switch (getScoreType()) {
    case COMPUTED:
      value = getGoal().getComputedScore(teamScore);
      break;
    case RAW:
      value = getGoal().getRawScore(teamScore);
      break;
    default:
      throw new FLLInternalException("Unknown score type: "
          + getScoreType());
    }

    return value;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(SCORE_TYPE_ATTRIBUTE, mScoreType.toXmlString());
    ele.setAttribute(GOAL_ATTRIBUTE, mGoalName);
    return ele;
  }
}
