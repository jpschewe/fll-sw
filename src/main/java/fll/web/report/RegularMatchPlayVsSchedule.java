/*
 * Copyright (c) 2022 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.TournamentParameters;
import fll.scheduler.PerformanceTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for regular-match-play-vs-schedule.jsp.
 */
public final class RegularMatchPlayVsSchedule {

  private RegularMatchPlayVsSchedule() {
  }

  /**
   * @param application get application variables
   * @param page used to set
   *          "data" a {@link Data} objects sorted by scheduled time, then by last
   *          edited time
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = description.getPerformance().getScoreType();

    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);

      final int numRegularMatchPlayRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                                     currentTournament.getTournamentID());

      final List<Data> data = new LinkedList<>();

      // use schedule to do initial populate
      if (TournamentSchedule.scheduleExistsInDatabase(connection, currentTournament.getTournamentID())) {
        final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournament.getTournamentID());
        for (final TeamScheduleInfo tsi : schedule.getSchedule()) {
          final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, currentTournament,
                                                                                   tsi.getTeamNumber());

          int nextRunNumber = 1;
          for (final PerformanceTime pt : tsi.getAllPerformances()) {
            if (!pt.isPractice()) {
              final Data d = new Data(team, nextRunNumber, pt);
              data.add(d);

              ++nextRunNumber;
            }
          }
        }
      }

      // update with tournament data
      try (
          PreparedStatement prep = connection.prepareStatement("SELECT teamnumber, runnumber, computedtotal, noshow, bye, timestamp FROM performance" //
              + " WHERE tournament = ?" //
              + "   AND runnumber <= ?")) {
        prep.setInt(1, currentTournament.getTournamentID());
        prep.setInt(2, numRegularMatchPlayRounds);

        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final int runNumber = rs.getInt(2);
            final double score = rs.getDouble(3);
            final boolean scoreIsNull = rs.wasNull();
            final boolean noShow = rs.getBoolean(4);
            final boolean bye = rs.getBoolean(5);
            final Timestamp lastEditedTs = castNonNull(rs.getTimestamp(6));
            final LocalTime lastEdited = lastEditedTs.toLocalDateTime().toLocalTime();

            final String value;
            if (noShow) {
              value = "No Show";
            } else if (bye) {
              value = "Bye";
            } else if (scoreIsNull) {
              value = "No Score";
            } else {
              value = Utilities.getFormatForScoreType(performanceScoreType).format(score);
            }

            final Optional<Data> needle = data.stream() //
                                              .filter(d -> d.getTeam().getTeamNumber() == teamNumber
                                                  && d.getRoundNumber() == runNumber) //
                                              .findFirst();
            if (needle.isPresent()) {
              needle.get().setFormattedScore(value);
              needle.get().setLastEdited(lastEdited);
            } else {
              final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, currentTournament,
                                                                                       teamNumber);
              final Data entry = new Data(team, runNumber, lastEdited, value);
              data.add(entry);
            }
          }
        }
      }

      Collections.sort(data);
      page.setAttribute("data", data);
    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting data for performance runs", e);
    }
  }

  /**
   * Data class for the JSP.
   */
  public static final class Data implements Comparable<Data> {

    /**
     * Used when constructing from the schedule.
     *
     * @param team {@link #getTeam()}
     * @param roundNumber {@link #getRoundNumber()}
     * @param perfTime {@link #getPerformanceTime()}
     */
    Data(final TournamentTeam team,
         final int roundNumber,
         final PerformanceTime perfTime) {
      this.team = team;
      this.roundNumber = roundNumber;
      this.perfTime = perfTime;
      this.lastEdited = null;
      this.formattedScore = "";
    }

    /**
     * Used when constructing from a score.
     * 
     * @param team {@link #getTeam()}
     * @param roundNumber {@link #getRoundNumber()}
     * @param lastEdited {@link #getLastEdited()}
     */
    Data(final TournamentTeam team,
         final int roundNumber,
         final LocalTime lastEdited,
         final String formattedScore) {
      this.team = team;
      this.roundNumber = roundNumber;
      this.perfTime = null;
      this.lastEdited = lastEdited;
      this.formattedScore = formattedScore;
    }

    private final TournamentTeam team;

    /**
     * @return team
     */
    public TournamentTeam getTeam() {
      return team;
    }

    private final int roundNumber;

    /**
     * @return round number (1-based)
     */
    public int getRoundNumber() {
      return roundNumber;
    }

    private final @Nullable PerformanceTime perfTime;

    /**
     * @return scheduled time, null if there isn't a schedule
     */
    public @Nullable PerformanceTime getPerformanceTime() {
      return perfTime;
    }

    private @Nullable LocalTime lastEdited;

    /**
     * @return when the score was last edited, null if score is not entered
     */
    public @Nullable LocalTime getLastEdited() {
      return lastEdited;
    }

    /**
     * @param v {@link #getLastEdited()}
     */
    public void setLastEdited(final LocalTime v) {
      lastEdited = v;
    }

    private String formattedScore;

    /**
     * @return score to display
     */
    public String getFormattedScore() {
      return formattedScore;
    }

    /**
     * @param v {@link #getFormattedScore()}
     */
    public void setFormattedScore(final String v) {
      formattedScore = v;
    }

    @Override
    public int hashCode() {
      return team.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      } else if (null == o) {
        return false;
      } else if (this.getClass().equals(o.getClass())) {
        return 0 == this.compareTo((Data) o);
      } else {
        return false;
      }
    }

    private int compareLastEdited(final Data o) {
      // if there is a mixture of null and not-null, put the nulls first
      final LocalTime thisLastEdited = this.lastEdited;
      final LocalTime otherLastEdited = o.lastEdited;

      if (null == thisLastEdited) {
        if (null != otherLastEdited) {
          return 1;
        } else {
          // otherLastEdited is null
          return 0;
        }
      } else if (null == otherLastEdited) {
        // lastEdited not null
        return -1;
      } else {
        return thisLastEdited.compareTo(otherLastEdited);
      }
    }

    @Override
    public int compareTo(final Data o) {
      // if we have a mixture of scheduled and not scheduled entries, sort the not
      // scheduled entries to the front of the list

      final PerformanceTime thisPerfTime = this.perfTime;
      final PerformanceTime otherPerfTime = o.perfTime;

      if (null == thisPerfTime
          && null != otherPerfTime) {
        return -1;
      } else if (null != thisPerfTime
          && null == otherPerfTime) {
        return 1;
      } else if (null == thisPerfTime
          && null == otherPerfTime) {
        // sort on edited time
        return compareLastEdited(o);
      } else {
        // have scheduled times
        // https://github.com/typetools/checker-framework/issues/5016
        final int perfCompare = castNonNull(thisPerfTime).getTime().compareTo(castNonNull(otherPerfTime).getTime());
        if (0 == perfCompare) {
          return compareLastEdited(o);
        } else {
          return perfCompare;
        }
      }
    }

  }

}
