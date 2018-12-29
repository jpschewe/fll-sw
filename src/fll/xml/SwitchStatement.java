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

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

public class SwitchStatement implements Evaluatable, Serializable {

  public SwitchStatement(final Element ele,
                         final GoalScope goalScope,
                         final VariableScope variableScope) {
    ComplexPolynomial defaultCase = null;
    final List<CaseStatement> cases = new LinkedList<CaseStatement>();
    for (final Element caseEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if ("case".equals(caseEle.getNodeName())) {
        final CaseStatement cs = new CaseStatement(caseEle, goalScope, variableScope);
        cases.add(cs);
      } else if ("default".equals(caseEle.getNodeName())) {
        defaultCase = new ComplexPolynomial(caseEle, goalScope, variableScope);
      } else {
        throw new FLLInternalException("Expecting 'case' or 'default', but found '"
            + caseEle.getNodeName() + "'");
      }
    }
    mCases = Collections.unmodifiableList(cases);
    mDefaultCase = defaultCase;
  }

  private final List<CaseStatement> mCases;

  public List<CaseStatement> getCases() {
    return mCases;
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

}
