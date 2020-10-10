/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * One of the test statements in a tiebreaker.
 */
public class TiebreakerTest extends BasicPolynomial {

  /**
   * XML tag name for this class.
   */
  public static final String TAG_NAME = "test";

  /**
   * @param ele the XML element to parse
   * @param goalScope where to find goals
   */
  public TiebreakerTest(final Element ele,
                        final @UnderInitialization GoalScope goalScope) {
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

  /**
   * @return how the winner of the tie breaker is determined
   */
  public WinnerType getWinner() {
    return mWinner;
  }

  /**
   * @param v {@link #getWinner()}
   */
  public void setWinner(final WinnerType v) {
    mWinner = v;
  }

  /**
   * @param doc used to create elements
   * @return XML representation of this class
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    populateXml(doc, ele);
    ele.setAttribute(ChallengeParser.WINNER_ATTRIBUTE, mWinner.toString());
    return ele;
  }
}
