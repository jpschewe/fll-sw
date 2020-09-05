/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Condition statement in a switch case that uses enums.
 */
public class EnumConditionStatement extends AbstractConditionStatement {

  /**
   * XML tag used for this class.
   */
  public static final String TAG_NAME = "enumCondition";

  private static final String ENUM_GOAL_REF_TAG_NAME = "enumGoalRef";

  private static final String STRING_CONSTANT_TAG_NAME = "stringConstant";

  private static final String GOAL_ATTRIBUTE = "goal";

  private static final String VALUE_ATTRIBUTE = "value";

  /**
   * @param ele the element to parse
   * @param goalScope where to find goals
   */
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

  /**
   * Default constructor. This has {@link #getLeftString()},
   * {@link #getLeftGoalRef()}, {@link #getRightGoalRef()},
   * {@link #getRightString()} all set to null.
   * The object cannot be evaluated in this state.
   */
  public EnumConditionStatement() {
    super();
    mLeftGoalRef = null;
    mLeftString = null;
    mRightGoalRef = null;
    mRightString = null;
  }

  private @Nullable String mLeftString;

  /**
   * Left string, may be null, but then leftGoal cannot not null at evaluation
   * time.
   *
   * @return the left string to compare against, may be null.
   */
  public @Nullable String getLeftString() {
    return mLeftString;
  }

  /**
   * @param v see {@link #getLeftString()}
   */
  public void setLeftString(final @Nullable String v) {
    mLeftString = v;
  }

  private @Nullable GoalRef mLeftGoalRef;

  /**
   * Left goal reference, may be null, but then leftString cannot be null at
   * evaluation time.
   *
   * @return the reference to the left goal, may be null
   */
  public @Nullable GoalRef getLeftGoalRef() {
    return mLeftGoalRef;
  }

  /**
   * @param v see {@link #getLeftGoalRef()}
   */
  public void setLeftGoalRef(final @Nullable GoalRef v) {
    mLeftGoalRef = v;
  }

  /**
   * Left goal, may be null, but then {@link #getLeftString()} must not be null at
   * evaluation time.
   * If {@link #getLeftGoalRef()} is not null, resolves the goal reference to a
   * goal.
   *
   * @see GoalRef#getGoal()
   * @return the left goal
   */
  public @Nullable AbstractGoal getLeftGoal() {
    if (null == mLeftGoalRef) {
      return null;
    } else {
      return mLeftGoalRef.getGoal();
    }
  }

  /**
   * @return the left goal name or the left string, whichever is not null
   * @throws IllegalArgumentException if both {@link #getLeftGoal()}
   *           and{@link #getLeftString()} are null
   */
  public String getLeftGoalNameOrString() {
    final AbstractGoal leftGoal = getLeftGoal();
    final String leftRawString = getLeftString();
    if (null != leftGoal) {
      return leftGoal.getName();
    } else if (null != leftRawString) {
      return leftRawString;
    } else {
      throw new IllegalArgumentException("Right goal ref OR right string must be non-null");
    }
  }

  private @Nullable String mRightString;

  /**
   * Right string, may be null, but then rightGoal is not null.
   *
   * @return the right string
   */
  public @Nullable String getRightString() {
    return mRightString;
  }

  /**
   * @param v see {@link #getRightString()}
   */
  public void setRightString(final @Nullable String v) {
    mRightString = v;
  }

  private @Nullable GoalRef mRightGoalRef;

  /**
   * Right goal reference, may be null, but then right string cannot be null at
   * evaluation time.
   *
   * @return the reference to the right goal, may be null
   */
  public @Nullable GoalRef getRightGoalRef() {
    return mRightGoalRef;
  }

  /**
   * @param v see {@link #getRightGoalRef()}
   */
  public void setRightGoalRef(final @Nullable GoalRef v) {
    mRightGoalRef = v;
  }

  /**
   * Right goal, may be null, but then {@link #getRightString()} must not null at
   * evalution time.
   * If {@link #getRightGoalRef()} is not null, resolves the goal reference to a
   * goal.
   *
   * @return the goal for the right side of the conditional
   * @see GoalRef#getGoal()
   */
  public @Nullable AbstractGoal getRightGoal() {
    if (null == mRightGoalRef) {
      return null;
    } else {
      return mRightGoalRef.getGoal();
    }
  }

  /**
   * @return the right goal name or the right string, whichever is not null
   * @throws IllegalArgumentException if both {@link #getRightGoal()}
   *           and{@link #getRightString()} are null
   */
  public String getRightGoalNameOrString() {
    final AbstractGoal rightGoal = getRightGoal();
    final String rightRawString = getRightString();
    if (null != rightGoal) {
      return rightGoal.getName();
    } else if (null != rightRawString) {
      return rightRawString;
    } else {
      throw new IllegalArgumentException("Right goal ref OR right string must be non-null");
    }
  }

  /**
   * @see fll.xml.AbstractConditionStatement#isTrue(fll.web.playoff.TeamScore)
   * @throws NullPointerException if both left goal ref and left string are null
   *           or both right goal ref and right string are null
   */
  @Override
  public boolean isTrue(final TeamScore teamScore) {
    final String leftStr = getLeftGoalNameOrString();

    final String rightStr = getRightGoalNameOrString();

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
