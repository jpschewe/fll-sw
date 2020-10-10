/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Objects;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * A reference to a goal.
 */
public class GoalRef implements Evaluatable, Serializable, StringValue {

  /**
   * XML tag name used for this class.
   */
  public static final String TAG_NAME = "goalRef";

  private static final String SCORE_TYPE_ATTRIBUTE = "scoreType";

  private static final String GOAL_ATTRIBUTE = "goal";

  /**
   * @param ele the element to parse
   * @param scope {@link #getGoalScope()}
   */
  public GoalRef(@NonNull final Element ele,
                 @NonNull final @UnderInitialization GoalScope scope) {
    mScoreType = GoalScoreType.fromString(ele.getAttribute(SCORE_TYPE_ATTRIBUTE));

    mGoalScope = scope;
    mGoalName = Objects.requireNonNull(ele.getAttribute(GOAL_ATTRIBUTE));
  }

  /**
   * @param goalName see {@link #getGoalName()}
   * @param scope the scope to use to find the goal at evaluation time
   * @param scoreType see {@link #getScoreType()}
   */
  public GoalRef(@NonNull final String goalName,
                 @NonNull final @UnderInitialization GoalScope scope,
                 @NonNull final GoalScoreType scoreType) {
    mScoreType = scoreType;
    mGoalScope = scope;
    mGoalName = goalName;
  }

  private String mGoalName;

  /**
   * @return the name of the goal referenced
   */
  @NonNull
  public String getGoalName() {
    return mGoalName;
  }

  /**
   * @param v see {@link #getGoalName()}
   */
  public void setGoalName(@NonNull final String v) {
    mGoalName = v;
  }

  private final GoalScope mGoalScope;

  /**
   * @return the scope used to lookup goals
   */
  @NonNull
  public GoalScope getGoalScope() {
    return mGoalScope;
  }

  /**
   * Resolve the goal name against the goal scope.
   * 
   * @return the goal
   * @throws ScopeException if the goal cannot be found
   */
  @NonNull
  public AbstractGoal getGoal() {
    return mGoalScope.getGoal(mGoalName);
  }

  private GoalScoreType mScoreType;

  /**
   * @return how the goal value should be interpreted
   */
  @NonNull
  public GoalScoreType getScoreType() {
    return mScoreType;
  }

  /**
   * @param v see {@link #getScoreType()}
   */
  public void setScoreType(@NonNull final GoalScoreType v) {
    mScoreType = v;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    double value;
    switch (getScoreType()) {
    case COMPUTED:
      value = getGoal().evaluate(teamScore);
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

  /**
   * @param doc used to creates elements
   * @return XML representation of this object
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(SCORE_TYPE_ATTRIBUTE, mScoreType.toXmlString());
    ele.setAttribute(GOAL_ATTRIBUTE, mGoalName);
    return ele;
  }

  @Override
  public boolean isGoalRef() {
    return true;
  }

  @Override
  public boolean isStringConstant() {
    return false;
  }

  // StringValue interface
  @Override
  public String getStringValue(final TeamScore score) {
    return score.getEnumRawScore(getGoalName());
  }

  @Override
  public String getRawStringValue() {
    return getGoalName();
  }
  // end StringValue interface
}
