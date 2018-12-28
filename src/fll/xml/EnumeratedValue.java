/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Element;

public class EnumeratedValue implements Serializable {

  public EnumeratedValue(final Element ele) {
    mScore = Double.valueOf(ele.getAttribute("score"));
    mTitle = ele.getAttribute("title");
    mValue = ele.getAttribute("value");
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

}
