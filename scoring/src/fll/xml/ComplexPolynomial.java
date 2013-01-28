/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

/**
 * A polynomial that can reference variables.
 */
public class ComplexPolynomial extends BasicPolynomial {

  public ComplexPolynomial(final Element ele,
                           final GoalScope goalScope,
                           final VariableScope variableScope) {
    super(ele, goalScope);

    final List<VariableTerm> variableTerms = new LinkedList<VariableTerm>();
    for (final Element varTermEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("variableRef"))) {
      final VariableTerm varTerm = new VariableTerm(varTermEle, variableScope);
      variableTerms.add(varTerm);
    }
    mVariableTerms = Collections.unmodifiableList(variableTerms);

  }

  private final List<VariableTerm> mVariableTerms;

  public List<VariableTerm> getVariableTerms() {
    return mVariableTerms;
  }

}
