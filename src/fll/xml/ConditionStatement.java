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
import fll.util.FP;
import fll.web.playoff.TeamScore;

/**
 * Condition statement in a switch case that uses numbers.
 */
public class ConditionStatement extends AbstractConditionStatement {

  public static final String TAG_NAME = "condition";

  public static final String LEFT_TAG_NAME = "left";

  public static final String RIGHT_TAG_NAME = "right";

  public ConditionStatement(final Element ele,
                            final GoalScope goalScope,
                            final VariableScope variableScope) {
    super(ele);

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(LEFT_TAG_NAME)).next();
    mLeft = new ComplexPolynomial(leftEle, goalScope, variableScope);

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(RIGHT_TAG_NAME)).next();
    mRight = new ComplexPolynomial(rightEle, goalScope, variableScope);
  }
  
  private final ComplexPolynomial mLeft;

  public ComplexPolynomial getLeft() {
    return mLeft;
  }

  private final ComplexPolynomial mRight;

  public ComplexPolynomial getRight() {
    return mRight;
  }

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
