/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * The description of a tournament.
 */
public class ChallengeDescription implements Serializable {

  /**
   * It's assumed that this element is the root element from the Document from
   * {@link ChallengeParser#parse(java.io.Reader)}.
   * 
   * @param ele the challenge description
   */
  public ChallengeDescription(final Element ele) {
    mTitle = ele.getAttribute("title");
    mRevision = ele.getAttribute("revision");
    mWinner = XMLUtils.getWinnerCriteria(ele);

    if (ele.hasAttribute("copyright")) {
      mCopyright = ele.getAttribute("copyright");
    } else {
      mCopyright = null;
    }

    final Element performanceElement = (Element) ele.getElementsByTagName("Performance").item(0);
    mPerformance = new PerformanceScoreCategory(performanceElement);

    mSubjectiveCategories = new LinkedList<ScoreCategory>();
    for (final Element subjEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("subjectiveCategory"))) {
      final ScoreCategory subj = new ScoreCategory(subjEle);
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

  public void setCopyright(final String v) {
    mCopyright = v;
  }

  private String mTitle;

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(final String v) {
    mTitle = v;
  }

  private String mRevision;

  public String getRevision() {
    return mRevision;
  }

  public void setRevision(final String v) {
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

  private final List<ScoreCategory> mSubjectiveCategories;

  /**
   * @return unmodifiable list
   */
  public List<ScoreCategory> getSubjectiveCategories() {
    return mSubjectiveCategories;
  }

  /**
   * Add a subjective category to the end of the list.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   * 
   * @param v the category to add
   * @throws IllegalArgumentException if the argument is an instance of
   *           {@link PerformanceScoreCategory}
   */
  public void addSubjectiveCategory(final ScoreCategory v) throws IllegalArgumentException {
    if (v instanceof PerformanceScoreCategory) {
      throw new IllegalArgumentException("Cannot add a performance category to the subjective categories");
    }
    mSubjectiveCategories.add(v);
  }

  /**
   * Add a subjective category at the specified index.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   * 
   * @param v the category to add
   * @param index the index to add the category at
   * @throws IllegalArgumentException if the argument is an instance of
   *           {@link PerformanceScoreCategory}
   */
  public void addSubjectiveCategory(final int index,
                                    final ScoreCategory v)
      throws IndexOutOfBoundsException {
    if (v instanceof PerformanceScoreCategory) {
      throw new IllegalArgumentException("Cannot add a performance category to the subjective categories");
    }
    mSubjectiveCategories.add(index, v);
  }

  /**
   * Remove a subjective category.
   * 
   * @param v the category to remove
   * @return if the category was removed
   * @see List#remove(Object)
   * @throws IllegalArgumentException if the argument is an instance of
   *           {@Link PerformanceScoreCategory}
   */
  public boolean removeSubjectiveCategory(final ScoreCategory v) throws IllegalArgumentException {
    if (v instanceof PerformanceScoreCategory) {
      throw new IllegalArgumentException("Cannot remove a performance category from the subjective categories");
    }
    return mSubjectiveCategories.remove(v);
  }

  /**
   * Remove the subjective category at the specified index.
   * 
   * @param index the index to remove the element from
   * @return the category that was removed
   */
  public ScoreCategory removeSubjectiveCategory(final int index) throws IndexOutOfBoundsException {
    return mSubjectiveCategories.remove(index);
  }

  /**
   * @param name category name
   * @return the category or null if not found
   */
  public ScoreCategory getSubjectiveCategoryByName(final String name) {
    for (final ScoreCategory cat : mSubjectiveCategories) {
      if (name.equals(cat.getName())) {
        return cat;
      }
    }
    return null;
  }

}
