/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Description of the performance.
 */
public class PerformanceScoreCategory extends ScoreCategory {

  public PerformanceScoreCategory(final Element ele) {
    super(ele, "performance", "Performance"); // element,name, title

    mMinimumScore = Double.valueOf(ele.getAttribute("minimumScore"));

    mRestrictions = new LinkedList<Restriction>();
    for (final Element restrictEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("restriction"))) {
      final Restriction restrict = new Restriction(restrictEle, this);
      mRestrictions.add(restrict);
    }

    mTiebreaker = new LinkedList<TiebreakerTest>();
    final NodeList tiebreakerElements = ele.getElementsByTagName("tiebreaker");
    if (0 != tiebreakerElements.getLength()) {
      final Element tiebreakerElement = (Element) tiebreakerElements.item(0);
      for (final Element testElement : new NodelistElementCollectionAdapter(tiebreakerElement.getChildNodes())) {
        final TiebreakerTest tie = new TiebreakerTest(testElement, this);
        mTiebreaker.add(tie);
      }
    }

  }

  private final Collection<Restriction> mRestrictions;

  /**
   * Get the restrictions.
   * 
   * @return unmodifiable collection
   */
  public Collection<Restriction> getRestrictions() {
    return Collections.unmodifiableCollection(mRestrictions);
  }

  /**
   * Add a restriction.
   * 
   * @param v the restriction to add
   */
  public void addRestriction(final Restriction v) {
    mRestrictions.add(v);
  }

  /**
   * Remove a restriction.
   * 
   * @param v the restriction to remove
   * @return if the restriction was removed
   */
  public boolean removeRestriction(final Restriction v) {
    return mRestrictions.remove(v);
  }

  private final List<TiebreakerTest> mTiebreaker;

  /**
   * Get the tiebreaker tests. These are checked in order.
   * 
   * @return unmodifiable list
   */
  public List<TiebreakerTest> getTiebreaker() {
    return Collections.unmodifiableList(mTiebreaker);
  }

  /**
   * Add a test to the end of the tiebreaker list.
   * 
   * @param v the test to add
   */
  public void addTiebreakerTest(final TiebreakerTest v) {
    mTiebreaker.add(v);
  }

  /**
   * Add a test at the specified index in the tiebreaker list.
   * 
   * @param index the index to add the test at
   * @param v the test to add
   */
  public void addTiebreakerTest(final int index,
                                final TiebreakerTest v)
      throws IndexOutOfBoundsException {
    mTiebreaker.add(index, v);
  }

  /**
   * Remove the specified test from the list of tiebreakers.
   * 
   * @param v the tiebreaker to remove
   * @return if the tiebreaker was removed
   */
  public boolean removeTiebreakerTest(final TiebreakerTest v) {
    return mTiebreaker.remove(v);
  }

  /**
   * Remove the tiebreaker test at the specified index.
   * 
   * @param index the index to remove at
   * @return the test that was removed
   */
  public TiebreakerTest removeTiebreakerTest(final int index) {
    return mTiebreaker.remove(index);
  }

  private double mMinimumScore;

  public double getMinimumScore() {
    return mMinimumScore;
  }

  public void setMinimumScore(final double v) {
    mMinimumScore = v;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    final double score = super.evaluate(teamScore);
    if (score < mMinimumScore
        && !teamScore.isNoShow()) {
      return mMinimumScore;
    } else {
      return score;
    }
  }

}
