/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import fll.Queries;

import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

import org.w3c.dom.Document;

import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Used to generate various files for download.  Called from getfile.jsp.
 *
 * @version $Revision$
 */
public final class GetFile {
   
  private GetFile() {
     
  }

  public static void getFile(final ServletContext application,
                             final HttpServletRequest request,
                             final HttpServletResponse response) throws SQLException, IOException {
    final String filename = request.getParameter("filename");
    if("teams.xml".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Map tournamentTeams = Queries.getTournamentTeams(connection);
      final Document teamsDocument = XMLUtils.createTeamsDocument(tournamentTeams.values());
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
        final Map tournamentTeams = Queries.getTournamentTeams(connection);
        final String tournament = Queries.getCurrentTournament(connection);
      
        final Document scoreDocument = XMLUtils.createSubjectiveScoresDocument(challengeDocument,
                                                                               tournamentTeams.values(),
                                                                               connection,
                                                                               tournament);
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
    } else if("subjective.zip".equals(filename)) {
      final Connection connection = (Connection)application.getAttribute("connection");
      final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
      if(Queries.isJudgesProperlyAssigned(connection, challengeDocument)) {
      
        final Map tournamentTeams = Queries.getTournamentTeams(connection);
        final String tournament = Queries.getCurrentTournament(connection);
      
        final Document scoreDocument = XMLUtils.createSubjectiveScoresDocument(challengeDocument,
                                                                               tournamentTeams.values(),
                                                                               connection,
                                                                               tournament);
        final XMLWriter xmlwriter = new XMLWriter();
      
        response.reset();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "filename=subjective.zip");
        final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
        xmlwriter.setOutput(zipOut, "UTF8");
      
        zipOut.putNextEntry(new ZipEntry("challenge.xml"));
        xmlwriter.write(challengeDocument);
        zipOut.closeEntry();
        zipOut.putNextEntry(new ZipEntry("score.xml"));
        xmlwriter.setNeedsIndent(true);
        xmlwriter.write(scoreDocument);
        zipOut.closeEntry();
      
        zipOut.close();
      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
      
    } else {
      response.reset();
      response.setContentType("text/plain");
      final ServletOutputStream os = response.getOutputStream();
      os.println("Unknown filename: " + filename);
      os.println("content type: " + request.getContentType());
//       final java.util.Map params = request.getParameterMap();
//       final java.util.Iterator iter = params.keySet().iterator();
//       while(iter.hasNext()) {
//         final String key = (String)iter.next();
//         final String[] values = (String[])params.get(key);
//         response.getOutputStream().println("param: " + key);
//         //for(int i=0; i<values.length; i++) {
//         //  response.getOutputStream().println("  value: " + values[i]);
//         //}
//       }
    }
    response.flushBuffer();
  }                             

}
