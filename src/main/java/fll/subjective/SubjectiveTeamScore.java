/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.text.ParseException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;

/**
 * Represents a team score from a score element used inside the subjective table
 * model.
 *
 * @author jpschewe
 */
/* package */class SubjectiveTeamScore extends TeamScore {

  /**
   * @param categoryDescription passed to superclass
   * @param scoreElement the score element that describes the team score
   * @throws ParseException if there is an error parsing the team number
   */
  /* package */ SubjectiveTeamScore(final Element scoreEle) throws ParseException {
    super(Utilities.getIntegerNumberFormat().parse(scoreEle.getAttribute("teamNumber")).intValue());
    this.scoreEle = scoreEle;
  }

  @Override
  public @Nullable String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      final Element subEle = SubjectiveUtils.getSubscoreElement(scoreEle, goalName);
      if (null == subEle) {
        return null;
      } else {
        final String value = subEle.getAttribute("value");
        if (value.isEmpty()) {
          return null;
        } else {
          return value;
        }
      }
    }
  }

  @Override
  public double getRawScore(final String goalName) {
    final Element subEle = SubjectiveUtils.getSubscoreElement(scoreEle, goalName);
    if (null == subEle) {
      return Double.NaN;
    } else {
      final String value = subEle.getAttribute("value");
      if (value.isEmpty()) {
        return Double.NaN;
      }
      try {
        return Utilities.getFloatingPointNumberFormat().parse(value).doubleValue();
      } catch (final ParseException pe) {
        throw new RuntimeException(pe);
      }
    }
  }

  @Override
  public boolean isNoShow() {
    if (!scoreExists()) {
      return false;
    } else {
      return Boolean.valueOf(scoreEle.getAttribute("NoShow"));
    }
  }

  /**
   * Always false as subjective scores are never byes.
   */
  @Override
  public boolean isBye() {
    return false;
  }

  @Override
  public boolean scoreExists() {
    return true;
  }

  private final Element scoreEle;
}
