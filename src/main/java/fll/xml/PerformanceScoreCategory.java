/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Description of the performance.
 */
public class PerformanceScoreCategory extends ScoreCategory {

  /**
   * XML tag used for the category.
   */
  public static final String TAG_NAME = "Performance";

  /**
   * XML tag for tie breakers.
   */
  private static final String TIE_BREAKER_TAG_NAME = "tiebreaker";

  private static final String MINIMUM_SCORE_ATTRIBUTE = "minimumScore";

  /**
   * @param ele the element to parse
   */
  public PerformanceScoreCategory(final Element ele) {
    super(ele);

    mMinimumScore = Double.parseDouble(ele.getAttribute(MINIMUM_SCORE_ATTRIBUTE));

    mRestrictions = new LinkedList<>();
    for (final Element restrictEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(Restriction.TAG_NAME))) {
      final Restriction restrict = new Restriction(restrictEle, this);
      mRestrictions.add(restrict);
    }

    mTiebreaker = new LinkedList<>();
    final NodeList tiebreakerElements = ele.getElementsByTagName(TIE_BREAKER_TAG_NAME);
    if (0 != tiebreakerElements.getLength()) {
      final Element tiebreakerElement = (Element) castNonNull(tiebreakerElements.item(0));
      for (final Element testElement : new NodelistElementCollectionAdapter(tiebreakerElement.getChildNodes())) {
        final TiebreakerTest tie = new TiebreakerTest(testElement, this);
        mTiebreaker.add(tie);
      }
    }

  }

  /**
   * Default constructor has a {@link #getMinimumScore()} of 0, no
   * {@link #getRestrictions()} and no {@link #getTiebreaker()}.
   */
  public PerformanceScoreCategory() {
    super();
    mMinimumScore = 0;
    mRestrictions = new LinkedList<>();
    mTiebreaker = new LinkedList<>();
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

  /**
   * Replace the restrictions.
   *
   * @param v the new value
   */
  public void setRestrictions(final List<Restriction> v) {
    mRestrictions.clear();
    mRestrictions.addAll(v);
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
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           tiebreakers
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

  /**
   * Replace the tiebreakers.
   *
   * @param v the new value
   */
  public void setTiebreaker(final List<TiebreakerTest> v) {
    mTiebreaker.clear();
    mTiebreaker.addAll(v);
  }

  private double mMinimumScore;

  /**
   * @return the minimum score to store
   */
  public double getMinimumScore() {
    return mMinimumScore;
  }

  /**
   * @param v {@link #getMinimumScore()}
   */
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

  /**
   * @param doc used to create elements
   * @return XML representation of this object
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    populateXml(doc, ele);

    ele.setAttribute(MINIMUM_SCORE_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mMinimumScore));

    for (final Restriction restrict : mRestrictions) {
      final Element restrictEle = restrict.toXml(doc);
      ele.appendChild(restrictEle);
    }

    if (!mTiebreaker.isEmpty()) {
      final Element tiebreakerEle = doc.createElement(TIE_BREAKER_TAG_NAME);

      for (final TiebreakerTest test : mTiebreaker) {
        final Element testEle = test.toXml(doc);
        tiebreakerEle.appendChild(testEle);
      }

      ele.appendChild(tiebreakerEle);
    }

    return ele;
  }

  /**
   * Category name for {@link #getName()}.
   */
  public static final String CATEGORY_NAME = "performance";

  @Override
  public String getName() {
    return CATEGORY_NAME;
  }

}
