/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;

/**
 * An enumerated value inside a goal.
 */
public class EnumeratedValue implements Serializable {

  private static final String TITLE_ATTRIBUTE = "title";

  private static final String SCORE_ATTRIBUTE = "score";

  private static final String VALUE_ATTRIBUTE = "value";

  /**
   * Name of XML tag for values.
   */
  public static final String TAG_NAME = "value";

  /**
   * @param ele XML element used to get the values
   */
  public EnumeratedValue(final Element ele) {
    this(ele.getAttribute(TITLE_ATTRIBUTE), ele.getAttribute(VALUE_ATTRIBUTE),
         Double.valueOf(ele.getAttribute(SCORE_ATTRIBUTE)));
  }

  /**
   * @param title see {@link #getTitle()}
   * @param value see {@link #getValue()}
   * @param score see {@link #getScore()}
   */
  public EnumeratedValue(final String title,
                         final String value,
                         final double score) {
    mScore = score;
    mTitle = title;
    mValue = value;
  }

  private double mScore;

  /**
   * @return the score associated with this value
   */
  public double getScore() {
    return mScore;
  }

  /**
   * @param v see {@link #getScore()}
   */
  public void setScore(final double v) {
    mScore = v;
  }

  private String mTitle;

  /**
   * @return the text to display for this value
   */
  public String getTitle() {
    return mTitle;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    mTitle = v;
  }

  private String mValue;

  /**
   * @return the value to store in the database, this needs to be a database name
   *         per the XML schema
   */
  public String getValue() {
    return mValue;
  }

  /**
   * @param v see {@link #getValue()}
   */
  public void setValue(final String v) {
    mValue = v;
  }

  /**
   * @param document used to create elements
   * @return the XML element that represents this enumerated value
   */
  public Element toXml(final Document document) {
    final Element ele = document.createElement(TAG_NAME);
    ele.setAttribute(SCORE_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mScore));
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);
    ele.setAttribute(VALUE_ATTRIBUTE, mValue);
    return ele;
  }

}
