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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import fll.web.report.awards.AwardCategory;
import fll.xml.SubjectiveScoreCategory.Nominates;

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
   */
  public VirtualSubjectiveScoreCategory(final Element ele) {
    weight = Double.parseDouble(ele.getAttribute(ScoreCategory.WEIGHT_ATTRIBUTE));
  }

  /**
   * Creates a category with no {@link SubjectiveGoalRef}s and a weight of 1.
   */
  public VirtualSubjectiveScoreCategory() {
    weight = 1;
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

  @Override
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

}
