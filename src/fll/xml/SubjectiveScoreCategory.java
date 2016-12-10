/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Description of a subjective category.
 */
public class SubjectiveScoreCategory extends ScoreCategory {

  public static final String TAG_NAME = "subjectiveCategory";

  public static final String NAME_ATTRIBUTE = "name";

  public static final String TITLE_ATTRIBUTE = "title";

  public SubjectiveScoreCategory(final Element ele) {
    super(ele);
    mName = ele.getAttribute(NAME_ATTRIBUTE);
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
  }

  private final String mName;

  public String getName() {
    return mName;
  }

  private String mTitle;

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(final String v) {
    mTitle = v;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    populateXml(doc, ele);

    ele.setAttribute(NAME_ATTRIBUTE, mName);
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);

    return ele;
  }

}
