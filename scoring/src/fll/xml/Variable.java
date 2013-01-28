/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

/**
 * A variable is a polynomial with a value.
 */
public class Variable extends BasicPolynomial {

  public Variable(final Element ele,
                  final GoalScope goalScope) {
    super(ele, goalScope);

    mName = ele.getAttribute("name");
  }

  private final String mName;

  public String getName() {
    return mName;
  }

}
