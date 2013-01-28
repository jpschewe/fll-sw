/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class VariableTerm extends AbstractTerm {

  public VariableTerm(final Element ele,
                      final VariableScope scope) {
    super(ele);

    final String variableName = ele.getAttribute("variable");
    mVariable = scope.getVariable(variableName);
  }

  private final Variable mVariable;

  public Variable getVariable() {
    return mVariable;
  }

}
