/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

/**
 * A polynomial that can reference variables.
 */
public class ComplexPolynomial extends BasicPolynomial {

  public ComplexPolynomial(final Element ele,
                           final GoalScope goalScope,
                           final VariableScope variableScope) {
    super(ele, goalScope, variableScope);
  }

}
