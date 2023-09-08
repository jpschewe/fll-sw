/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import fll.scheduler.TournamentSchedule;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for teamColumnSelection.jsp.
 */
public final class TeamColumnSelection {

  private TeamColumnSelection() {
  }

  /**
   * Setup page variables used by the JSP.
   * 
   * @param session session variables
   * @param page set page variables
   */
  public static void populateContext(final HttpSession session, final PageContext page) {
    if (null == session.getAttribute("columnSelectOptions")) {
      throw new RuntimeException(
      "Error columnSelectOptions not set.  Please start back at administration page and go forward.");
    }

    page.setAttribute("TEAM_NUMBER_HEADER", UploadTeams.sanitizeColumnName(TournamentSchedule.TEAM_NUMBER_HEADER));
    page.setAttribute("TEAM_NAME_HEADER", UploadTeams.sanitizeColumnName(TournamentSchedule.TEAM_NAME_HEADER));
    page.setAttribute("ORGANIZATION_HEADER", UploadTeams.sanitizeColumnName(TournamentSchedule.ORGANIZATION_HEADER));
    page.setAttribute("AWARD_GROUP_HEADER", UploadTeams.sanitizeColumnName(TournamentSchedule.AWARD_GROUP_HEADER));
    page.setAttribute("JUDGE_GROUP_HEADER", UploadTeams.sanitizeColumnName(TournamentSchedule.JUDGE_GROUP_HEADER));
  }

}
