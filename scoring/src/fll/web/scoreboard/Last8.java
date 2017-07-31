/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreboard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.flltools.displaySystem.list.SetArray;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;

/**
 * Display the most recent scores.
 * Currently hard coded to 20. This was originally 8, thus the name 'Last8'.
 */
@WebServlet("/scoreboard/Last8")
public class Last8 extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
    final Formatter formatter = new Formatter(response.getWriter());
    final String showOrgStr = request.getParameter("showOrganization");
    final boolean showOrg = null == showOrgStr ? true : Boolean.parseBoolean(showOrgStr);

    formatter.format("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">%n");
    formatter.format("<html>%n");
    formatter.format("<head>%n");
    formatter.format("<link rel='stylesheet' type='text/css' href='../style/fll-sw.css' />%n");
    formatter.format("<link rel='stylesheet' type='text/css' href='score_style.css' />%n");
    formatter.format("<meta http-equiv='refresh' content='30' />%n");
    formatter.format("</head>%n");

    formatter.format("<body class='scoreboard'>%n");
    formatter.format("<table border='1' cellpadding='2' cellspacing='0' width='98%%'>%n");

    formatter.format("<colgroup>%n");
    formatter.format("<col width='75px' />%n");
    formatter.format("<col />%n");
    if (showOrg) {
      formatter.format("<col />%n");
    }
    formatter.format("<col width='100px' />%n");
    formatter.format("<col width='75px' />%n");
    formatter.format("</colgroup>%n");

    formatter.format("<tr>%n");
    int numColumns = 5;
    if (!showOrg) {
      --numColumns;
    }
    formatter.format("<th colspan='%d' bgcolor='#800080'>Most Recent Performance Scores</th>%n", numColumns);
    formatter.format("</tr>%n");

    // scores here
    processScores(application, (rs) -> {
      formatter.format("<tr>%n");
      formatter.format("<td class='left'>%d</td>%n", rs.getInt("TeamNumber"));
      String teamName = rs.getString("TeamName");
      if (null == teamName) {
        teamName = "&nbsp;";
      }
      formatter.format("<td class='left truncate'>%s</td>%n", teamName);
      if (showOrg) {
        String organization = rs.getString("Organization");
        if (null == organization) {
          organization = "&nbsp;";
        }
        formatter.format("<td class='left truncate'>%s</td>%n", organization);
      }
      formatter.format("<td class='right truncate'>%s</td>%n", rs.getString("event_division"));

      formatter.format("<td class='right'>");
      if (rs.getBoolean("NoShow")) {
        formatter.format("No Show");
      } else if (rs.getBoolean("Bye")) {
        formatter.format("Bye");
      } else {
        formatter.format("%s",
                         Utilities.getFormatForScoreType(performanceScoreType).format(rs.getDouble("ComputedTotal")));
      }
      formatter.format("</td>%n");
      formatter.format("</tr>%n");
    });

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Exiting doPost");
    }
  }

  /**
   * Get the displayed data as a list for flltools.
   * 
   * @param application get all of the appropriate parameters
   * @return payload for the set array message
   */
  public static SetArray.Payload getTableAsList(@Nonnull final ServletContext application) {
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();

    final List<List<String>> data = new LinkedList<>();
    processScores(application, (rs) -> {
      final List<String> row = new LinkedList<>();

      row.add(String.valueOf(rs.getInt("TeamNumber")));
      String teamName = rs.getString("TeamName");
      if (null == teamName) {
        teamName = "";
      }
      row.add(teamName);
      String organization = rs.getString("Organization");
      if (null == organization) {
        organization = "";
      }
      row.add(organization);
      row.add(rs.getString("event_division"));

      if (rs.getBoolean("NoShow")) {
        row.add("No Show");
      } else if (rs.getBoolean("Bye")) {
        row.add("Bye");
      } else {
        row.add(Utilities.getFormatForScoreType(performanceScoreType).format(rs.getDouble("ComputedTotal")));
      }

      data.add(row);
    });

    final SetArray.Payload payload = new SetArray.Payload("Most Recent Performance Scores", data);
    return payload;
  }

  /**
   * Used for processing the result of a score query in
   * {@link Last8#processScores(ServletContext, ProcessScoreEntry)}.
   */
  private static interface ProcessScoreEntry {
    public void execute(@Nonnull ResultSet rs) throws SQLException;
  }

  /**
   * @param application the context to get the database connection from
   * @param processor passed the {@link ResultSet} for each row. (Team Number,
   *          Organization, Team
   *          Name, award group, bye, no show, computed total)
   */
  private static void processScores(@Nonnull final ServletContext application,
                                    @Nonnull final ProcessScoreEntry processor) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int currentTournament = Queries.getCurrentTournament(connection);
      final int maxScoreboardRound = TournamentParameters.getMaxScoreboardPerformanceRound(connection,
                                                                                           currentTournament);
      try (PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamNumber"
          + ", Teams.Organization" //
          + ", Teams.TeamName" //
          + ", current_tournament_teams.event_division" //
          + ", verified_performance.Bye" //
          + ", verified_performance.NoShow" //
          + ", verified_performance.ComputedTotal" //
          + " FROM Teams,verified_performance,current_tournament_teams"//
          + " WHERE verified_performance.Tournament = ?" //
          + "  AND Teams.TeamNumber = verified_performance.TeamNumber" //
          + "  AND Teams.TeamNumber = current_tournament_teams.TeamNumber" //
          + "  AND verified_performance.Bye = False" //
          + "  AND verified_performance.RunNumber <= ?"
          + " ORDER BY verified_performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 20")) {
        prep.setInt(1, currentTournament);
        prep.setInt(2, maxScoreboardRound);
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            processor.execute(rs);
          } // end while next
        } // try ResultSet
      } // try PreparedStatement

    } catch (final SQLException e) {
      throw new RuntimeException("Error talking to the database", e);
    }
  }
}
