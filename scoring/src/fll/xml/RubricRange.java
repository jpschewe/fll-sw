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

    final NodelistElementCollectionAdapter descriptions = new NodelistElementCollectionAdapter(ele.getElementsByTagName("description"));
    if (descriptions.hasNext()) {
      final Element descriptionEle = descriptions.next();
      mDescription = removeExtraWhitespace(descriptionEle.getTextContent());
    } else {
      mDescription = null;
    }

    mShortDescription = ele.getAttribute("shortDescription");
  }

  private static String removeExtraWhitespace(final String str) {
    if (null == str) {
      return str;
    }

    String result = str.trim();
    result = result.replace('\r', ' ');
    result = result.replace('\n', ' ');
    result = result.replaceAll("\\s+", " ");
    return result;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final String mDescription;

  /**
   * The long description, may be null.
   * Extra whitespace is removed. All line endings
   * are removed.
   */
  public String getDescription() {
    return mDescription;
  }

  private final String mShortDescription;

  /**
   * Short description, typically 1 line. May be null.
   */
  public String getShortDescription() {
    return mShortDescription;
  }

  /**
   * Combine short description and description.
   * If short description doesn't end with a punctuation,
   * add a period. Handles null description.
   */
  public String getFullDescription() {
    final StringBuilder sb = new StringBuilder();
    final String shortDescription = getShortDescription();

    if (null != shortDescription && !shortDescription.trim().isEmpty()) {
      sb.append(shortDescription.trim());
      if (!shortDescription.endsWith(".")
          && !shortDescription.endsWith("!") && !shortDescription.endsWith("?")) {
        sb.append(".");
      }
    }

    final String description = getDescription();
    if (null != description) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(description.trim());
    }

    return sb.toString();
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
