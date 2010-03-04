/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
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

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

/**
 * Commit the changes made by editTeam.jsp.
 * 
 * @web.servlet name="DownloadSubjectiveData"
 * @web.servlet-mapping url-pattern="/admin/subjective-data.fll"
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
   * Create a document to hold subject scores for the tournament described in
   * challengeDocument.
   * 
   * @param challengeDocument describes the tournament
   * @param teams the teams for this tournament
   * @param connection the database connection used to retrieve the judge
   *          information
   * @param currentTournament the tournament to generate the document for, used for
   *          deciding which set of judges to use
   * @return the document
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { 
  "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static Document createSubjectiveScoresDocument(final Document challengeDocument,
                                                        final Collection<Team> teams,
                                                        final Connection connection,
                                                        final int currentTournament) throws SQLException {
    ResultSet rs = null;
    ResultSet rs2 = null;
    PreparedStatement prep = null;
    PreparedStatement prep2 = null;
    try {
      prep = connection.prepareStatement("SELECT id, event_division FROM Judges WHERE category = ? AND Tournament = ?");
      prep.setInt(2, currentTournament);
  
      final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
      final Element top = document.createElement("scores");
      document.appendChild(top);
  
      for (final Element categoryDescription : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
        final String categoryName = categoryDescription.getAttribute("name");
        final Element categoryElement = document.createElement(categoryName);
        top.appendChild(categoryElement);
  
        prep.setString(1, categoryName);
        rs = prep.executeQuery();
        while (rs.next()) {
          final String judge = rs.getString(1);
          final String division = rs.getString(2);
  
          for (final Team team : teams) {
            final String teamDiv = Queries.getEventDivision(connection, team.getTeamNumber());
            if ("All".equals(division)
                || division.equals(teamDiv)) {
              final Element scoreElement = document.createElement("score");
              categoryElement.appendChild(scoreElement);
  
              scoreElement.setAttribute("teamName", team.getTeamName());
              scoreElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
              scoreElement.setAttribute("division", teamDiv);
              scoreElement.setAttribute("organization", team.getOrganization());
              scoreElement.setAttribute("judge", judge);
              scoreElement.setAttribute("NoShow", "false");
  
              prep2 = connection.prepareStatement("SELECT * FROM "
                  + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
              prep2.setInt(1, team.getTeamNumber());
              prep2.setInt(2, currentTournament);
              prep2.setString(3, judge);
              rs2 = prep2.executeQuery();
              if (rs2.next()) {
                for (final Element goalDescription : XMLUtils.filterToElements(categoryDescription.getElementsByTagName("goal"))) {
                  final String goalName = goalDescription.getAttribute("name");
                  final String value = rs2.getString(goalName);
                  if (!rs2.wasNull()) {
                    scoreElement.setAttribute(goalName, value);
                  }
                }
                scoreElement.setAttribute("NoShow", rs2.getString("NoShow"));
              }
            }
          }
        }
      }
      return document;
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeResultSet(rs2);
      SQLFunctions.closePreparedStatement(prep);
      SQLFunctions.closePreparedStatement(prep2);
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
    final Document scoreDocument = DownloadSubjectiveData.createSubjectiveScoresDocument(challengeDocument, tournamentTeams.values(), connection, tournament);
    xmlwriter.write(scoreDocument);
    zipOut.closeEntry();

    zipOut.close();
  }
}
