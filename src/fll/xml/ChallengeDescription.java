/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * The description of a tournament.
 */
public class ChallengeDescription implements Serializable {

  public static final String TITLE_ATTRIBUTE = "title";

  public static final String REVISION_ATTRIBUTE = "revision";

  public static final String COPYRIGHT_ATTRIBUTE = "copyright";

  public static final String SCHEMA_VERSION_ATTRIBUTE = "schemaVersion";

  /**
   * It's assumed that this element is the root element from the Document from
   * {@link ChallengeParser#parse(java.io.Reader)}.
   * 
   * @param ele the challenge description
   */
  public ChallengeDescription(final Element ele) {
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
    mRevision = ele.getAttribute(REVISION_ATTRIBUTE);
    mWinner = XMLUtils.getWinnerCriteria(ele);

    if (ele.hasAttribute(COPYRIGHT_ATTRIBUTE)) {
      mCopyright = ele.getAttribute(COPYRIGHT_ATTRIBUTE);
    } else {
      mCopyright = null;
    }

    final Element performanceElement = (Element) ele.getElementsByTagName(PerformanceScoreCategory.TAG_NAME).item(0);
    mPerformance = new PerformanceScoreCategory(performanceElement);

    mSubjectiveCategories = new LinkedList<>();
    for (final Element subjEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(SubjectiveScoreCategory.TAG_NAME))) {
      final SubjectiveScoreCategory subj = new SubjectiveScoreCategory(subjEle);
      mSubjectiveCategories.add(subj);
    }
  }

  private String mCopyright;

  /**
   * Copyright statement for the challenge.
   * 
   * @return the statement or null if there is no copyright
   */
  public String getCopyright() {
    return mCopyright;
  }

  /**
   * @param v see {@link #getCopyright()}
   */
  public void setCopyright(final String v) {
    mCopyright = v;
  }

  private String mTitle;

  /**
   * The name of the challenge description, it cannot be null, but may be the
   * empty string, although this is not advised.
   * 
   * @return the title of the tournament
   */
  @Nonnull
  public String getTitle() {
    return mTitle;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(@Nonnull final String v) {
    mTitle = v;
  }

  private String mRevision;

  /**
   * This is used to keep track of changes, it can be an empty string, but not
   * null.
   * 
   * @return the revision of the description
   */
  @Nonnull
  public String getRevision() {
    return mRevision;
  }

  /**
   * @param v see {@link #setRevision(String)}
   */
  public void setRevision(@Nonnull final String v) {
    mRevision = v;
  }

  private WinnerType mWinner;

  public WinnerType getWinner() {
    return mWinner;
  }

  public void setWinner(final WinnerType v) {
    mWinner = v;
  }

  private PerformanceScoreCategory mPerformance;

  public PerformanceScoreCategory getPerformance() {
    return mPerformance;
  }

  /**
   * Since {@link PerformanceScoreCategory} is mutable, this should not be
   * needed except to add or remove the performance element.
   * 
   * @param v setting the value to null will remove the element
   */
  public void setPerformance(final PerformanceScoreCategory v) {
    mPerformance = v;
  }

  private final List<SubjectiveScoreCategory> mSubjectiveCategories;

  /**
   * @return unmodifiable list
   */
  public List<SubjectiveScoreCategory> getSubjectiveCategories() {
    return mSubjectiveCategories;
  }

  /**
   * Add a subjective category to the end of the list.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   * 
   * @param v the category to add
   */
  public void addSubjectiveCategory(final SubjectiveScoreCategory v) {
    mSubjectiveCategories.add(v);
  }

  /**
   * Add a subjective category at the specified index.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   * 
   * @param v the category to add
   * @param index the index to add the category at
   */
  public void addSubjectiveCategory(final int index,
                                    final SubjectiveScoreCategory v)
      throws IndexOutOfBoundsException {
    mSubjectiveCategories.add(index, v);
  }

  /**
   * Remove a subjective category.
   * 
   * @param v the category to remove
   * @return if the category was removed
   * @see List#remove(Object)
   */
  public boolean removeSubjectiveCategory(final SubjectiveScoreCategory v) {
    return mSubjectiveCategories.remove(v);
  }

  /**
   * Remove the subjective category at the specified index.
   * 
   * @param index the index to remove the element from
   * @return the category that was removed
   */
  public SubjectiveScoreCategory removeSubjectiveCategory(final int index) throws IndexOutOfBoundsException {
    return mSubjectiveCategories.remove(index);
  }

  /**
   * @param name category name
   * @return the category or null if not found
   */
  public SubjectiveScoreCategory getSubjectiveCategoryByName(final String name) {
    for (final SubjectiveScoreCategory cat : mSubjectiveCategories) {
      if (name.equals(cat.getName())) {
        return cat;
      }
    }
    return null;
  }

  /**
   * Create the XML for the current state of this challenge description.
   * 
   * @return a non-null document.
   */
  public Document toXml() {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
    final Element fll = document.createElementNS(null, "fll");
    document.appendChild(fll);

    fll.setAttribute(TITLE_ATTRIBUTE, mTitle);
    fll.setAttribute(REVISION_ATTRIBUTE, mRevision);
    fll.setAttribute(XMLUtils.WINNER_ATTRIBUTE, mWinner.toString());
    fll.setAttribute(SCHEMA_VERSION_ATTRIBUTE, String.valueOf(ChallengeParser.CURRENT_SCHEMA_VERSION));

    if (null != mCopyright) {
      fll.setAttribute(COPYRIGHT_ATTRIBUTE, mCopyright);
    }

    final Element performanceElement = mPerformance.toXml(document);
    fll.appendChild(performanceElement);

    for (final SubjectiveScoreCategory cat : mSubjectiveCategories) {
      final Element subjEle = cat.toXml(document);
      fll.appendChild(subjEle);
    }

    return document;
  }

}
