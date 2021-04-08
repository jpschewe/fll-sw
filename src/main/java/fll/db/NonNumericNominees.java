/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.xml.NonNumericCategory;

/**
 * Keep track of non-numeric nominees for a category.
 * Each instance represents a category and the teams in that category.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public class NonNumericNominees {

  /**
   * @param categoryName see {@link #getCategoryName()}
   * @param nominees see {@link #getNominees()}
   */
  public NonNumericNominees(@JsonProperty("categoryName") final String categoryName,
                            @JsonProperty("nominees") final Collection<Nominee> nominees) {
    this.mCategoryName = categoryName;
    this.nominees = new HashSet<>(nominees);
  }

  private final String mCategoryName;

  /**
   * @return title of the category for these nominees.
   * @see NonNumericCategory#getTitle()
   */
  public String getCategoryName() {
    return mCategoryName;
  }

  /**
   * Store this instance in the database. This replaces any nominees
   * for the category.
   * 
   * @param connection database connection
   * @param tournamentId the tournament to store the nominees with
   * @throws SQLException on a database error
   */
  public void store(final Connection connection,
                    final int tournamentId)
      throws SQLException {
    storeNominees(connection, tournamentId, mCategoryName, nominees);
  }

  private final Set<Nominee> nominees;

  /**
   * @return read-only set
   */
  public Set<Nominee> getNominees() {
    return Collections.unmodifiableSet(nominees);
  }

  /**
   * Replace the nominees in the database for the specified category.
   * 
   * @param connection database to add the nominees to
   * @param tournamentId the tournament to add the nominees to
   * @param category the category the nominees are for
   * @param nominees see {@link #getNominees()}
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "SpotBugs doesn't appear to handle Nullable annotation inside generic type")
  private static void storeNominees(final Connection connection,
                                    final int tournamentId,
                                    final String category,
                                    final Set<Nominee> nominees)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM non_numeric_nominees"//
        + " WHERE tournament = ?"//
        + " AND category = ?")) {
      delete.setInt(1, tournamentId);
      delete.setString(2, category);
      delete.executeUpdate();
    }

    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO non_numeric_nominees" //
        + " (tournament, category, team_number, judge) VALUES(?, ?, ?, ?)")) {

      insert.setInt(1, tournamentId);
      insert.setString(2, category);

      for (final Nominee nominee : nominees) {
        insert.setInt(3, nominee.getTeamNumber());

        for (final String judge : nominee.getJudges()) {
          insert.setString(4, judge);
          insert.executeUpdate();
        } // foreach judge
      } // foreach nominee

    } // allocation of PreparedStatement
  }

  /**
   * Get all non-numeric categories known for the specified tournament.
   * 
   * @param connection the database to get the categories from
   * @param tournamentId the tournament to get the categories for
   * @return the non-numeric categories
   * @throws SQLException on a database error
   */
  public static Set<String> getCategories(final Connection connection,
                                          final int tournamentId)
      throws SQLException {
    final Set<String> result = new HashSet<>();
    try (
        PreparedStatement get = connection.prepareStatement("SELECT DISTINCT category FROM non_numeric_nominees WHERE tournament = ?")) {
      get.setInt(1, tournamentId);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final String category = rs.getString(1);
          result.add(category);
        }
      }
    }
    return result;
  }

  /**
   * Get all nominees in the specified category.
   * 
   * @param connection database connection
   * @param tournamentId the tournament
   * @param category the category
   * @return teams that are nominees in the category
   * @throws SQLException on a database error
   */
  public static Set<Nominee> getNominees(final Connection connection,
                                         final int tournamentId,
                                         final String category)
      throws SQLException {
    final Set<Nominee> result = new HashSet<>();
    try (
        PreparedStatement get = connection.prepareStatement("SELECT team_number, array_agg(judge) AS judges FROM non_numeric_nominees"
            + " WHERE tournament = ?" //
            + " AND category = ?" //
            + " GROUP BY team_number")) {
      get.setInt(1, tournamentId);
      get.setString(2, category);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final int team = rs.getInt("team_number");
          final Set<String> judges = new HashSet<>();
          try (ResultSet arrayRs = rs.getArray("judges").getResultSet()) {
            while (arrayRs.next()) {
              judges.add(arrayRs.getString(2));
            }
          }
          final Nominee nominee = new Nominee(team, judges);
          result.add(nominee);
        } // foreach result
      } // allocate query
    } // allocate prepared statement

    return result;
  }

  /**
   * Get the nominees by the specified judge for a team.
   * 
   * @param connection database connection
   * @param tournament the tournament
   * @param judge the judge
   * @param teamNumber the team number
   * @return the titles of the non-numeric categories
   * @throws SQLException on a database error
   */
  public static Set<String> getNomineesByJudgeForTeam(final Connection connection,
                                                      final Tournament tournament,
                                                      final String judge,
                                                      final int teamNumber)
      throws SQLException {
    final Set<String> result = new HashSet<>();
    try (PreparedStatement get = connection.prepareStatement("SELECT category FROM non_numeric_nominees"
        + " WHERE tournament = ?" //
        + " AND judge = ?" //
        + " AND team_number = ?")) {
      get.setInt(1, tournament.getTournamentID());
      get.setString(2, judge);
      get.setInt(3, teamNumber);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final String category = rs.getString("category");
          result.add(category);
        } // foreach result
      } // allocate query
    } // allocate prepared statement

    return result;
  }

  /**
   * Store operation for
   * {@link #getNomineesByJudgeForTeam(Connection, Tournament, String, int)}.
   * 
   * @param connection database connection
   * @param tournament the tournament
   * @param judge the judge
   * @param teamNumber the team number
   * @param nominations the new value for nominations
   * @throws SQLException on a database error
   */
  public static void storeNomineesByJudgeForTeam(final Connection connection,
                                                 final Tournament tournament,
                                                 final String judge,
                                                 final int teamNumber,
                                                 final Set<String> nominations)
      throws SQLException {
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement delete = connection.prepareStatement("DELETE FROM non_numeric_nominees" //
          + " WHERE tournament = ?" //
          + " AND judge = ?" //
          + " AND team_number = ?")) {
        delete.setInt(1, tournament.getTournamentID());
        delete.setString(2, judge);
        delete.setInt(3, teamNumber);
        delete.executeUpdate();
      }

      if (!nominations.isEmpty()) {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO non_numeric_nominees" //
            + " (tournament, judge, team_number, category)" //
            + " VALUES(?, ?, ?, ?)"//
        )) {
          insert.setInt(1, tournament.getTournamentID());
          insert.setString(2, judge);
          insert.setInt(3, teamNumber);
          for (final String nomination : nominations) {
            insert.setString(4, nomination);
            insert.addBatch();
          }

          insert.executeBatch();
        }
      }

      connection.commit();
    } finally {
      connection.setAutoCommit(autoCommit);
    }
  }

  /**
   * A single nominee.
   */
  public static final class Nominee {
    /**
     * @param teamNumber see {@link #getTeamNumber()}
     * @param judges see {@link #getJudges()}
     */
    public Nominee(final @JsonProperty("teamNumber") int teamNumber,
                   final @JsonProperty("judges") Set<@Nullable String> judges) {
      this.judges.addAll(judges);
      this.teamNumber = teamNumber;
    }

    private final int teamNumber;

    /**
     * @return the team being nominated
     */
    public int getTeamNumber() {
      return teamNumber;
    }

    private final Set<@Nullable String> judges = new HashSet<>();

    /**
     * The judges that have nominated this team. Null is a valid judge meaning that
     * the team has been nominated outside of a context where a judge name is
     * available.
     * 
     * @return read-only set of the judges that have nominated this team
     */
    public Set<@Nullable String> getJudges() {
      return Collections.unmodifiableSet(this.judges);
    }

    @Override
    public int hashCode() {
      return Objects.hash(teamNumber, judges);
    }

    @Override
    public boolean equals(final Object o) {
      if (null == o) {
        return false;
      } else if (this == o) {
        return true;
      } else if (Nominee.class.equals(o.getClass())) {
        final Nominee other = (Nominee) o;
        return this.teamNumber == other.teamNumber
            && this.judges.equals(other.judges);
      } else {
        return false;
      }
    }
  }
}
