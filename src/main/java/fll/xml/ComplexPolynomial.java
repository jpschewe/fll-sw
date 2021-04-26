/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Element;

/**
 * A polynomial that can reference variables.
 */
public class ComplexPolynomial extends BasicPolynomial {

  /**
   * @param ele element to parse
   * @param goalScope where to find goals
   * @param variableScope where to find variables
   */
  public ComplexPolynomial(final Element ele,
                           final @UnknownInitialization GoalScope goalScope,
                           final @UnknownInitialization VariableScope variableScope) {
    super(ele, goalScope, variableScope);
  }

  /**
   * see {@link BasicPolynomial#BasicPolynomial()}.
   */
  public ComplexPolynomial() {
    super();
  }

}
