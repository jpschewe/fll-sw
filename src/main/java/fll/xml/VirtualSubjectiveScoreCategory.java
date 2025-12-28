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
import fll.scores.TeamScore;
import fll.web.report.awards.AwardCategory;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * A category made up of goals from other categories.
 */
public class VirtualSubjectiveScoreCategory implements Serializable, Evaluatable<TeamScore>, AwardCategory {

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
    if (teamScore.isNoShow()) {
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

  /**
   * @param v replace all goal references
   */
  public void setGoalReferences(final List<SubjectiveGoalRef> v) {
    goalReferences.clear();
    goalReferences.addAll(v);
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
    final double maximumScore = getGoalReferences().stream().map(SubjectiveGoalRef::getGoal)
                                                   .mapToDouble(AbstractGoal::getMaximumScore).sum();
    return maximumScore;
  }

  /**
   * Add the specified element to the end of the list.
   *
   * @param v the new reference
   */
  public void addGoalReference(final SubjectiveGoalRef v) {
    goalReferences.add(v);
  }

  /**
   * Add a reference at the specified index.
   *
   * @param index the index to add the reference at
   * @param v the reference to add
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void addGoalReference(final int index,
                               final SubjectiveGoalRef v)
      throws IndexOutOfBoundsException {
    goalReferences.add(index, v);
  }

  /**
   * Remove the specified reference from the list.
   *
   * @param v the reference to remove
   * @return if the reference was removed
   */
  public boolean removeGoalReference(final SubjectiveGoalRef v) {
    return goalReferences.remove(v);
  }

  /**
   * Remove the reference at the specified index.
   *
   * @param index the index of the reference to remove
   * @return the removed reference
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public SubjectiveGoalRef removeGoalReference(final int index) throws IndexOutOfBoundsException {
    return goalReferences.remove(index);
  }

  /**
   * @param v {@link #getWeight()}
   */
  public void setWeight(final double v) {
    weight = v;
  }

  /**
   * Determines the {@link ScoreType} for this category.
   * This is done by walking all of the references and checking their score type.
   * If any reference is floating point, then this category can have a floating
   * point score.
   *
   * @return not null
   */
  public ScoreType getScoreType() {
    for (final SubjectiveGoalRef ge : goalReferences) {
      if (ge.getGoal().getScoreType() == ScoreType.FLOAT) {
        return ScoreType.FLOAT;
      }
    }

    return ScoreType.INTEGER;
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

  @Override
  public boolean isRanked() {
    return true;
  }

}
