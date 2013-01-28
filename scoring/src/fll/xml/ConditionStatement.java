/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.playoff.TeamScore;

/**
 * Condition statement in a switch case that uses numbers.
 */
public class ConditionStatement extends AbstractConditionStatement {

  public ConditionStatement(final Element ele,
                            final GoalScope goalScope,
                            final VariableScope variableScope) {

    final Element leftEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("left")).next();
    mLeft = new ComplexPolynomial(leftEle, goalScope, variableScope);

    if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("less-than")).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("less-than-or-equal")).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("greater-than")).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("greater-than-or-equal")).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("equal-to")).hasNext()) {
      mComparison = InequalityComparison.EQUAL_TO;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("not-equal-to")).hasNext()) {
      mComparison = InequalityComparison.NOT_EQUAL_TO;
    } else {
      throw new FLLInternalException("Unknown comparison");
    }

    final Element rightEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("right")).next();
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

  private final InequalityComparison mComparison;

  public InequalityComparison getComparison() {
    return mComparison;
  }

}
