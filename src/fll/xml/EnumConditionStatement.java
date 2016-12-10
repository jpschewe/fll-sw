/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * Condition statement in a switch case that uses enums.
 */
public class EnumConditionStatement extends AbstractConditionStatement {

  public static final String TAG_NAME = "enumCondition";

  public static final String ENUM_GOAL_REF_TAG_NAME = "enumGoalRef";

  public static final String STRING_CONSTANT_TAG_NAME = "stringConstant";

  public static final String GOAL_ATTRIBUTE = "goal";

  public static final String VALUE_ATTRIBUTE = "value";

  public EnumConditionStatement(final Element ele,
                                final GoalScope goalScope) {
    super(ele);

    mGoalScope = goalScope;

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.LEFT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter leftEles = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (leftEles.hasNext()) {
      final Element e = leftEles.next();
      mLeftGoalName = e.getAttribute(GOAL_ATTRIBUTE);
      mLeftString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      mLeftString = e.getAttribute(VALUE_ATTRIBUTE);
      mLeftGoalName = null;
    }

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.RIGHT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter rightEles = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (rightEles.hasNext()) {
      final Element e = rightEles.next();
      mRightGoalName = e.getAttribute(GOAL_ATTRIBUTE);
      mRightString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      mRightString = e.getAttribute(VALUE_ATTRIBUTE);
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
    if (null == mLeftGoalName) {
      return null;
    } else {
      return mGoalScope.getGoal(mLeftGoalName);
    }
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
    if (null == mRightGoalName) {
      return null;
    } else {
      return mGoalScope.getGoal(mRightGoalName);
    }
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
      rightStr = getRightString();
    }

    final boolean result = leftStr.equalsIgnoreCase(rightStr);
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

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element leftEle = doc.createElement(ConditionStatement.LEFT_TAG_NAME);
    final Element leftChild;
    if (null != mLeftGoalName) {
      leftChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      leftChild.setAttribute(GOAL_ATTRIBUTE, mLeftGoalName);
    } else {
      leftChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      leftChild.setAttribute(VALUE_ATTRIBUTE, mLeftString);
    }
    leftEle.appendChild(leftChild);
    ele.appendChild(leftEle);
    
    ele.appendChild(getComparisonElement(doc));
    

    final Element rightEle = doc.createElement(ConditionStatement.RIGHT_TAG_NAME);
    final Element rightChild;
    if (null != mRightGoalName) {
      rightChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      rightChild.setAttribute(GOAL_ATTRIBUTE, mLeftGoalName);
    } else {
      rightChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      rightChild.setAttribute(VALUE_ATTRIBUTE, mLeftString);
    }
    rightEle.appendChild(rightChild);
    ele.appendChild(rightEle);

    return ele;
  }
}
