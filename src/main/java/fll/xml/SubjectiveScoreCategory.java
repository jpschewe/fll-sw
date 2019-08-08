/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Description of a subjective category.
 */
public class SubjectiveScoreCategory extends ScoreCategory {

  public static final String TAG_NAME = "subjectiveCategory";

  public static final String NAME_ATTRIBUTE = "name";

  public static final String TITLE_ATTRIBUTE = "title";

  /**
   * Construct from the provided XML element.
   *
   * @param ele where to get the information from
   */
  public SubjectiveScoreCategory(final Element ele) {
    super(ele);
    mName = ele.getAttribute(NAME_ATTRIBUTE);
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
  }

  /**
   * Default constructor when no XML element is available.
   *
   * @param name see {@link #getName()}
   * @param title see {@link #getTitle()}
   */
  public SubjectiveScoreCategory(@Nonnull final String name,
                                 @Nonnull final String title) {
    super();
    mName = name;
    mTitle = title;
  }

  private String mName;

  @Override
  public String getName() {
    return mName;
  }

  /**
   * @param v see {@link #getName()}
   */
  public void setName(final String v) {
    mName = v;
  }

  private String mTitle;

  /**
   * @return the title of the category
   */
  @Nonnull
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
