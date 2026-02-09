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
 * @see ScheduleWriter#outputPerformanceScheduleByTime(Connection,
 *      TournamentData, TournamentSchedule, java.io.OutputStream)
 */
@WebServlet("/admin/PerformanceSchedule")
public class PerformanceSchedule extends BaseScheduleServlet {

  @Override
  void outputSchedule(final Connection connection,
                      final TournamentData tournamentData,
                      final ChallengeDescription description,
                      final TournamentSchedule schedule,
                      final OutputStream output)
      throws SQLException, IOException {
    ScheduleWriter.outputPerformanceScheduleByTime(connection, tournamentData, schedule, output);
  }

  @Override
  String getFilename(Tournament tournament) {
    return String.format("%s_performanceSchedule.pdf", tournament.getName());
  }

}
