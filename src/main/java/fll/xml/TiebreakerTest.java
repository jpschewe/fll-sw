/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * One of the test statements in a tiebreaker.
 */
public class TiebreakerTest extends BasicPolynomial {

  public static final String TAG_NAME = "test";

  public TiebreakerTest(final Element ele,
                        final GoalScope goalScope) {
    super(ele, goalScope);
    mWinner = ChallengeParser.getWinnerCriteria(ele);
  }

  /**
   * Default constructor uses {@link WinnerType#HIGH} as {@link #getWinner()}.
   */
  public TiebreakerTest() {
    super();
    mWinner = WinnerType.HIGH;
  }

  private WinnerType mWinner;

  public WinnerType getWinner() {
    return mWinner;
  }

  public void setWinner(final WinnerType v) {
    mWinner = v;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    populateXml(doc, ele);
    ele.setAttribute(ChallengeParser.WINNER_ATTRIBUTE, mWinner.toString());
    return ele;
  }
}
