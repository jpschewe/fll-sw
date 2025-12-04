/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.scores.TeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;

/**
 * A reference to a goal.
 */
public class GoalRef implements Evaluatable<TeamScore>, Serializable, StringValue {

  /**
   * XML tag name used for this class.
   */
  public static final String TAG_NAME = "goalRef";

  private static final String SCORE_TYPE_ATTRIBUTE = "scoreType";

  /**
   * Attribute for the goal referenced.
   */
  public static final String GOAL_ATTRIBUTE = "goal";

  /**
   * @param ele the element to parse
   * @param scope {@link #getGoalScope()}
   */
  public GoalRef(final Element ele,
                 final @UnknownInitialization GoalScope scope) {
    mScoreType = GoalScoreType.fromString(ele.getAttribute(SCORE_TYPE_ATTRIBUTE));

    mGoalScope = scope;
    mGoalName = ele.getAttribute(GOAL_ATTRIBUTE);
  }

  /**
   * @param goalName see {@link #getGoalName()}
   * @param scope the scope to use to find the goal at evaluation time
   * @param scoreType see {@link #getScoreType()}
   */
  public GoalRef(final String goalName,
                 final @UnknownInitialization GoalScope scope,
                 final GoalScoreType scoreType) {
    mScoreType = scoreType;
    mGoalScope = scope;
    mGoalName = goalName;
  }

  private String mGoalName;

  /**
   * @return the name of the goal referenced
   */

  public String getGoalName() {
    return mGoalName;
  }

  /**
   * @param v see {@link #getGoalName()}
   */
  public void setGoalName(final String v) {
    mGoalName = v;
  }

  private final @NotOnlyInitialized GoalScope mGoalScope;

  /**
   * @return the scope used to lookup goals
   */

  public GoalScope getGoalScope() {
    return mGoalScope;
  }

  /**
   * Resolve the goal name against the goal scope.
   * 
   * @return the goal
   * @throws ScopeException if the goal cannot be found
   */
  public AbstractGoal getGoal() throws ScopeException {
    return mGoalScope.getGoal(mGoalName);
  }

  private GoalScoreType mScoreType;

  /**
   * @return how the goal value should be interpreted
   */

  public GoalScoreType getScoreType() {
    return mScoreType;
  }

  /**
   * @param v see {@link #getScoreType()}
   */
  public void setScoreType(final GoalScoreType v) {
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
   * @param doc used to create elements
   * @return XML representation of this object
   */
  public Element toXml(final Document doc) {
    return internalToXml(doc, TAG_NAME);
  }

  /**
   * Subclasses should call this method to specify the tag for the element. The
   * returned value can be modified to contain additional attributes and children.
   * 
   * @param doc used to create elements
   * @param tagName tag for the element created
   * @return XML reprepsentation of this object
   */
  protected Element internalToXml(final Document doc,
                                  final String tagName) {
    final Element ele = doc.createElement(tagName);
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
    final String rawScore = score.getEnumRawScore(getGoalName());
    if (null == rawScore) {
      throw new FLLRuntimeException("The enumerated goal "
          + getGoalName()
          + " has no score for team "
          + score.getTeamNumber());
    } else {
      return rawScore;
    }
  }

  @Override
  public String getRawStringValue() {
    return getGoalName();
  }
  // end StringValue interface
}
