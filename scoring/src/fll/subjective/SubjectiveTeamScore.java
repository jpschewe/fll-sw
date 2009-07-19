/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.text.ParseException;

import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;

/**
 * Represents a team score from a score element used inside the subjective table
 * model.
 * 
 * @author jpschewe
 * @version $Revision$
 */
/* package */class SubjectiveTeamScore extends TeamScore {

  /**
   * @param categoryDescription passed to superclass
   * @param scoreElement the score element that describes the team score
   */
  public SubjectiveTeamScore(final Element categoryDescription, final Element scoreEle) throws ParseException {
    super(categoryDescription, Utilities.NUMBER_FORMAT_INSTANCE.parse(scoreEle.getAttribute("teamNumber")).intValue());
    _scoreEle = scoreEle;
  }

  @Override
  public String getEnumRawScore(final String goalName) {
    final String value = _scoreEle.getAttribute(goalName);
    if (null == value
        || "".equals(value)) {
      return null;
    } else {
      return value;
    }
  }

  @Override
  public Double getRawScore(final String goalName) {
    final String value = _scoreEle.getAttribute(goalName);
    if (null == value
        || "".equals(value)) {
      return null;
    } else {
      try {
        return Utilities.NUMBER_FORMAT_INSTANCE.parse(value).doubleValue();
      } catch (final ParseException pe) {
        throw new RuntimeException(pe);
      }
    }
  }

  @Override
  public boolean isNoShow() {
    return Boolean.valueOf(_scoreEle.getAttribute("NoShow"));
  }

  @Override
  public boolean scoreExists() {
    return true;
  }

  private final Element _scoreEle;
}
