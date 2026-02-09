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
import fll.scheduler.ScheduleWriter;
import fll.scheduler.TournamentSchedule;
import fll.web.TournamentData;
import fll.xml.ChallengeDescription;
import jakarta.servlet.annotation.WebServlet;

/**
 * @see ScheduleWriter#outputPerformanceSchedulePerTableByTime(Connection,
 *      TournamentData, TournamentSchedule, java.io.OutputStream)
 */
@WebServlet("/admin/PerformanceNotes")
public class PerformanceNotes extends BaseScheduleServlet {

  @Override
  void outputSchedule(final Connection connection,
                      final TournamentData tournamentData,
                      final ChallengeDescription description,
                      final TournamentSchedule schedule,
                      final OutputStream output)
      throws SQLException, IOException {
    ScheduleWriter.outputPerformanceSchedulePerTableByTimeForNotes(connection, tournamentData, schedule, output);
  }

  @Override
  String getFilename(Tournament tournament) {
    return String.format("%s_performanceSchedulePerTableForNotes.pdf", tournament.getName());
  }
}
