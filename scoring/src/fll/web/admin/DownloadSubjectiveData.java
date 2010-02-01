/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;

import fll.Team;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

/**
 * Commit the changes made by editTeam.jsp.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class DownloadSubjectiveData extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    try {
      final Connection connection = datasource.getConnection();
      final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
      if (Queries.isJudgesProperlyAssigned(connection, challengeDocument)) {
        response.reset();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "filename=subjective-data.fll");
        writeSubjectiveScores(connection, challengeDocument, response.getOutputStream());
      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write out the subjective scores data for the current tournament.
   * 
   * @param stream where to write the scores file
   * @throws IOException
   */
  public static void writeSubjectiveScores(final Connection connection, final Document challengeDocument, final OutputStream stream) throws IOException,
      SQLException {
    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
    final int tournament = Queries.getCurrentTournament(connection);

    final XMLWriter xmlwriter = new XMLWriter();

    final ZipOutputStream zipOut = new ZipOutputStream(stream);
    xmlwriter.setOutput(zipOut, "UTF8");

    zipOut.putNextEntry(new ZipEntry("challenge.xml"));
    xmlwriter.write(challengeDocument);
    zipOut.closeEntry();

    zipOut.putNextEntry(new ZipEntry("score.xml"));
    xmlwriter.setNeedsIndent(true);
    final Document scoreDocument = XMLUtils.createSubjectiveScoresDocument(challengeDocument, tournamentTeams.values(), connection, tournament);
    xmlwriter.write(scoreDocument);
    zipOut.closeEntry();

    zipOut.close();
  }
}
