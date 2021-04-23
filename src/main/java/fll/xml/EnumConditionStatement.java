/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
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
                                final @UnknownInitialization GoalScope goalScope) {
    super(ele);

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.LEFT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter leftEles = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (leftEles.hasNext()) {
      final Element e = leftEles.next();
      // enum goals are referenced by raw value
      left = new GoalRef(e.getAttribute(GOAL_ATTRIBUTE), goalScope, GoalScoreType.RAW);
    } else {
      final Element e = new NodelistElementCollectionAdapter(leftEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      left = new StringConstant(e.getAttribute(VALUE_ATTRIBUTE));
    }

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(ConditionStatement.RIGHT_TAG_NAME)).next();
    final NodelistElementCollectionAdapter rightEles = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(ENUM_GOAL_REF_TAG_NAME));
    if (rightEles.hasNext()) {
      final Element e = rightEles.next();
      right = new GoalRef(e.getAttribute(GOAL_ATTRIBUTE), goalScope, GoalScoreType.RAW);
    } else {
      final Element e = new NodelistElementCollectionAdapter(rightEle.getElementsByTagName(STRING_CONSTANT_TAG_NAME)).next();
      right = new StringConstant(e.getAttribute(VALUE_ATTRIBUTE));
    }
  }

  /**
   * @param left see {@link #getLeft()}
   * @param right see {@link #getRight()}
   */
  public EnumConditionStatement(final StringValue left,
                                final StringValue right) {
    super();
    this.right = right;
    this.left = left;
  }

  private @NotOnlyInitialized StringValue left;

  /**
   * @return the left side of the inequality
   */
  public StringValue getLeft() {
    return left;
  }

  /**
   * @param v see {@link #getLeft()}
   */
  public void setLeft(final StringValue v) {
    left = v;
  }

  private @NotOnlyInitialized StringValue right;

  /**
   * @return the right side of the inequality
   */
  public StringValue getRight() {
    return right;
  }

  /**
   * @param v see {@link #getRight()}
   */
  public void setRight(final StringValue v) {
    right = v;
  }

  @Override
  public boolean isTrue(final TeamScore teamScore) {
    final String leftStr = getLeft().getStringValue(teamScore);

    final String rightStr = getRight().getStringValue(teamScore);

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

    if (left.isGoalRef()) {
      leftChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      leftChild.setAttribute(GOAL_ATTRIBUTE, left.getRawStringValue());
    } else {
      leftChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      leftChild.setAttribute(VALUE_ATTRIBUTE, left.getRawStringValue());
    }
    leftEle.appendChild(leftChild);
    ele.appendChild(leftEle);

    ele.appendChild(getComparisonElement(doc));

    final Element rightEle = doc.createElement(ConditionStatement.RIGHT_TAG_NAME);
    final Element rightChild;

    final StringValue right = getRight();
    if (right.isGoalRef()) {
      final GoalRef rightGoalRef = (GoalRef) right;
      rightChild = doc.createElement(ENUM_GOAL_REF_TAG_NAME);
      rightChild.setAttribute(GOAL_ATTRIBUTE, rightGoalRef.getGoalName());
    } else {
      final StringConstant rightString = (StringConstant) right;
      rightChild = doc.createElement(STRING_CONSTANT_TAG_NAME);
      rightChild.setAttribute(VALUE_ATTRIBUTE, rightString.getValue());
    }
    rightEle.appendChild(rightChild);
    ele.appendChild(rightEle);

    return ele;
  }
}
