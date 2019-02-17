/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class VariableRef implements Evaluatable, Serializable {

  public static final String TAG_NAME = "variableRef";

  public static final String VARIABLE_ATTRIBUTE = "variable";

  /**
   * Create a new variable reference.
   * 
   * @param ele the XML element to parse the information from
   * @param scope the scope to lookup the variable in at evaluation time
   */
  public VariableRef(@Nonnull final Element ele,
                     @Nonnull final VariableScope scope) {
    this(ele.getAttribute(VARIABLE_ATTRIBUTE), scope);
  }

  public VariableRef(@Nonnull final String variableName,
                     @Nonnull final VariableScope scope) {
    mVariableName = variableName;
    mVariableScope = scope;
  }

  private String mVariableName;

  /**
   * @return the variable name to reference
   */
  @Nonnull
  public String getVariableName() {
    return mVariableName;
  }

  /**
   * @param v see {@link #getVariableName()}
   */
  public void setVariableName(@Nonnull final String v) {
    mVariableName = v;
  }

  private final VariableScope mVariableScope;

  /**
   * @return the scope used to lookup the variable
   */
  @Nonnull
  public VariableScope getVariableScope() {
    return mVariableScope;
  }

  /**
   * @return the variable
   * @throws ScopeException if the variable cannot be found in the scope
   * @see VariableScope#getVariable(String)
   */
  @Nonnull
  public Variable getVariable() {
    return mVariableScope.getVariable(mVariableName);
  }

  /**
   * @see fll.xml.Evaluatable#evaluate(fll.web.playoff.TeamScore)
   * @see #getVariable()
   */
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
