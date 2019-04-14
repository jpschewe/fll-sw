/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

public class CaseStatement implements Evaluatable, Serializable {

  public static final String TAG_NAME = "case";

  public static final String RESULT_TAG_NAME = "result";

  /**
   * Construct from an XML document.
   *
   * @param ele the element to parse
   * @param goalScope where to lookup referenced goals
   * @param variableScope where to lookup referenced variables
   */
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
          + ConditionStatement.TAG_NAME
          + "' or '"
          + EnumConditionStatement.TAG_NAME
          + "', but found '"
          + condEle.getNodeName()
          + "'");
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
          + SwitchStatement.TAG_NAME
          + "' or '"
          + RESULT_TAG_NAME
          + "', but found '"
          + resultEle.getNodeName()
          + "'");
    }

  }

  /**
   * Default constructor. All values are null, they must be set before
   * {@link #evaluate(TeamScore)} is called.
   */
  public CaseStatement() {
    mCondition = null;
    mResultPoly = null;
    mResultSwitch = null;
  }

  private AbstractConditionStatement mCondition;

  /**
   * The condition to evaluate.
   *
   * @return may not be null at evaluation time
   */
  public AbstractConditionStatement getCondition() {
    return mCondition;
  }

  /**
   * Specify a new condition statement.
   *
   * @param v see {@link #getCondition()}
   */
  public void setCondition(final AbstractConditionStatement v) {
    mCondition = v;
  }

  private ComplexPolynomial mResultPoly;

  /**
   * May be null, but then resultSwitch cannot be null at evaluation time.
   *
   * @return the polynomial that defines the result
   */
  public ComplexPolynomial getResultPoly() {
    return mResultPoly;
  }

  /**
   * @param v see {@link #getResultPoly()}
   */
  public void setResultPoly(final ComplexPolynomial v) {
    mResultPoly = v;
  }

  private SwitchStatement mResultSwitch;

  /**
   * May be null, but then {@link #getResultPoly()} cannot be null at evaluation
   * time.
   *
   * @param the switch statement that defines the result
   */
  public SwitchStatement getResultSwitch() {
    return mResultSwitch;
  }

  /**
   * @param v see {@link #getResultSwitch()}
   */
  public void setResultSwitch(final SwitchStatement v) {
    mResultSwitch = v;
  }

  /**
   * @see fll.xml.Evaluatable#evaluate(fll.web.playoff.TeamScore)
   * @throws NullPointerException if the {@link #getCondition()} is null or both
   *           {@link #getResultPoly()} and {@link #getResultSwitch()} are null.
   */
  @Override
  public double evaluate(final TeamScore teamScore) {
    Objects.requireNonNull(getCondition(), "Condition must not be null");
    if (null == getResultPoly()
        && null == getResultSwitch()) {
      throw new NullPointerException("Both result poly and result switch cannot be null");
    }

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

  /**
   * Convert the object to XML.
   *
   * @param doc the document to add to
   * @return the XML element
   */
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
