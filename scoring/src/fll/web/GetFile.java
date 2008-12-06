/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

import com.lowagie.text.DocumentException;

import fll.Team;
import fll.Utilities;
import fll.db.DumpDB;
import fll.db.Queries;
import fll.web.report.FinalComputedScores;
import fll.web.scoreEntry.ScoresheetGenerator;
import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

/**
 * Used to generate various files for download.
 * 
 * @version $Revision$
 */
public final class GetFile extends HttpServlet {

  public GetFile() {

  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    doGet(request, response);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    try {
      GetFile.getFile(getServletContext(), request, response);
    } catch(final ParseException pe) {
      throw new RuntimeException(pe);
    } catch(final DocumentException de) {
      throw new RuntimeException(de);
    } catch(final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  /**
   * Write out the subjective scores data for the current tournament.
   * 
   * @param stream
   *          where to write the scores file
   * @throws IOException
   */
  public static void writeSubjectiveScores(final Connection connection, final Document challengeDocument, final OutputStream stream)
      throws IOException, SQLException {
    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
    final String tournament = Queries.getCurrentTournament(connection);

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

  /**
   * Get a file. Use the parameter "filename" to determine which file.
   */
  public static void getFile(final ServletContext application, final HttpServletRequest request, final HttpServletResponse response)
      throws SQLException, IOException, DocumentException, ParseException {
    final String filename = request.getParameter("filename");
    if("teams.xml".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
      final Document teamsDocument = XMLUtils.createTeamsDocument(connection, tournamentTeams.values());
      final XMLWriter xmlwriter = new XMLWriter();

      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "filename=teams.xml");
      xmlwriter.setOutput(response.getOutputStream(), null);
      xmlwriter.write(teamsDocument);
    } else if("score.xml".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      if(Queries.isJudgesProperlyAssigned(connection, challengeDocument)) {
        final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
        final String tournament = Queries.getCurrentTournament(connection);

        final Document scoreDocument = XMLUtils.createSubjectiveScoresDocument(challengeDocument, tournamentTeams.values(), connection, tournament);
        final XMLWriter xmlwriter = new XMLWriter();

        response.reset();
        response.setContentType("text/xml");
        response.setHeader("Content-Disposition", "filename=score.xml");
        xmlwriter.setOutput(response.getOutputStream(), null);
        xmlwriter.setNeedsIndent(true);
        xmlwriter.write(scoreDocument);
      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
    } else if("challenge.xml".equals(filename)) {
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

      final XMLWriter xmlwriter = new XMLWriter();

      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "filename=challenge.xml");
      xmlwriter.setOutput(response.getOutputStream(), null);
      xmlwriter.setNeedsIndent(true);
      xmlwriter.setStyleSheet("fll.css");
      xmlwriter.write(challengeDocument);

    } else if("subjective-data.zip".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      if(Queries.isJudgesProperlyAssigned(connection, challengeDocument)) {
        response.reset();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "filename=subjective-data.zip");
        writeSubjectiveScores(connection, challengeDocument, response.getOutputStream());
      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
    } else if("database.zip".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

      response.reset();
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "filename=database.zip");

      final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
      DumpDB.dumpDatabase(zipOut, connection, challengeDocument);
      zipOut.close();
    } else if("finalComputedScores.pdf".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      final String tournament = Queries.getCurrentTournament(connection);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalComputedScores.pdf");
      final FinalComputedScores fcs = new FinalComputedScores(challengeDocument, tournament);
      fcs.generateReport(connection, response.getOutputStream());
    } else if("teamScoreSheet.pdf".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");
      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();
      // Create the scoresheet generator - must provide correct number of
      // scoresheets
      final ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(teamNumber, challengeDocument, connection);

      // Write the scoresheets to the browser - content-type: application/pdf
      scoresheetGen.writeFile(connection, response.getOutputStream());
    } else if("scoreSheet.pdf".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      final String tournament = Queries.getCurrentTournament(connection);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

      // Create the scoresheet generator - must provide correct number of
      // scoresheets
      final ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(request.getParameterMap(), connection, tournament, challengeDocument);

      // Write the scoresheets to the browser - content-type: application/pdf
      scoresheetGen.writeFile(connection, response.getOutputStream());
    } else if("blankScoreSheet.pdf".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
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
      os.println("Unknown filename: " + filename);
      os.println("content type: " + request.getContentType());
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
  }

}
