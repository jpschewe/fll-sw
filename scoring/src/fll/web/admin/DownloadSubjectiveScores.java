/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fll.Team;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.XMLUtils;

/**
 * Download the subjective scores to be used by mobile apps.
 */
@WebServlet("/admin/score.xml")
public class DownloadSubjectiveScores extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      if (Queries.isJudgesProperlyAssigned(connection, challengeDescription)) {
        response.reset();
        response.setContentType("text/xml");
        response.setHeader("Content-Disposition", "filename=score.xml");

        final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
        final int tournament = Queries.getCurrentTournament(connection);

        final Document scoreDocument = DownloadSubjectiveData.createSubjectiveScoresDocument(challengeDescription,
                                                                                             tournamentTeams.values(),
                                                                                             connection, tournament);

        try {
          DownloadSubjectiveData.validateXML(scoreDocument);
        } catch (final SAXException e) {
          throw new FLLInternalException("Subjective XML document is invalid", e);
        }

        final Charset charset = Charset.forName("UTF-8");
        final Writer writer = new OutputStreamWriter(response.getOutputStream(), charset);
        XMLUtils.writeXML(scoreDocument, writer, "UTF-8");

      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
