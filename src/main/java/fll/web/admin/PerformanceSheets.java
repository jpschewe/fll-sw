/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import fll.Tournament;
import fll.scheduler.TournamentSchedule;
import fll.web.TournamentData;
import fll.xml.ChallengeDescription;
import jakarta.servlet.annotation.WebServlet;

/**
 * @see TournamentSchedule#outputPerformanceSheets(TournamentData,
 *      java.io.OutputStream, ChallengeDescription)
 */
@WebServlet("/admin/PerformanceSheets")
public class PerformanceSheets extends BaseScheduleServlet {

  @Override
  void outputSchedule(final Connection connection,
                      final TournamentData tournamentData,
                      final ChallengeDescription description,
                      final TournamentSchedule schedule,
                      final OutputStream output)
      throws SQLException, IOException {
    schedule.outputPerformanceSheets(tournamentData, output, description);
  }

  @Override
  String getFilename(Tournament tournament) {
    return String.format("%s_performance-sheets.pdf", tournament.getName());
  }

}
