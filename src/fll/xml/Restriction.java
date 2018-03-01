/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;

public class Restriction extends BasicPolynomial {

  public static final String TAG_NAME = "restriction";

  public static final String LOWER_BOUND_ATTRIBUTE = "lowerBound";

  public static final String UPPER_BOUND_ATTRIBUTE = "upperBound";

  public static final String MESSAGE_ATTRIBUTE = "message";

  public Restriction(final Element ele,
                     final GoalScope goalScope) {
    super(ele, goalScope);

    mLowerBound = Double.valueOf(ele.getAttribute(LOWER_BOUND_ATTRIBUTE));
    mUpperBound = Double.valueOf(ele.getAttribute(UPPER_BOUND_ATTRIBUTE));
    mMessage = ele.getAttribute(MESSAGE_ATTRIBUTE);
  }

  /**
   * Default constructor, lower bound is {@link Double#NEGATIVE_INFINITY}, upper
   * bound is {@link Double#POSITIVE_INFINITY}, message is null.
   */
  public Restriction(final GoalScope goalScope) {
    super(goalScope, null);
    mLowerBound = Double.NEGATIVE_INFINITY;
    mUpperBound = Double.POSITIVE_INFINITY;
    mMessage = null;
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

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    populateXml(doc, ele);

    ele.setAttribute(LOWER_BOUND_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mLowerBound));
    ele.setAttribute(UPPER_BOUND_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mUpperBound));
    ele.setAttribute(MESSAGE_ATTRIBUTE, mMessage);

    return ele;
  }

}
