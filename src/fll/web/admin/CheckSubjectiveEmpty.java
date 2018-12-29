/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Check that there isn't any subjective data in the database. If there is, make
 * the user confirm before allowing the database to be exported.
 */
@WebServlet("/admin/CheckSubjectiveEmpty")
public class CheckSubjectiveEmpty extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentId = Queries.getCurrentTournament(connection);

      for (final SubjectiveScoreCategory category : challenge.getSubjectiveCategories()) {
        if (categoryHasScores(connection, category, tournamentId)) {
          response.sendRedirect(response.encodeRedirectURL("confirm-export-with-subjective-scores.jsp"));
          return;
        }
      }

      response.sendRedirect(response.encodeRedirectURL("ExportPerformanceData"));
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "table name is dependent on category name")
  private boolean categoryHasScores(final Connection connection,
                                    final SubjectiveScoreCategory category,
                                    final int tournamentId)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM "
        + category.getName()
        + " WHERE TOURNAMENT = ?")) {
      prep.setInt(1, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        } else {
          return false;
        }
      }
    }
  }

}
