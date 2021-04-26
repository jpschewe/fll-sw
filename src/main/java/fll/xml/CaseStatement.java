/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Represents a case statement in a {@link ComputedGoal}.
 */
public class CaseStatement implements Evaluatable, Serializable {

  /**
   * XML tag used for this class.
   */
  public static final String TAG_NAME = "case";

  /**
   * XML tag used for the result.
   */
  public static final String RESULT_TAG_NAME = "result";

  /**
   * Construct from an XML document.
   *
   * @param ele the element to parse
   * @param goalScope where to lookup referenced goals
   * @param variableScope where to lookup referenced variables
   */
  public CaseStatement(final Element ele,
                       final @UnknownInitialization GoalScope goalScope,
                       final @UnknownInitialization VariableScope variableScope) {
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
      mResult = new ComplexPolynomial(resultEle, goalScope, variableScope);
    } else if (SwitchStatement.TAG_NAME.equals(resultEle.getNodeName())) {
      mResult = new SwitchStatement(resultEle, goalScope, variableScope);
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
   * Default constructor creates an empty condition and uses a polynomial as the
   * result.
   * 
   * @see ConditionStatement#ConditionStatement()
   * @see ComplexPolynomial#ComplexPolynomial()
   */
  public CaseStatement() {
    mCondition = new ConditionStatement();
    mResult = new ComplexPolynomial();
  }

  private @NotOnlyInitialized AbstractConditionStatement mCondition;

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

  private @NotOnlyInitialized CaseStatementResult mResult;

  /**
   * The result of the case statement.
   *
   * @return the result of the case statement if true
   */
  public CaseStatementResult getResult() {
    return mResult;
  }

  /**
   * @param v see {@link #getResult()}
   */
  public void setResult(final CaseStatementResult v) {
    mResult = v;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    if (getCondition().isTrue(teamScore)) {
      return mResult.evaluate(teamScore);
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
    final Evaluatable result = getResult();
    if (result instanceof ComplexPolynomial) {
      final ComplexPolynomial resultPoly = (ComplexPolynomial) result;
      resultEle = doc.createElement(RESULT_TAG_NAME);
      resultPoly.populateXml(doc, resultEle);
    } else if (result instanceof SwitchStatement) {
      final SwitchStatement resultSwitch = (SwitchStatement) result;
      resultEle = resultSwitch.toXml(doc);
    } else {
      throw new FLLInternalException("Unexpected result object type for CastStatement: "
          + result);
    }
    ele.appendChild(resultEle);

    return ele;
  }

  /**
   * Computed the value based on {@link #getResult()}.
   * 
   * @return score type for the statement
   */
  public ScoreType getScoreType() {
    return mResult.getScoreType();
  }

}
