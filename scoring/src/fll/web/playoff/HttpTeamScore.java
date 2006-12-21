/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import fll.Team;

/**
 * A team score in an HTTP request.
 * 
 * @author jpschewe
 * @version $Revision$
 */
/* package */final class HttpTeamScore extends TeamScore {

  private static final Logger LOG = Logger.getLogger(HttpTeamScore.class);

  public HttpTeamScore(final HttpServletRequest request, final Team team, final int runNumber) {
    super(team, runNumber);
    _request = request;
  }

  /**
   * @see fll.web.playoff.TeamScore#getEnumScore(java.lang.String)
   */
  public String getEnumScore(final String goalName) {
    return _request.getParameter(goalName);
  }

  /**
   * @see fll.web.playoff.TeamScore#getIntScore(java.lang.String)
   */
  public int getIntScore(final String goalName) {
    return Integer.parseInt(_request.getParameter(goalName));
  }

  /**
   * @see fll.web.playoff.TeamScore#getTotalScore()
   */
  public int getTotalScore() {
    return Integer.parseInt(_request.getParameter("totalScore"));
  }

  /**
   * @see fll.web.playoff.TeamScore#isNoShow()
   */
  public boolean isNoShow() {
    final String noShow = _request.getParameter("NoShow");
    if (null == noShow) {
      throw new RuntimeException("Missing parameter: NoShow");
    }
    return noShow.equalsIgnoreCase("true") || noShow.equalsIgnoreCase("t") || noShow.equals("1");
  }

  /*
   * @see fll.web.playoff.TeamScore#scoreExists()
   */
  public boolean scoreExists() {
    return true;
  }

  private final HttpServletRequest _request;
}
