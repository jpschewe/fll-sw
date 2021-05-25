/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;

/**
 * Representation of a restriction in the challenge.
 */
public class Restriction extends BasicPolynomial {

  /**
   * XML tag name for this class.
   */
  public static final String TAG_NAME = "restriction";

  private static final String LOWER_BOUND_ATTRIBUTE = "lowerBound";

  private static final String UPPER_BOUND_ATTRIBUTE = "upperBound";

  private static final String MESSAGE_ATTRIBUTE = "message";

  /**
   * @param ele the element to parse
   * @param goalScope where to find goals
   */
  public Restriction(final Element ele,
                     final @UnknownInitialization GoalScope goalScope) {
    super(ele, goalScope);

    mLowerBound = Double.parseDouble(ele.getAttribute(LOWER_BOUND_ATTRIBUTE));
    mUpperBound = Double.parseDouble(ele.getAttribute(UPPER_BOUND_ATTRIBUTE));
    mMessage = ele.getAttribute(MESSAGE_ATTRIBUTE);
  }

  /**
   * Default constructor, lower bound is {@link Double#NEGATIVE_INFINITY}, upper
   * bound is {@link Double#POSITIVE_INFINITY}, message is null.
   */
  public Restriction() {
    super();
    mLowerBound = Double.NEGATIVE_INFINITY;
    mUpperBound = Double.POSITIVE_INFINITY;
    mMessage = "";
  }

  private double mLowerBound;

  /**
   * @return lower bound
   */
  public double getLowerBound() {
    return mLowerBound;
  }

  /**
   * @param v {@link #getLowerBound()}
   */
  public void setLowerBound(final double v) {
    mLowerBound = v;
  }

  private double mUpperBound;

  /**
   * @return upper bound
   */
  public double getUpperBound() {
    return mUpperBound;
  }

  /**
   * @param v {@link #getUpperBound()}
   */
  public void setUpperBound(final double v) {
    mUpperBound = v;
  }

  private String mMessage;

  /**
   * @return message to display when restriction is violated
   */
  public String getMessage() {
    return mMessage;
  }

  /**
   * @param v {@link #getMessage()}
   */
  public void setMessage(final String v) {
    mMessage = v;
  }

  /**
   * @param doc used to create elements
   * @return XML element representing this restriction
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    populateXml(doc, ele);

    ele.setAttribute(LOWER_BOUND_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mLowerBound));
    ele.setAttribute(UPPER_BOUND_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mUpperBound));
    ele.setAttribute(MESSAGE_ATTRIBUTE, mMessage);

    return ele;
  }

}
