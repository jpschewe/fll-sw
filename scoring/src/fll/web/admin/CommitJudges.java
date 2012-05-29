/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.JudgeInformation;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit judges information.
 */
@WebServlet("/admin/CommitJudges")
public class CommitJudges extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitJudges.processRequest");
    }

    try {
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();

      commitData(session, connection, Queries.getCurrentTournament(connection));

      // finally redirect to index.jsp
      session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully assigned judges</i></p>");
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  private static void commitData(final HttpSession session,
                                 final Connection connection,
                                 final int tournament) throws SQLException, IOException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // save old judge information
      final Set<JudgeInformation> oldJudgeInfo = new HashSet<JudgeInformation>();
      prep = connection.prepareStatement("SELECT id, category, event_division FROM Judges WHERE Tournament = ?");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String division = rs.getString(3);
        oldJudgeInfo.add(new JudgeInformation(id, category, division));
      }

      // delete old data in judges
      prep = connection.prepareStatement("DELETE FROM Judges where Tournament = ?");
      prep.setInt(1, tournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      prep = connection.prepareStatement("INSERT INTO Judges (id, category, event_division, Tournament) VALUES(?, ?, ?, ?)");
      prep.setInt(4, tournament);

      // can't put types inside a session
      @SuppressWarnings("unchecked")
      final Collection<JudgeInformation> judges = SessionAttributes.getNonNullAttribute(session,
                                                                                        GatherJudgeInformation.JUDGES_KEY,
                                                                                        Collection.class);

      final Set<JudgeInformation> newJudgeInfo = new HashSet<JudgeInformation>();
      for (final JudgeInformation info : judges) {
        newJudgeInfo.add(info);

        prep.setString(1, info.getId());
        prep.setString(2, info.getCategory());
        prep.setString(3, info.getDivision());
        prep.executeUpdate();
      }

      // figure out which ones are no longer there and remove all of their old
      // scores
      oldJudgeInfo.removeAll(newJudgeInfo);
      for (final JudgeInformation oldInfo : oldJudgeInfo) {
        prep = connection.prepareStatement(String.format("DELETE FROM %s WHERE Judge = ? AND Tournament = ?",
                                                         oldInfo.getCategory()));
        prep.setString(1, oldInfo.getId());
        prep.setInt(2, tournament);
        prep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(prep);
      SQLFunctions.close(rs);
    }
  }
}
