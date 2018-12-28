/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Element;

/**
 * One of the test statements in a tiebreaker.
 */
public class TiebreakerTest extends BasicPolynomial {

  public TiebreakerTest(final Element ele, final GoalScope goalScope) {
    super(ele, goalScope);
    mWinner = XMLUtils.getWinnerCriteria(ele);
  }

  private final WinnerType mWinner;

  public WinnerType getWinner() {
    return mWinner;
  }

}
