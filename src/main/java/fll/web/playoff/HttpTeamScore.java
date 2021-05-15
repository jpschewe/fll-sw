/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import javax.servlet.http.HttpServletRequest;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A team score in an HTTP request.
 */
public final class HttpTeamScore extends TeamScore {

  /**
   * @param teamNumber {@link #getTeamNumber()}
   * @param runNumber {@link #getRunNumber()}
   * @param request used to read the goal scores
   */
  public HttpTeamScore(final int teamNumber,
                       final int runNumber,
                       final HttpServletRequest request) {
    super(teamNumber, runNumber);
    this.request = request;
  }

  @Override
  public @Nullable String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      return request.getParameter(goalName);
    }
  }

  @Override
  public double getRawScore(final String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else {
      final String value = request.getParameter(goalName);
      if (null == value) {
        return Double.NaN;
      } else {
        return Double.parseDouble(value);
      }
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#isNoShow()
   */
  @Override
  public boolean isNoShow() {
    if (!scoreExists()) {
      return false;
    } else {
      final String noShow = request.getParameter("NoShow");
      if (null == noShow) {
        throw new RuntimeException("Missing parameter: NoShow");
      }
      return noShow.equalsIgnoreCase("true")
          || noShow.equalsIgnoreCase("t")
          || noShow.equals("1");
    }
  }

  @Override
  public boolean isBye() {
    if (NON_PERFORMANCE_RUN_NUMBER == getRunNumber()) {
      return false;
    } else if (!scoreExists()) {
      return false;
    } else {
      final String noShow = request.getParameter("Bye");
      if (null == noShow) {
        throw new RuntimeException("Missing parameter: Bye");
      }
      return noShow.equalsIgnoreCase("true")
          || noShow.equalsIgnoreCase("t")
          || noShow.equals("1");
    }
  }

  @Override
  public boolean scoreExists() {
    return true;
  }

  private final HttpServletRequest request;
}
