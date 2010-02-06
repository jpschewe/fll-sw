/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;

import com.lowagie.text.DocumentException;

import fll.Utilities;
import fll.db.Queries;
import fll.web.report.FinalComputedScores;
import fll.web.scoreEntry.ScoresheetGenerator;

/**
 * Used to generate various files for download.
 * 
 * @version $Revision$
 */
public final class GetFile extends BaseFLLServlet {

  public GetFile() {

  }

  /**
   * Get a file. Use the parameter "filename" to determine which file.
   */
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    try {
      final String filename = request.getParameter("filename");
      if ("finalComputedScores.pdf".equals(filename)) {
        final DataSource datasource = SessionAttributes.getDataSource(session);
        final Connection connection = datasource.getConnection();
        final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
        final int tournament = Queries.getCurrentTournament(connection);
        final String tournamentName = Queries.getTournamentName(connection, tournament);
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "filename=finalComputedScores.pdf");
        final FinalComputedScores fcs = new FinalComputedScores(challengeDocument, tournament, tournamentName);
        fcs.generateReport(connection, response.getOutputStream());
      } else if ("teamScoreSheet.pdf".equals(filename)) {
        final DataSource datasource = SessionAttributes.getDataSource(session);
        final Connection connection = datasource.getConnection();
        final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");
        final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();
        // Create the scoresheet generator - must provide correct number of
        // scoresheets
        final ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(teamNumber, challengeDocument, connection);

        // Write the scoresheets to the browser - content-type: application/pdf
        scoresheetGen.writeFile(connection, response.getOutputStream());
      } else if ("scoreSheet.pdf".equals(filename)) {
        final DataSource datasource = SessionAttributes.getDataSource(session);
        final Connection connection = datasource.getConnection();
        final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
        final int tournament = Queries.getCurrentTournament(connection);
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

        // Create the scoresheet generator - must provide correct number of
        // scoresheets
        final ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(request.getParameterMap(), connection, tournament, challengeDocument);

        // Write the scoresheets to the browser - content-type: application/pdf
        scoresheetGen.writeFile(connection, response.getOutputStream());
      } else if ("blankScoreSheet.pdf".equals(filename)) {
        final DataSource datasource = SessionAttributes.getDataSource(session);
        final Connection connection = datasource.getConnection();
        final Document challengeDocument = (Document) application.getAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT);
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

        // Create the scoresheet generator - must provide correct number of
        // scoresheets
        final ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(Queries.getScoresheetLayoutNUp(connection), challengeDocument);
        // Write the scoresheets to the browser - content-type: application/pdf
        scoresheetGen.writeFile(connection, response.getOutputStream());

      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Unknown filename: "
            + filename);
        os.println("content type: "
            + request.getContentType());
        // final java.util.Map params = request.getParameterMap();
        // final java.util.Iterator iter = params.keySet().iterator();
        // while(iter.hasNext()) {
        // final String key = (String)iter.next();
        // final String[] values = (String[])params.get(key);
        // response.getOutputStream().println("param: " + key);
        // //for(int i=0; i<values.length; i++) {
        // // response.getOutputStream().println(" value: " + values[i]);
        // //}
        // }
      }
      response.flushBuffer();
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    } catch (final DocumentException de) {
      throw new RuntimeException(de);
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

}
