/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class CaseStatement implements Evaluatable, Serializable {

  public CaseStatement(final Element ele,
                       final GoalScope goalScope,
                       final VariableScope variableScope) {
    final NodelistElementCollectionAdapter condEles = new NodelistElementCollectionAdapter(
                                                                                           ele.getElementsByTagName("condition"));
    if (condEles.hasNext()) {
      final Element e = condEles.next();
      mCondition = new ConditionStatement(e, goalScope, variableScope);
    } else {
      final Element ee = new NodelistElementCollectionAdapter(ele.getElementsByTagName("enumCondition")).next();
      mCondition = new EnumConditionStatement(ee, goalScope);
    }

    final NodelistElementCollectionAdapter resultEles = new NodelistElementCollectionAdapter(
                                                                                             ele.getElementsByTagName("result"));
    if (resultEles.hasNext()) {
      final Element re = resultEles.next();
      mResultPoly = new ComplexPolynomial(re, goalScope, variableScope);
      mResultSwitch = null;
    } else {
      final Element se = new NodelistElementCollectionAdapter(ele.getElementsByTagName("switch")).next();
      mResultSwitch = new SwitchStatement(se, goalScope, variableScope);
      mResultPoly = null;
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

}
