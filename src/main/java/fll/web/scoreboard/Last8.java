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
import java.util.Set;

import javax.annotation.Nonnull;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.Utilities;
import fll.db.DelayedPerformance;
import fll.db.TournamentParameters;
import fll.flltools.displaySystem.list.SetArray;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;

/**
 * Display the most recent scores.
 * Currently hard coded to 20. This was originally 8, thus the name 'Last8'.
 */
@WebServlet("/scoreboard/Last8")
public class Last8 extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Entering doPost");
    }

    final Formatter formatter = new Formatter(response.getWriter());
    final String showOrgStr = request.getParameter("showOrganization");
    final boolean showOrg = null == showOrgStr ? true : Boolean.parseBoolean(showOrgStr);

    formatter.format("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">%n");
    formatter.format("<html>%n");
    formatter.format("<head>%n");
    formatter.format("<link rel='stylesheet' type='text/css' href='../style/fll-sw.css' />%n");
    formatter.format("<link rel='stylesheet' type='text/css' href='score_style.css' />%n");
    formatter.format("<meta http-equiv='refresh' content='30' />%n");

    formatter.format("<script type=\"text/javascript\" src=\"set-font-size.js\"></script>%n");

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
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      processScores(application, (teamNumber,
                                  teamName,
                                  organization,
                                  awardGroup,
                                  formattedScore) -> {
        formatter.format("<tr>%n");
        formatter.format("<td class='left'>%d</td>%n", teamNumber);
        if (null == teamName) {
          teamName = "&nbsp;";
        }
        formatter.format("<td class='left truncate'>%s</td>%n", teamName);
        if (showOrg) {
          if (null == organization) {
            organization = "&nbsp;";
          }
          formatter.format("<td class='left truncate'>%s</td>%n", organization);
        }
        formatter.format("<td class='right truncate'>%s</td>%n", awardGroup);

        formatter.format("<td class='right'>%s</td>", formattedScore);
        formatter.format("</tr>%n");
      });
    } catch (final SQLException e) {
      throw new FLLInternalException("Got an error getting the most recent scores from the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Exiting doPost");
    }
  }

  /**
   * Get the displayed data as a list for flltools.
   *
   * @param application get all of the appropriate parameters
   * @return payload for the set array message
   * @throws SQLException if a database error occurs
   */
  public static SetArray.Payload getTableAsList(@Nonnull final ServletContext application) throws SQLException {

    final List<List<String>> data = new LinkedList<>();
    processScores(application, (teamNumber,
                                teamName,
                                organization,
                                awardGroup,
                                formattedScore) -> {
      final List<String> row = new LinkedList<>();

      row.add(String.valueOf(teamNumber));
      if (null == teamName) {
        row.add("");
      } else {
        row.add(teamName);
      }

      if (null == organization) {
        row.add("");
      } else {
        row.add(organization);
      }

      row.add(awardGroup);

      row.add(formattedScore);

      data.add(row);
    });

    final SetArray.Payload payload = new SetArray.Payload("Most Recent Performance Scores", data);
    return payload;
  }

  private interface ProcessScoreEntry {
    void execute(int teamNumber,
                 @Nullable String teamName,
                 @Nullable String organization,
                 String awardGroup,
                 String formattedScore);
  }

  /**
   * @param application the context to get the database connection from
   * @param processor passed the {@link ResultSet} for each row. (Team Number,
   *          Organization, Team
   *          Name, award group, bye, no show, computed total)
   */
  private static void processScores(@Nonnull final ServletContext application,
                                    @Nonnull final ProcessScoreEntry processor)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);
      final int currentTournamentId = currentTournament.getTournamentID();
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournamentId);
      final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, currentTournamentId);
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
      final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, currentTournament);

      try (PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamNumber"
          + ", Teams.Organization" //
          + ", Teams.TeamName" //
          + ", TournamentTeams.event_division" //
          + ", verified_performance.Bye" //
          + ", verified_performance.NoShow" //
          + ", verified_performance.ComputedTotal" //
          + " FROM Teams,verified_performance,TournamentTeams"//
          + " WHERE verified_performance.Tournament = ?" //
          + "  AND Teams.TeamNumber = verified_performance.TeamNumber" //
          + "  AND Teams.TeamNumber = TournamentTeams.TeamNumber" //
          + "  AND TournamentTeams.tournament = verified_performance.Tournament" //
          + "  AND verified_performance.Bye = False" //
          + "  AND (? OR verified_performance.RunNumber <= ?)" //
          + "  AND verified_performance.RunNumber <= ?" //
          + " ORDER BY verified_performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 20")) {
        prep.setInt(1, currentTournamentId);
        prep.setBoolean(2, !runningHeadToHead);
        prep.setInt(3, numSeedingRounds);
        prep.setInt(4, maxRunNumberToDisplay);

        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt("TeamNumber");
            final String teamName = castNonNull(rs.getString("TeamName"));

            final String organization = rs.getString("Organization");
            final String awardGroup = castNonNull(rs.getString("event_division"));

            final String formattedScore;
            if (rs.getBoolean("NoShow")) {
              formattedScore = "No Show";
            } else if (rs.getBoolean("Bye")) {
              formattedScore = "Bye";
            } else {
              formattedScore = Utilities.getFormatForScoreType(performanceScoreType)
                                        .format(rs.getDouble("ComputedTotal"));
            }

            processor.execute(teamNumber, teamName, organization, awardGroup, formattedScore);
          } // end while next
        } // try ResultSet
      } // try PreparedStatement

    } // try connection
  }
}
