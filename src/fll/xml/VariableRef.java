/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class VariableRef implements Evaluatable, Serializable {

  public VariableRef(final Element ele,
                     final VariableScope scope) {
    mVariableName = ele.getAttribute("variable");
    mVariableScope = scope;
  }

  private final String mVariableName;
  public String getVariableName() {
    return mVariableName;
  }

  private final VariableScope mVariableScope;

  public Variable getVariable() {
    return mVariableScope.getVariable(mVariableName);
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    return getVariable().evaluate(teamScore);
  }

}
