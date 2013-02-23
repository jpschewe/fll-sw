/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

public class CaseStatement implements Evaluatable, Serializable {

  public CaseStatement(final Element ele,
                       final GoalScope goalScope,
                       final VariableScope variableScope) {
    final NodelistElementCollectionAdapter children = new NodelistElementCollectionAdapter(ele.getChildNodes());
    final Element condEle = children.next();
    if ("condition".equals(condEle.getNodeName())) {
      mCondition = new ConditionStatement(condEle, goalScope, variableScope);
    } else if ("enumCondition".equals(condEle.getNodeName())) {
      mCondition = new EnumConditionStatement(condEle, goalScope);
    } else {
      throw new FLLInternalException("Expecting 'condition' or 'enumCondition', but found '"
          + condEle.getNodeName() + "'");
    }

    final Element resultEle = children.next();
    if ("result".equals(resultEle.getNodeName())) {
      mResultPoly = new ComplexPolynomial(resultEle, goalScope, variableScope);
      mResultSwitch = null;
    } else if ("switch".equals(resultEle.getNodeName())) {
      mResultSwitch = new SwitchStatement(resultEle, goalScope, variableScope);
      mResultPoly = null;
    } else {
      throw new FLLInternalException("Expecting 'switch' or 'result', but found '"
          + resultEle.getNodeName() + "'");
    }

  }

  private final AbstractConditionStatement mCondition;

  public AbstractConditionStatement getCondition() {
    return mCondition;
  }

  private final ComplexPolynomial mResultPoly;

  /**
   * May be null, but then resultSwitch cannot be null.
   */
  public ComplexPolynomial getResultPoly() {
    return mResultPoly;
  }

  private final SwitchStatement mResultSwitch;

  /**
   * May be null, but then resultPoly cannot be null.
   */
  public SwitchStatement getResultSwitch() {
    return mResultSwitch;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    
    if (getCondition().isTrue(teamScore)) {
      if (null != getResultPoly()) {
        return getResultPoly().evaluate(teamScore);
      } else {
        return getResultSwitch().evaluate(teamScore);
      }
    } else {
      return 0;
    }
  }

}
