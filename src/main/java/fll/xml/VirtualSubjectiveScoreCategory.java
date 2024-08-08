/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import fll.web.report.awards.AwardCategory;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * A category made up of goals from other categories.
 */
public class VirtualSubjectiveScoreCategory implements Serializable, Evaluatable, AwardCategory {

  /**
   * Name of the XML tag used for this class.
   */
  public static final String TAG_NAME = "virtualSubjectiveCategory";

  /**
   * @param ele the element to parse
   * @param subjectiveCategories see
   *          {@link SubjectiveGoalRef#SubjectiveGoalRef(Element, List)}
   */
  public VirtualSubjectiveScoreCategory(final Element ele,
                                        final List<SubjectiveScoreCategory> subjectiveCategories) {
    weight = Double.parseDouble(ele.getAttribute(ScoreCategory.WEIGHT_ATTRIBUTE));
    name = ele.getAttribute(SubjectiveScoreCategory.NAME_ATTRIBUTE);
    title = ele.getAttribute(SubjectiveScoreCategory.TITLE_ATTRIBUTE);

    for (final Element ele2 : new NodelistElementCollectionAdapter(ele.getElementsByTagName(SubjectiveGoalRef.TAG_NAME))) {
      final SubjectiveGoalRef ref = new SubjectiveGoalRef(ele2, subjectiveCategories);
      goalReferences.add(ref);
    }

  }

  /**
   * Creates a category with no {@link SubjectiveGoalRef}s and a weight of 1.
   * 
   * @param name see {@link #getName()}
   * @param title see {@link #getTitle()}
   */
  public VirtualSubjectiveScoreCategory(final String name,
                                        final String title) {
    weight = 1;
    this.name = name;
    this.title = title;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    } else if (teamScore.isNoShow()
        || teamScore.isBye()) {
      return 0D;
    }

    return goalReferences.stream().mapToDouble(g -> g.evaluate(teamScore)).sum();
  }

  private final List<SubjectiveGoalRef> goalReferences = new LinkedList<>();

  /**
   * @return unmodifiable list of {@link SubjectiveGoalRef}s that make up the
   *         virtual
   *         category
   */
  public List<SubjectiveGoalRef> getGoalReferences() {
    return Collections.unmodifiableList(goalReferences);
  }

  private String name;

  /**
   * @return the internal name of the category, used in the database
   */
  public String getName() {
    return name;
  }

  /**
   * @param v see {@link #getName()}
   */
  public void setName(final String v) {
    name = v;
  }

  private String title;

  @Override
  public String getTitle() {
    return title;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    title = v;
  }

  @Override
  public boolean getPerAwardGroup() {
    return true;
  }

  private double weight;

  /**
   * @return the weight for this category in the overall score
   */
  public double getWeight() {
    return weight;
  }

  /**
   * The maximum score for the category.
   * This implementation does a simple computation over
   * {@link #getGoalReferences()}.
   * 
   * @return The maximum score
   */
  public double getMaximumScore() {
    final double maximumScore = getGoalReferences().stream().map(GoalRef::getGoal)
                                                   .mapToDouble(AbstractGoal::getMaximumScore).sum();
    return maximumScore;
  }

  /**
   * @param v {@link #getWeight()}
   */
  public void setWeight(final double v) {
    weight = v;
  }

  /**
   * @param doc used to create elements
   * @return an XML element representing this subjective category
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    ele.setAttribute(SubjectiveScoreCategory.NAME_ATTRIBUTE, name);
    ele.setAttribute(SubjectiveScoreCategory.TITLE_ATTRIBUTE, title);
    ele.setAttribute(ScoreCategory.WEIGHT_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(weight));

    for (final SubjectiveGoalRef ref : goalReferences) {
      final Element refEle = ref.toXml(doc);
      ele.appendChild(refEle);
    }

    return ele;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + " "
        + this.getName();
  }

  @Override
  public int hashCode() {
    return getTitle().hashCode();
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (!o.getClass().equals(this.getClass())) {
      return false;
    } else {
      final VirtualSubjectiveScoreCategory other = (VirtualSubjectiveScoreCategory) o;
      return getTitle().equals(other.getTitle())
          && getPerAwardGroup() == other.getPerAwardGroup();
    }
  }

}
