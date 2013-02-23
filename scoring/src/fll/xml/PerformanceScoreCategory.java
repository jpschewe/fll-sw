/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

/**
 * Description of the performance.
 */
public class PerformanceScoreCategory extends ScoreCategory {

  public PerformanceScoreCategory(final Element ele) {
    super(ele, "performance", "Performance"); // element,name, title

    mMinimumScore = Double.valueOf(ele.getAttribute("minimumScore"));

    final List<Restriction> restrictions = new LinkedList<Restriction>();
    for (final Element restrictEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("restriction"))) {
      final Restriction restrict = new Restriction(restrictEle, this);
      restrictions.add(restrict);
    }
    mRestrictions = Collections.unmodifiableList(restrictions);

    final List<TiebreakerTest> tiebreakers = new LinkedList<TiebreakerTest>();
    final Element tiebreakerElement = (Element) ele.getElementsByTagName("tiebreaker").item(0);
    for (final Element testElement : new NodelistElementCollectionAdapter(tiebreakerElement.getChildNodes())) {
      final TiebreakerTest tie = new TiebreakerTest(testElement, this);
      tiebreakers.add(tie);
    }
    mTiebreaker = Collections.unmodifiableList(tiebreakers);

  }

  private final List<Restriction> mRestrictions;

  public List<Restriction> getRestrictions() {
    return mRestrictions;
  }

  private final List<TiebreakerTest> mTiebreaker;

  public List<TiebreakerTest> getTiebreaker() {
    return mTiebreaker;
  }

  private final double mMinimumScore;

  public double getMinimumScore() {
    return mMinimumScore;
  }

}
