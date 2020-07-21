/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Description of a subjective category.
 */
public class SubjectiveScoreCategory extends ScoreCategory {

  /**
   * Name of tag for a subjective category XML element.
   */
  public static final String TAG_NAME = "subjectiveCategory";

  private static final String NAME_ATTRIBUTE = "name";

  private static final String TITLE_ATTRIBUTE = "title";

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

  /**
   * @return the title of the category
   */
  @Nonnull
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

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element scoreSheetInstructionsEle = doc.createElement(SCORE_SHEET_INSTRUCTIONS_TAG_NAME);
    scoreSheetInstructionsEle.appendChild(doc.createTextNode(scoreSheetInstructions));
    ele.appendChild(scoreSheetInstructionsEle);

    populateXml(doc, ele);

    ele.setAttribute(NAME_ATTRIBUTE, mName);
    ele.setAttribute(TITLE_ATTRIBUTE, mTitle);

    return ele;
  }

}
