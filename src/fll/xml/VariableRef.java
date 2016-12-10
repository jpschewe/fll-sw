/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class VariableRef implements Evaluatable, Serializable {

  public static final String TAG_NAME = "variableRef";

  public static final String VARIABLE_ATTRIBUTE = "variable";

  /**
   * Create a new variable reference.
   * 
   * @param ele
   * @param scope
   * @throws NullPointerException if scope or ele are null
   */
  public VariableRef(final Element ele,
                     final VariableScope scope) {
    if (null == scope) {
      throw new NullPointerException("Scope must not be null");
    }

    mVariableName = ele.getAttribute(VARIABLE_ATTRIBUTE);
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

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(VARIABLE_ATTRIBUTE, mVariableName);
    return ele;
  }
}
