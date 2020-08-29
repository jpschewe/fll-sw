/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * A category that doesn't have a score.
 */
public class NonNumericCategory implements Serializable {

  /**
   * Name of the XML tag used for this class.
   */
  public static final String TAG_NAME = "nonNumericCategory";

  private static final String PER_AWARD_GROUP_ATTRIBUTE = "perAwardGroup";

  /**
   * Parse the object from an XML element.
   * 
   * @param ele the XML element to parse
   */
  public NonNumericCategory(final Element ele) {
    title = ele.getAttribute(ChallengeDescription.TITLE_ATTRIBUTE);
    perAwardGroup = Boolean.valueOf(ele.getAttribute(PER_AWARD_GROUP_ATTRIBUTE));

    final NodelistElementCollectionAdapter elements = new NodelistElementCollectionAdapter(ele.getElementsByTagName(GoalElement.DESCRIPTION_TAG_NAME));
    if (elements.hasNext()) {
      final Element descriptionEle = elements.next();
      description = ChallengeDescription.removeExtraWhitespace(descriptionEle.getTextContent());
    } else {
      description = "";
    }

  }

  /**
   * @param title see {@link #getTitle()}
   * @param perAwardGroup see {@link #getPerAwardGroup()}
   */
  public NonNumericCategory(final String title,
                            final boolean perAwardGroup) {
    this.title = title;
    this.perAwardGroup = perAwardGroup;
  }

  private String title;

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    title = v;
  }

  private boolean perAwardGroup = true;

  /**
   * @return if the winners are per award group, otherwise per tournament
   */
  public boolean getPerAwardGroup() {
    return perAwardGroup;
  }

  /**
   * @param v see {@link #getPerAwardGroup()}
   */
  public void setPerAwardGroup(final boolean v) {
    perAwardGroup = v;
  }

  private String description;

  /**
   * @return description of the category
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param v see {@link #getDescription()}
   */
  public void setDescription(final String v) {
    description = ChallengeDescription.removeExtraWhitespace(v);
  }

  /**
   * @param doc the XML document used to create elements
   * @return an XML element representing the current state of this object
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(PER_AWARD_GROUP_ATTRIBUTE, Boolean.toString(perAwardGroup));
    ele.setAttribute(ChallengeDescription.TITLE_ATTRIBUTE, title);

    if (null != description) {
      final Element descriptionEle = doc.createElement(RubricRange.DESCRIPTION_TAG_NAME);
      descriptionEle.appendChild(doc.createTextNode(description));
      ele.appendChild(descriptionEle);
    }

    return ele;
  }

}
