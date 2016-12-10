/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

public class SwitchStatement implements Evaluatable, Serializable {

  public static final String TAG_NAME = "switch";

  public static final String DEFAULT_TAG_NAME = "default";

  public SwitchStatement(final Element ele,
                         final GoalScope goalScope,
                         final VariableScope variableScope) {
    ComplexPolynomial defaultCase = null;
    mCases = new LinkedList<CaseStatement>();
    for (final Element caseEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if (CaseStatement.TAG_NAME.equals(caseEle.getNodeName())) {
        final CaseStatement cs = new CaseStatement(caseEle, goalScope, variableScope);
        mCases.add(cs);
      } else if (DEFAULT_TAG_NAME.equals(caseEle.getNodeName())) {
        defaultCase = new ComplexPolynomial(caseEle, goalScope, variableScope);
      } else {
        throw new FLLInternalException("Expecting '"
            + CaseStatement.TAG_NAME + "' or '" + DEFAULT_TAG_NAME + "', but found '" + caseEle.getNodeName() + "'");
      }
    }
    mDefaultCase = defaultCase;
  }

  private final List<CaseStatement> mCases;

  public List<CaseStatement> getCases() {
    return Collections.unmodifiableList(mCases);
  }

  /**
   * Add a case statement to the end of the case statements.
   * 
   * @param v the case statement to add
   */
  public void addCase(final CaseStatement v) {
    mCases.add(v);
  }

  /**
   * Add a case statement at the specified position.
   * 
   * @param index the index to add the case statement at
   * @param v the case statement to add
   */
  public void addCase(final int index,
                      final CaseStatement v)
      throws IndexOutOfBoundsException {
    mCases.add(index, v);
  }

  /**
   * Remove a case statement
   * 
   * @param v the case statement to remove
   * @return true if the case statement was removed
   */
  public boolean removeCase(final CaseStatement v) {
    return mCases.remove(v);
  }

  /**
   * Remove a case statement at a specified position
   * 
   * @param index the position to remove from
   * @return the case statement that was removed
   * @throws IndexOutOfBoundsException
   */
  public CaseStatement removeCase(final int index) throws IndexOutOfBoundsException {
    return mCases.remove(index);
  }

  private final ComplexPolynomial mDefaultCase;

  public ComplexPolynomial getDefaultCase() {
    return mDefaultCase;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    for (final CaseStatement cs : getCases()) {
      if (cs.getCondition().isTrue(teamScore)) {
        return cs.evaluate(teamScore);
      }
    }
    return getDefaultCase().evaluate(teamScore);
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    for (final CaseStatement cs : mCases) {
      final Element caseEle = cs.toXml(doc);
      ele.appendChild(caseEle);
    }

    if (null != mDefaultCase) {
      final Element defaultEle = doc.createElement(DEFAULT_TAG_NAME);
      mDefaultCase.populateXml(doc, defaultEle);
      ele.appendChild(defaultEle);
    }

    return ele;
  }

}
