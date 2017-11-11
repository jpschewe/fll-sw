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

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.LEFT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter leftEles = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (leftEles.hasNext()) {
      final Element e = leftEles.next();
      // enum goals are referenced by raw value
      mLeftGoalRef = new GoalRef(e.getAttribute(GOAL_ATTRIBUTE), goalScope, GoalScoreType.RAW);
      mLeftString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      mLeftString = e.getAttribute(VALUE_ATTRIBUTE);
      mLeftGoalRef = null;
    }

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.RIGHT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter rightEles = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (rightEles.hasNext()) {
      final Element e = rightEles.next();
      mRightGoalRef = new GoalRef(e.getAttribute(GOAL_ATTRIBUTE), goalScope, GoalScoreType.RAW);
      mRightString = null;
    } else {
      final Element e = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      mRightString = e.getAttribute(VALUE_ATTRIBUTE);
      mRightGoalRef = null;
    }
  }

  private String mLeftString;

  /**
   * Left string, may be null, but then leftGoal cannot not null at evalution
   * time.
   * 
   * @return the left string to compare against, may be null.
   */
  public String getLeftString() {
    return mLeftString;
  }

  /**
   * @param v see {@link #getLeftString()}
   */
  public void setLeftString(final String v) {
    mLeftString = v;
  }

  private GoalRef mLeftGoalRef;

  /**
   * Left goal reference, may be null, but then leftString cannot be null at
   * evaluation time.
   * 
   * @return the reference to the left goal, may be null
   */
  public GoalRef getLeftGoalRef() {
    return mLeftGoalRef;
  }

  /**
   * @param v see {@link #getLeftGoalRef()}
   */
  public void setLeftGoalRef(final GoalRef v) {
    mLeftGoalRef = v;
  }

  /**
   * Left goal, may be null, but then {@link #getLeftString()} must not be null at
   * evalution time.
   * If {@link #getLeftGoalRef()} is not null, resolves the goal reference to a
   * goal.
   * 
   * @see GoalRef#getGoal()
   */
  public AbstractGoal getLeftGoal() {
    if (null == mLeftGoalRef) {
      return null;
    } else {
      return mLeftGoalRef.getGoal();
    }
  }

  private String mRightString;

  /**
   * Right string, may be null, but then rightGoal is not null.
   */
  public String getRightString() {
    return mRightString;
  }

  /**
   * @param v see {@link #getRightString()}
   */
  public void setRightString(final String v) {
    mRightString = v;
  }

  private GoalRef mRightGoalRef;

  /**
   * Right goal reference, may be null, but then right string cannot be null at
   * evaluation time.
   * 
   * @return the reference to the right goal, may be null
   */
  public GoalRef getRightGoalRef() {
    return mRightGoalRef;
  }

  /**
   * @param v see {@link #getRightGoalRef()}
   */
  public void setRightGoalRef(final GoalRef v) {
    mRightGoalRef = v;
  }

  /**
   * Right goal, may be null, but then {@link #getRightString()} must not null at
   * evalution time.
   * If {@link #getRightGoalRef()} is not null, resolves the goal reference to a
   * goal.
   * 
   * @see GoalRef#getGoal()
   */
  public AbstractGoal getRightGoal() {
    if (null == mRightGoalRef) {
      return null;
    } else {
      return mRightGoalRef.getGoal();
    }
  }

  /**
   * @see fll.xml.AbstractConditionStatement#isTrue(fll.web.playoff.TeamScore)
   * @throws NullPointerException if both left goal ref and left string are null
   *           or both right goal ref and right string are null
   */
  @Override
  public boolean isTrue(final TeamScore teamScore) {
    if (null == getLeftGoalRef()
        && null == getLeftString()) {
      throw new NullPointerException("Left goal ref OR left string must be non-null");
    }
    if (null == getRightGoalRef()
        && null == getRightString()) {
      throw new NullPointerException("Right goal ref OR right string must be non-null");
    }

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
    if (null != mLeftGoalRef) {
      leftChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      leftChild.setAttribute(GOAL_ATTRIBUTE, mLeftGoalRef.getGoalName());
    } else {
      leftChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      leftChild.setAttribute(VALUE_ATTRIBUTE, mLeftString);
    }
    leftEle.appendChild(leftChild);
    ele.appendChild(leftEle);

    ele.appendChild(getComparisonElement(doc));

    final Element rightEle = doc.createElement(ConditionStatement.RIGHT_TAG_NAME);
    final Element rightChild;
    if (null != mRightGoalRef) {
      rightChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      rightChild.setAttribute(GOAL_ATTRIBUTE, mRightGoalRef.getGoalName());
    } else {
      rightChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      rightChild.setAttribute(VALUE_ATTRIBUTE, mLeftString);
    }
    rightEle.appendChild(rightChild);
    ele.appendChild(rightEle);

    return ele;
  }
}
