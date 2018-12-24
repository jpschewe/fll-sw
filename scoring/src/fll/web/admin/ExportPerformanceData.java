/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;

import fll.Tournament;
import fll.db.DumpDB;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Export the database using a name with the tournament name, date and label
 * that it's the performance seeding export.
 */
@WebServlet("/admin/ExportPerformanceData")
public class ExportPerformanceData extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

      final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
      final String dateStr = df.format(new Date());

      final String filename = String.format("%s_%s_perf-after-seeding.flldb", tournament.getName(), dateStr);
      response.reset();
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "attachment; filename="
          + filename);

      final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
      try {
        DumpDB.dumpDatabase(zipOut, connection, challengeDocument, application);
      } finally {
        zipOut.close();
      }
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

}
