/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

public class Restriction extends BasicPolynomial {

  public Restriction(final Element ele,
                     final GoalScope goalScope) {
    super(ele, goalScope);

    mLowerBound = Double.valueOf(ele.getAttribute("lowerBound"));
    mUpperBound = Double.valueOf(ele.getAttribute("upperBound"));
    mMessage = ele.getAttribute("message");
  }

  private final double mLowerBound;

  public double getLowerBound() {
    return mLowerBound;
  }

  private final double mUpperBound;

  public double getUpperBound() {
    return mUpperBound;
  }

  private final String mMessage;

  public String getMessage() {
    return mMessage;
  }

}
