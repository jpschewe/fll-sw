/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

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

    final List<ScoreCategory> subjCats = new LinkedList<ScoreCategory>();
    for (final Element subjEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("subjectiveCategory"))) {
      final ScoreCategory subj = new ScoreCategory(subjEle);
      subjCats.add(subj);
    }
    mSubjectiveCategories = Collections.unmodifiableList(subjCats);
  }

  private final String mCopyright;

  /**
   * Copyright statement for the challenge.
   * 
   * @return the statement or null if there is no copyright
   */
  public String getCopyright() {
    return mCopyright;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final String mRevision;

  public String getRevision() {
    return mRevision;
  }

  private final WinnerType mWinner;

  public WinnerType getWinner() {
    return mWinner;
  }

  private final PerformanceScoreCategory mPerformance;

  public PerformanceScoreCategory getPerformance() {
    return mPerformance;
  }

  private final List<ScoreCategory> mSubjectiveCategories;

  /**
   * @return unmodifiable list
   */
  public List<ScoreCategory> getSubjectiveCategories() {
    return mSubjectiveCategories;
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
