/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Commit judges information.
 */
@WebServlet("/admin/CommitJudges")
public class CommitJudges extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE, UserRole.HEAD_JUDGE), false)) {
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitJudges.processRequest");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      commitData(session, connection, Queries.getCurrentTournament(connection));

      // finally redirect to index.jsp
      SessionAttributes.appendToMessage(session, "<p id='success'><i>Successfully assigned judges</i></p>");
      response.sendRedirect(response.encodeRedirectURL("index.jsp"));

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   *
   * @param tournament the current tournament
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  private static void commitData(final HttpSession session,
                                 final Connection connection,
                                 final int tournament)
      throws SQLException, IOException {
    // save old judge information
    final Collection<JudgeInformation> oldJudges = JudgeInformation.getJudges(connection, tournament);
    final Set<JudgeInformation> oldJudgeInfo = new HashSet<>();
    oldJudgeInfo.addAll(oldJudges);

    // delete old data in judges
    try (PreparedStatement deleteJudges = connection.prepareStatement("DELETE FROM Judges where Tournament = ?")) {
      deleteJudges.setInt(1, tournament);
      deleteJudges.executeUpdate();
    }

    final Set<JudgeInformation> newJudgeInfo = new HashSet<>();
    try (
        PreparedStatement insertJudge = connection.prepareStatement("INSERT INTO Judges (id, category, station, Tournament) VALUES(?, ?, ?, ?)")) {
      insertJudge.setInt(4, tournament);

      // can't put types inside a session
      @SuppressWarnings("unchecked")
      final Collection<JudgeInformation> judges = SessionAttributes.getNonNullAttribute(session,
                                                                                        GatherJudgeInformation.JUDGES_KEY,
                                                                                        Collection.class);

      for (final JudgeInformation info : judges) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Doing insert for id: "
              + info.getId()
              + " category: "
              + info.getCategory()
              + " station: "
              + info.getGroup());
        }

        newJudgeInfo.add(info);

        insertJudge.setString(1, info.getId());
        insertJudge.setString(2, info.getCategory());
        insertJudge.setString(3, info.getGroup());
        insertJudge.executeUpdate();
      }
    }

    // figure out which ones are no longer there and remove all of their old
    // scores
    oldJudgeInfo.removeAll(newJudgeInfo);
    for (final JudgeInformation oldInfo : oldJudgeInfo) {
      try (
          PreparedStatement deleteScores = connection.prepareStatement(String.format("DELETE FROM %s WHERE Judge = ? AND Tournament = ?",
                                                                                     oldInfo.getCategory()))) {
        deleteScores.setInt(2, tournament);
        deleteScores.setString(1, oldInfo.getId());
        deleteScores.executeUpdate();
      }
    }
  }
}
