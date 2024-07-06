/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Reference to a goal in another score category.
 */
public class SubjectiveGoalRef extends GoalRef {

  /**
   * XML tag name used for this class.
   */
  public static final String TAG_NAME = "subjectiveGoalRef";

  private static final String CATEGORY_NAME_ATTRIBUTE = "categoryName";

  /**
   * @param ele the XML element to parse to find the reference information
   * @param scope {@link #getCategory()}
   */
  public SubjectiveGoalRef(final Element ele,
                           @UnknownInitialization final SubjectiveScoreCategory scope) {
    super(ele, scope);
    this.category = scope;
  }

  /**
   * @param goalName {@link #getGoalName()}
   * @param scope {@link #getCategory()}
   * @param scoreType {@Link #getScoreType()}
   */
  public SubjectiveGoalRef(final String goalName,
                           @UnknownInitialization final SubjectiveScoreCategory scope,
                           final GoalScoreType scoreType) {
    super(goalName, scope, scoreType);
    this.category = scope;
  }

  private final SubjectiveScoreCategory category;

  /**
   * @return the subjective category the goal is in
   */
  public final SubjectiveScoreCategory getCategory() {
    return category;
  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = internalToXml(doc, TAG_NAME);
    ele.setAttribute(CATEGORY_NAME_ATTRIBUTE, category.getName());
    return ele;
  }
}
