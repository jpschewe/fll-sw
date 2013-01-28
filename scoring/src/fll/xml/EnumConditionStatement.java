/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

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

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("left")).next();
    final NodelistElementCollectionAdapter leftEles = new NodelistElementCollectionAdapter(
                                                                                           leftEle.getElementsByTagName("goalRef"));
    if (leftEles.hasNext()) {
      final Element e = leftEles.next();
      mLeftGoal = goalScope.getGoal(e.getAttribute("goal"));
      mLeftString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName("stringConstant")).next();
      mLeftString = e.getAttribute("value");
      mLeftGoal = null;
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
      mRightGoal = goalScope.getGoal(e.getAttribute("goal"));
      mRightString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName("stringConstant")).next();
      mRightString = e.getAttribute("value");
      mRightGoal = null;
    }
  }

  private final String mLeftString;

  /**
   * Left string, may be null, but then leftGoal is not null.
   */
  public String getLeftString() {
    return mLeftString;
  }

  private final AbstractGoal mLeftGoal;

  /**
   * Left goal, may be null, but then leftString is not null.
   */
  public AbstractGoal getLeftGoal() {
    return mLeftGoal;
  }

  private final String mRightString;

  /**
   * Right string, may be null, but then rightGoal is not null.
   */
  public String getRightString() {
    return mRightString;
  }

  private final AbstractGoal mRightGoal;

  /**
   * Right goal, may be null, but then rightString is not null.
   */
  public AbstractGoal getRightGoal() {
    return mRightGoal;
  }

  private final EqualityComparison mComparison;

  public EqualityComparison getComparison() {
    return mComparison;
  }

}
