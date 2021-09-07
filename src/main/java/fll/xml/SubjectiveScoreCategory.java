/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Description of a subjective category.
 */
public class SubjectiveScoreCategory extends ScoreCategory implements Category {

  /**
   * Name of the XML tag used for this class.
   */
  public static final String TAG_NAME = "subjectiveCategory";

  /**
   * XML attribute to store the name.
   */
  /* package */ static final String NAME_ATTRIBUTE = "name";

  private static final String TITLE_ATTRIBUTE = ChallengeDescription.TITLE_ATTRIBUTE;

  /**
   * The XML attribute to write the description to.
   */
  private static final String SCORE_SHEET_INSTRUCTIONS_TAG_NAME = "scoreSheetInstructions";

  private static final String DEFAULT_SCORE_SHEET_TEXT = "Directions: For each skill area, clearly mark the box that best describes the team's accomplishments.  "
      + "If the team does not demonstrate skill in a particular area, then put an 'X' in the first box for Not Demonstrated (ND).  "
      + "Please provide as many written comments as you can to acknowledge each teams's hard work and to help teams improve. ";

  /**
   * Construct from the provided XML element.
   *
   * @param ele where to get the information from
   */
  public SubjectiveScoreCategory(final Element ele) {
    super(ele);
    mName = ele.getAttribute(NAME_ATTRIBUTE);
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);

    final NodelistElementCollectionAdapter elements = new NodelistElementCollectionAdapter(ele.getElementsByTagName(SCORE_SHEET_INSTRUCTIONS_TAG_NAME));
    if (elements.hasNext()) {
      final Element descriptionEle = elements.next();
      scoreSheetInstructions = ChallengeDescription.removeExtraWhitespace(descriptionEle.getTextContent());
    } else {
      scoreSheetInstructions = DEFAULT_SCORE_SHEET_TEXT;
    }

    final NodelistElementCollectionAdapter nominatesElements = new NodelistElementCollectionAdapter(ele.getElementsByTagName(Nominates.TAG_NAME));
    for (final Element e : nominatesElements) {
      final Nominates nominates = new Nominates(e);
      this.nominates.add(nominates);
    }

  }

  /**
   * Default constructor when no XML element is available.
   *
   * @param name see {@link #getName()}
   * @param title see {@link #getTitle()}
   */
  public SubjectiveScoreCategory(final String name,
                                 final String title) {
    super();
    mName = name;
    mTitle = title;
    scoreSheetInstructions = DEFAULT_SCORE_SHEET_TEXT;
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

  @Override
  public String getTitle() {
    return mTitle;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    mTitle = v;
  }

  private String scoreSheetInstructions;

  /**
   * @return instructions for the judges
   */
  public String getScoreSheetInstructions() {
    return scoreSheetInstructions;
  }

  /**
   * @param v see {@link #getScoreSheetInstructions()}
   */
  public void setScoreSheetInstructions(final String v) {
    this.scoreSheetInstructions = v;
  }

  private final LinkedList<Nominates> nominates = new LinkedList<>();

  /**
   * @return read-only collection of the non-numeric category names that this
   *         category can nominate teams for in the order specified in the
   *         challenge description
   */
  public List<Nominates> getNominates() {
    return Collections.unmodifiableList(nominates);
  }

  /**
   * @param v the new value {@link #getNominates()}
   */
  public void setNominates(final Set<Nominates> v) {
    this.nominates.clear();
    this.nominates.addAll(v);
  }

  /**
   * @param v the value to add
   * @return see {@link Set#add(Object)}
   */
  public boolean addNominates(final Nominates v) {
    return this.nominates.add(v);
  }

  /**
   * @param v the value to remove
   * @return see {@link Set#remove(Object)}
   */
  public boolean removeNominates(final Nominates v) {
    return this.nominates.remove(v);
  }

  /**
   * @param doc used to create elements
   * @return an XML element representing this subjective category
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element scoreSheetInstructionsEle = doc.createElement(SCORE_SHEET_INSTRUCTIONS_TAG_NAME);
    scoreSheetInstructionsEle.appendChild(doc.createTextNode(scoreSheetInstructions));
    ele.appendChild(scoreSheetInstructionsEle);

    populateXml(doc, ele);

    ele.setAttribute(NAME_ATTRIBUTE, mName);
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);

    for (final Nominates nom : this.nominates) {
      final Element e = nom.toXml(doc);
      ele.appendChild(e);
    }

    return ele;
  }

  /**
   * Represents a subjective category being able to nominate teams for a
   * non-numeric category.
   */
  public static final class Nominates implements Serializable {

    private static final String TAG_NAME = "nominates";

    private static final String NON_NUMERIC_CATEGORY_REF_ATTRIBUTE = "nonNumericCategoryTitle";

    /**
     * @param ele XML element to read information from
     */
    public Nominates(final Element ele) {
      this.nonNumericCategoryTitle = ele.getAttribute(NON_NUMERIC_CATEGORY_REF_ATTRIBUTE);
    }

    /**
     * @param nonNumericCategoryTitle see {@link #getNonNumericCategoryTitle()}
     */
    public Nominates(final String nonNumericCategoryTitle) {
      this.nonNumericCategoryTitle = nonNumericCategoryTitle;
    }

    private String nonNumericCategoryTitle;

    /**
     * @return the {@link NonNumericCategory#getTitle()} for the referenced
     *         non-numeric category
     */
    public String getNonNumericCategoryTitle() {
      return nonNumericCategoryTitle;
    }

    /**
     * @param v see {@link #getNonNumericCategoryTitle()}
     */
    public void setNonNumericCategoryName(final String v) {
      this.nonNumericCategoryTitle = v;
    }

    /**
     * @param doc used to create elements
     * @return an XML element representing this object
     */
    public Element toXml(final Document doc) {
      final Element ele = doc.createElement(Nominates.TAG_NAME);

      ele.setAttribute(Nominates.NON_NUMERIC_CATEGORY_REF_ATTRIBUTE, nonNumericCategoryTitle);

      return ele;
    }

    @Override
    public int hashCode() {
      return nonNumericCategoryTitle.hashCode();
    }

    @Override
    @EnsuresNonNullIf(expression = "#1", result = true)
    public boolean equals(final @Nullable Object o) {
      if (null == o) {
        return false;
      } else if (this == o) {
        return true;
      } else if (Nominates.class.equals(o.getClass())) {
        final Nominates other = (Nominates) o;
        return nonNumericCategoryTitle.equals(other.nonNumericCategoryTitle);
      } else {
        return false;
      }
    }

  }

  @Override
  public boolean getPerAwardGroup() {
    return true;
  }

}
