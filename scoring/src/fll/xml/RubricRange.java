/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

/**
 * Represents a rubric range.
 */
public class RubricRange implements Serializable {

  public RubricRange(final Element ele) {
    mTitle = ele.getAttribute("title");
    mMin = Integer.parseInt(ele.getAttribute("min"));
    mMax = Integer.parseInt(ele.getAttribute("max"));

    final NodelistElementCollectionAdapter descriptions = new NodelistElementCollectionAdapter(
                                                                                               ele.getElementsByTagName("description"));
    if (descriptions.hasNext()) {
      final Element descriptionEle = descriptions.next();
      mDescription = descriptionEle.getTextContent();
    } else {
      mDescription = null;
    }
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final String mDescription;

  public String getDescription() {
    return mDescription;
  }

  private final int mMin;

  public int getMin() {
    return mMin;
  }

  private final int mMax;

  public int getMax() {
    return mMax;
  }

}
