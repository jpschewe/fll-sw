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
import fll.util.FP;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Condition statement in a switch case that uses numbers.
 */
public class ConditionStatement extends AbstractConditionStatement {

  /**
   * XML tag used for the class.
   */
  public static final String TAG_NAME = "condition";

  /**
   * XML tag used for {@link #getLeft()}.
   */
  public static final String LEFT_TAG_NAME = "left";

  /**
   * XML tag used for {@link #getRight()}.
   */
  public static final String RIGHT_TAG_NAME = "right";

  /**
   * @param ele the element to parse
   * @param goalScope where to find goals
   * @param variableScope where to find variables
   */
  public ConditionStatement(final Element ele,
                            final @UnknownInitialization GoalScope goalScope,
                            final @UnknownInitialization VariableScope variableScope) {
    super(ele);

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(LEFT_TAG_NAME)).next();
    mLeft = new ComplexPolynomial(leftEle, goalScope, variableScope);

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(RIGHT_TAG_NAME)).next();
    mRight = new ComplexPolynomial(rightEle, goalScope, variableScope);
  }

  /**
   * Default constructor with empty left and right polynomials.
   */
  public ConditionStatement() {
    super();
    mLeft = new ComplexPolynomial();
    mRight = new ComplexPolynomial();
  }

  private final @NotOnlyInitialized ComplexPolynomial mLeft;

  /**
   * @return the left side of the condition
   */
  public ComplexPolynomial getLeft() {
    return mLeft;
  }

  private final @NotOnlyInitialized ComplexPolynomial mRight;

  /**
   * @return the right side of the condition
   */
  public ComplexPolynomial getRight() {
    return mRight;
  }

  @Override
  public boolean isTrue(TeamScore teamScore) {
    final double left = getLeft().evaluate(teamScore);
    final double right = getRight().evaluate(teamScore);

    switch (getComparison()) {
    case GREATER_THAN:
      return FP.greaterThan(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    case GREATER_THAN_OR_EQUAL:
      return FP.greaterThanOrEqual(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    case LESS_THAN:
      return FP.lessThan(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    case LESS_THAN_OR_EQUAL:
      return FP.lessThanOrEqual(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    case EQUAL_TO:
      return FP.equals(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    case NOT_EQUAL_TO:
      return !FP.equals(left, right, ChallengeParser.INITIAL_VALUE_TOLERANCE);
    default:
      throw new FLLInternalException("Unknown comparison: "
          + getComparison());
    }

  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element leftEle = doc.createElement(LEFT_TAG_NAME);
    mLeft.populateXml(doc, leftEle);
    ele.appendChild(leftEle);

    ele.appendChild(getComparisonElement(doc));

    final Element rightEle = doc.createElement(RIGHT_TAG_NAME);
    mRight.populateXml(doc, rightEle);
    ele.appendChild(rightEle);

    return ele;
  }

}
