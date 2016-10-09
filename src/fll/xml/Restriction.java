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

  private double mLowerBound;

  public double getLowerBound() {
    return mLowerBound;
  }
  
  public void setLowerBound(final double v) {
    mLowerBound = v;
  }

  private double mUpperBound;

  public double getUpperBound() {
    return mUpperBound;
  }

  public void setUpperBound(final double v) {
    mUpperBound = v;
  }
  
  private String mMessage;

  public String getMessage() {
    return mMessage;
  }

  public void setMessage(final String v) {
    mMessage = v;
  }

}
