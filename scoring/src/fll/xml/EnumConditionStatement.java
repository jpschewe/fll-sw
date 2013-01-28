/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * Condition statement in a switch case that uses enums.
 */
public class EnumConditionStatement extends AbstractConditionStatement {

  public EnumConditionStatement(final Element ele,
                                final GoalScope goalScope) {

    mGoalScope = goalScope;

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("left")).next();
    final NodelistElementCollectionAdapter leftEles = new NodelistElementCollectionAdapter(
                                                                                           leftEle.getElementsByTagName("goalRef"));
    if (leftEles.hasNext()) {
      final Element e = leftEles.next();
      mLeftGoalName = e.getAttribute("goal");
      mLeftString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName("stringConstant")).next();
      mLeftString = e.getAttribute("value");
      mLeftGoalName = null;
    }

    if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("equal-to")).hasNext()) {
      mComparison = EqualityComparison.EQUAL_TO;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("not-equal-to")).hasNext()) {
      mComparison = EqualityComparison.NOT_EQUAL_TO;
    } else {
      throw new FLLInternalException("Unknown comparison");
    }

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("right")).next();
    final NodelistElementCollectionAdapter rightEles = new NodelistElementCollectionAdapter(
                                                                                            rightEle.getElementsByTagName("goalRef"));
    if (rightEles.hasNext()) {
      final Element e = rightEles.next();
      mRightGoalName = e.getAttribute("goal");
      mRightString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName("stringConstant")).next();
      mRightString = e.getAttribute("value");
      mRightGoalName = null;
    }
  }

  private final GoalScope mGoalScope;

  private final String mLeftString;

  /**
   * Left string, may be null, but then leftGoal is not null.
   */
  public String getLeftString() {
    return mLeftString;
  }

  private final String mLeftGoalName;

  /**
   * Left goal, may be null, but then leftString is not null.
   */
  public AbstractGoal getLeftGoal() {
    return mGoalScope.getGoal(mLeftGoalName);
  }

  private final String mRightString;

  /**
   * Right string, may be null, but then rightGoal is not null.
   */
  public String getRightString() {
    return mRightString;
  }

  private final String mRightGoalName;

  /**
   * Right goal, may be null, but then rightString is not null.
   */
  public AbstractGoal getRightGoal() {
    return mGoalScope.getGoal(mRightGoalName);
  }

  private final EqualityComparison mComparison;

  public EqualityComparison getComparison() {
    return mComparison;
  }

  public boolean isTrue(TeamScore teamScore) {
    final String leftStr;
    if (null != getLeftGoal()) {
      leftStr = teamScore.getEnumRawScore(getLeftGoal().getName());
    } else {
      leftStr = getLeftString();
    }

    final String rightStr;
    if (null != getRightGoal()) {
      rightStr = teamScore.getEnumRawScore(getRightGoal().getName());
    } else {
      rightStr = getLeftString();
    }

    final boolean result = ComparisonUtils.safeEquals(leftStr, rightStr);
    switch (getComparison()) {
    case EQUAL_TO:
      return result;
    case NOT_EQUAL_TO:
      return !result;
    default:
      throw new FLLInternalException("Unknown comparison: "
          + getComparison());
    }

  }
}
