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

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Represents a rubric range.
 */
public class RubricRange implements Serializable {

  public static final String RUBRIC_TAG_NAME = "rubric";

  public static final String TAG_NAME = "range";

  public static final String TITLE_ATTRIBUTE = "title";

  public static final String MIN_ATTRIBUTE = "min";

  public static final String MAX_ATTRIBUTE = "max";

  public static final String DESCRIPTION_TAG_NAME = "description";

  public static final String SHORT_DESCRIPTION_ATTRIBUTE = "shortDescription";

  public RubricRange(final Element ele) {
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
    mMin = Integer.parseInt(ele.getAttribute(MIN_ATTRIBUTE));
    mMax = Integer.parseInt(ele.getAttribute(MAX_ATTRIBUTE));

    final NodelistElementCollectionAdapter descriptions = new NodelistElementCollectionAdapter(ele.getElementsByTagName(DESCRIPTION_TAG_NAME));
    if (descriptions.hasNext()) {
      final Element descriptionEle = descriptions.next();
      mDescription = removeExtraWhitespace(descriptionEle.getTextContent());
    } else {
      mDescription = null;
    }

    mShortDescription = ele.getAttribute(SHORT_DESCRIPTION_ATTRIBUTE);
  }

  /**
   * Default constructor. {@link #getDescription()} is null,
   * {@link #getShortDescription()} is null, {@link #getMin()} is 0,
   * {@Link #getMax()} is 1.
   * 
   * @param title the title of the range
   */
  public RubricRange(@Nonnull final String title) {
    mTitle = title;
    mMin = 0;
    mMax = 1;
    mDescription = null;
    mShortDescription = null;
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

  private String mTitle;

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(final String v) {
    mTitle = v;
  }

  private String mDescription;

  /**
   * The long description, may be null.
   * Extra whitespace is removed. All line endings
   * are removed.
   */
  public String getDescription() {
    return mDescription;
  }

  public void setDescription(final String v) {
    mDescription = removeExtraWhitespace(v);
  }

  private String mShortDescription;

  /**
   * Short description, typically 1 line. May be null.
   */
  public String getShortDescription() {
    return mShortDescription;
  }

  public void setShortDescription(final String v) {
    mShortDescription = v;
  }

  /**
   * Combine short description and description.
   * If short description doesn't end with a punctuation,
   * add a period. Handles null description.
   */
  public String getFullDescription() {
    final StringBuilder sb = new StringBuilder();
    final String shortDescription = getShortDescription();

    if (null != shortDescription
        && !shortDescription.trim().isEmpty()) {
      sb.append(shortDescription.trim());
      if (!shortDescription.endsWith(".")
          && !shortDescription.endsWith("!")
          && !shortDescription.endsWith("?")) {
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

  private int mMin;

  public int getMin() {
    return mMin;
  }

  public void setMin(final int v) {
    mMin = v;
  }

  private int mMax;

  public int getMax() {
    return mMax;
  }

  public void setMax(final int v) {
    mMax = v;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);
    ele.setAttribute(MIN_ATTRIBUTE, Integer.toString(mMin));
    ele.setAttribute(MAX_ATTRIBUTE, Integer.toString(mMax));

    if (null != mDescription) {
      final Element descriptionEle = doc.createElement(DESCRIPTION_TAG_NAME);
      descriptionEle.appendChild(doc.createTextNode(mDescription));
      ele.appendChild(descriptionEle);
    }

    ele.setAttribute(SHORT_DESCRIPTION_ATTRIBUTE, mShortDescription);

    return ele;
  }

}
