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

import fll.Utilities;

public class EnumeratedValue implements Serializable {

  public static final String TITLE_ATTRIBUTE = "title";

  public static final String SCORE_ATTRIBUTE = "score";

  public static final String VALUE_ATTRIBUTE = "value";

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
  public EnumeratedValue(@Nonnull final String title,
                         @Nonnull final String value,
                         final double score) {
    mScore = score;
    mTitle = title;
    mValue = value;
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
