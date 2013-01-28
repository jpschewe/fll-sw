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

import fll.web.playoff.TeamScore;

public class SwitchStatement implements Evaluatable, Serializable {

  public SwitchStatement(final Element ele,
                         final GoalScope goalScope,
                         final VariableScope variableScope) {

    final List<CaseStatement> cases = new LinkedList<CaseStatement>();
    for(final Element caseEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("case"))) {
      final CaseStatement cs = new CaseStatement(caseEle, goalScope, variableScope);
      cases.add(cs);
    }
    mCases = Collections.unmodifiableList(cases);
    
    final Element defaultEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("default")).next();
    mDefaultCase = new ComplexPolynomial(defaultEle, goalScope, variableScope);
  }

  private final List<CaseStatement> mCases;

  public List<CaseStatement> getCases() {
    return mCases;
  }

  private final ComplexPolynomial mDefaultCase;

  public ComplexPolynomial getDefaultCase() {
    return mDefaultCase;
  }

}
