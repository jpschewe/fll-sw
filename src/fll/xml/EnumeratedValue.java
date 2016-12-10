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

public class EnumeratedValue implements Serializable {

  public static final String TITLE_ATTRIBUTE = "title";

  public static final String SCORE_ATTRIBUTE = "score";

  public static final String VALUE_ATTRIBUTE = "value";

  public static final String TAG_NAME = "value";

  public EnumeratedValue(final Element ele) {
    mScore = Double.valueOf(ele.getAttribute(SCORE_ATTRIBUTE));
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
    mValue = ele.getAttribute(VALUE_ATTRIBUTE);
  }

  private final double mScore;

  public double getScore() {
    return mScore;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private String mValue;

  public String getValue() {
    return mValue;
  }

  public Element toXml(final Document document) {
    final Element ele = document.createElement(TAG_NAME);
    ele.setAttribute(SCORE_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mScore));
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);
    ele.setAttribute(VALUE_ATTRIBUTE, mValue);
    return ele;
  }

}
