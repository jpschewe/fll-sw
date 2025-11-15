/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

/**
 * Reference to a goal in another score category.
 */
public class SubjectiveGoalRef implements Evaluatable<TeamScore>, Serializable {

  /**
   * XML tag name used for this class.
   */
  public static final String TAG_NAME = "subjectiveGoalRef";

  private static final String CATEGORY_NAME_ATTRIBUTE = "categoryName";

  /**
   * Determine the subjective category that is referenced
   */
  private static SubjectiveScoreCategory findCategory(final Element element,
                                                      final List<SubjectiveScoreCategory> subjectiveCategories)
      throws ChallengeValidationException {
    final String categoryName = element.getAttribute(CATEGORY_NAME_ATTRIBUTE);
    final Optional<SubjectiveScoreCategory> category = subjectiveCategories.stream()
                                                                           .filter(c -> categoryName.equals(c.getName()))
                                                                           .findAny();
    if (category.isEmpty()) {
      throw new ChallengeValidationException(String.format("Unable to find subjective category with name '%s' referenced in virtual subjective category",
                                                           categoryName));
    }
    return category.get();
  }

  /**
   * @param ele the XML element to parse to find the reference information
   * @param subjectiveCategories list of the subjective categories, this is used
   *          to find the category to use as the scope for the goal references
   * @throws ChallengeValidationException if the referenced subjective category
   *           cannot be found
   */
  public SubjectiveGoalRef(final Element ele,
                           final List<SubjectiveScoreCategory> subjectiveCategories)
      throws ChallengeValidationException {
    this.goalName = ele.getAttribute(GoalRef.GOAL_ATTRIBUTE);
    this.category = findCategory(ele, subjectiveCategories);
  }

  /**
   * @param goalName {@link #getGoalName()}
   * @param scope {@link #getCategory()}
   */
  public SubjectiveGoalRef(final String goalName,
                           final @UnknownInitialization SubjectiveScoreCategory scope) {
    this.goalName = goalName;
    this.category = scope;
  }

  private String goalName;

  /**
   * @return the name of the goal referenced
   */

  public String getGoalName() {
    return goalName;
  }

  /**
   * @param v see {@link #getGoalName()}
   */
  public void setGoalName(final String v) {
    goalName = v;
  }

  private final @NotOnlyInitialized SubjectiveScoreCategory category;

  /**
   * @return the subjective category the goal is in
   */
  public final SubjectiveScoreCategory getCategory() {
    return category;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    return getGoal().evaluate(teamScore);
  }

  /**
   * Resolve the goal name against the goal scope.
   * 
   * @return the goal
   * @throws ScopeException if the goal cannot be found
   */
  public AbstractGoal getGoal() throws ScopeException {
    return category.getGoal(goalName);
  }

  /**
   * @param doc used to create elements
   * @return XML version of this object
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    ele.setAttribute(GoalRef.GOAL_ATTRIBUTE, goalName);
    ele.setAttribute(CATEGORY_NAME_ATTRIBUTE, category.getName());
    return ele;
  }
}
