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

import fll.util.FLLRuntimeException;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Represents a rubric range.
 */
public class RubricRange implements Serializable {

  /**
   * The XML tag name to write the rubric into.
   */
  public static final String RUBRIC_TAG_NAME = "rubric";

  /**
   * The XML tag to write the range into.
   */
  public static final String TAG_NAME = "range";

  /**
   * The XML attribute name to put the title in.
   */
  public static final String TITLE_ATTRIBUTE = "title";

  /**
   * The XML attribute to write the min value to.
   */
  public static final String MIN_ATTRIBUTE = "min";

  /**
   * The XML attribute to write the max value to.
   */
  public static final String MAX_ATTRIBUTE = "max";

  /**
   * The XML attribute to write the description to.
   */
  public static final String DESCRIPTION_TAG_NAME = GoalElement.DESCRIPTION_TAG_NAME;

  /**
   * The XML element to write the short description to.
   */
  public static final String SHORT_DESCRIPTION_ATTRIBUTE = "shortDescription";

  /**
   * @param ele the XML element to parse the range from
   */
  public RubricRange(final Element ele) {
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
    mMin = Integer.parseInt(ele.getAttribute(MIN_ATTRIBUTE));
    mMax = Integer.parseInt(ele.getAttribute(MAX_ATTRIBUTE));

    final NodelistElementCollectionAdapter descriptions = new NodelistElementCollectionAdapter(ele.getElementsByTagName(DESCRIPTION_TAG_NAME));
    if (descriptions.hasNext()) {
      final Element descriptionEle = descriptions.next();
      mDescription = ChallengeDescription.removeExtraWhitespace(descriptionEle.getTextContent());
    } else {
      mDescription = "";
    }

    mShortDescription = ele.getAttribute(SHORT_DESCRIPTION_ATTRIBUTE);
  }

  /**
   * Default constructor. {@link #getDescription()} is null,
   * {@link #getShortDescription()} is "", {@link #getMin()} is 0,
   * {@link #getMax()} is 1.
   *
   * @param title the title of the range
   */
  public RubricRange(@Nonnull final String title) {
    mTitle = title;
    mMin = 0;
    mMax = 1;
    mDescription = "";
    mShortDescription = "";
  }

  private String mTitle;

  /**
   * @return the title of the range.
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

  private String mDescription;

  /**
   * The long description.
   * Extra whitespace is removed. All line endings
   * are removed.
   *
   * @return the long description
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @param v see {@link #getDescription()}
   */
  public void setDescription(final String v) {
    mDescription = ChallengeDescription.removeExtraWhitespace(v);
  }

  private String mShortDescription;

  /**
   * @return Short description, typically 1 line.
   */
  public String getShortDescription() {
    return mShortDescription;
  }

  /**
   * @param v see {@link #getShortDescription()}
   */
  public void setShortDescription(final String v) {
    mShortDescription = v;
  }

  /**
   * Combine short description and description.
   * If short description doesn't end with a punctuation,
   * add a period. Handles null description.
   *
   * @return the full description to display
   */
  public String getFullDescription() {
    final StringBuilder sb = new StringBuilder();
    final String shortDescription = getShortDescription();

    if (!shortDescription.trim().isEmpty()) {
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

  /**
   * @return the minimum value for the range.
   */
  public int getMin() {
    return mMin;
  }

  /**
   * @param v see {@link #getMin()}
   */
  public void setMin(final int v) {
    mMin = v;
  }

  private int mMax;

  /**
   * @return the maximum value for the range
   */
  public int getMax() {
    return mMax;
  }

  /**
   * @param v see {@link #getMax()}
   */
  public void setMax(final int v) {
    mMax = v;
  }

  /**
   * @param doc the document to put the range in
   * @return an XML element with the information from this range
   */
  public Element toXml(final Document doc) {
    if (mMin > mMax) {
      throw new FLLRuntimeException("Minimum value must be less than maximum value");
    }

    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);
    ele.setAttribute(MIN_ATTRIBUTE, Integer.toString(mMin));
    ele.setAttribute(MAX_ATTRIBUTE, Integer.toString(mMax));

    if (!mDescription.isEmpty()) {
      final Element descriptionEle = doc.createElement(DESCRIPTION_TAG_NAME);
      descriptionEle.appendChild(doc.createTextNode(mDescription));
      ele.appendChild(descriptionEle);
    }

    ele.setAttribute(SHORT_DESCRIPTION_ATTRIBUTE, mShortDescription);

    return ele;
  }

}
