/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

public class CaseStatement implements Evaluatable, Serializable {

  public static final String TAG_NAME = "case";

  public static final String RESULT_TAG_NAME = "result";

  public CaseStatement(final Element ele,
                       final GoalScope goalScope,
                       final VariableScope variableScope) {
    final NodelistElementCollectionAdapter children = new NodelistElementCollectionAdapter(ele.getChildNodes());
    final Element condEle = children.next();
    if (ConditionStatement.TAG_NAME.equals(condEle.getNodeName())) {
      mCondition = new ConditionStatement(condEle, goalScope, variableScope);
    } else if (EnumConditionStatement.TAG_NAME.equals(condEle.getNodeName())) {
      mCondition = new EnumConditionStatement(condEle, goalScope);
    } else {
      throw new FLLInternalException("Expecting '"
          + ConditionStatement.TAG_NAME + "' or '" + EnumConditionStatement.TAG_NAME + "', but found '"
          + condEle.getNodeName() + "'");
    }

    final Element resultEle = children.next();
    if (RESULT_TAG_NAME.equals(resultEle.getNodeName())) {
      mResultPoly = new ComplexPolynomial(resultEle, goalScope, variableScope);
      mResultSwitch = null;
    } else if (SwitchStatement.TAG_NAME.equals(resultEle.getNodeName())) {
      mResultSwitch = new SwitchStatement(resultEle, goalScope, variableScope);
      mResultPoly = null;
    } else {
      throw new FLLInternalException("Expecting '"
          + SwitchStatement.TAG_NAME + "' or '" + RESULT_TAG_NAME + "', but found '" + resultEle.getNodeName() + "'");
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

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element conditionElement = mCondition.toXml(doc);
    ele.appendChild(conditionElement);

    final Element resultEle;
    if (null != mResultPoly) {
      resultEle = doc.createElement(RESULT_TAG_NAME);
      mResultPoly.populateXml(doc, resultEle);
    } else {
      resultEle = mResultSwitch.toXml(doc);
    }
    ele.appendChild(resultEle);

    return ele;
  }

}
