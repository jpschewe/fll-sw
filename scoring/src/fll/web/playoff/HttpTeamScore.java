/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import javax.servlet.http.HttpServletRequest;

/**
 * A team score in an HTTP request.
 */
public final class HttpTeamScore extends TeamScore {

  public HttpTeamScore(final int teamNumber,
                       final int runNumber,
                       final HttpServletRequest request) {
    super(teamNumber, runNumber);
    _request = request;
  }

  /**
   * @see fll.web.playoff.TeamScore#getEnumRawScore(java.lang.String)
   */
  @Override
  public String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      return _request.getParameter(goalName);
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#getRawScore(java.lang.String)
   */
  @Override
  public double getRawScore(final String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else if (null == _request.getParameter(goalName)) {
      return Double.NaN;
    } else {
      return Double.parseDouble(_request.getParameter(goalName));
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
      final String noShow = _request.getParameter("NoShow");
      if (null == noShow) {
        throw new RuntimeException("Missing parameter: NoShow");
      }
      return noShow.equalsIgnoreCase("true")
          || noShow.equalsIgnoreCase("t") || noShow.equals("1");
    }
  }

  /*
   * @see fll.web.playoff.TeamScore#scoreExists()
   */
  @Override
  public boolean scoreExists() {
    return true;
  }

  private final HttpServletRequest _request;
}
