/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import net.mtu.eggplant.util.StringUtils;

/**
 * The static state of a team. This does not include information about the team
 * at a given tournament. If
 * someone changes the database, this object does not notice the changes. It's a
 * snapshot in time from when the object was created.
 */
public class Team implements Serializable {

  /**
   * Constant to represent the team number for a bye.
   */
  public static final int BYE_TEAM_NUMBER = -1;

  /**
   * Constant to represent the team number when there is a tie.
   */
  public static final int TIE_TEAM_NUMBER = -2;

  /**
   * Constant to represent a NULL team entry in the playoff data table.
   */
  public static final int NULL_TEAM_NUMBER = -3;

  /**
   * Team that represents a BYE.
   */
  public static final Team BYE = new Team(BYE_TEAM_NUMBER, "INTERNAL", "BYE");

  /**
   * Team that represents a TIE.
   */
  public static final Team TIE = new Team(TIE_TEAM_NUMBER, "INTERNAL", "TIE");

  /**
   * NULL Team.
   */
  public static final Team NULL = new Team(NULL_TEAM_NUMBER, "INTERNAL", "NULL");

  /**
   * Compare teams by number.
   */
  public static final Comparator<Team> TEAM_NUMBER_COMPARATOR = new Comparator<Team>() {
    public int compare(final Team one,
                       final Team two) {
      final int oneNum = one.getTeamNumber();
      final int twoNum = two.getTeamNumber();
      if (oneNum < twoNum) {
        return -1;
      } else if (oneNum > twoNum) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  /**
   * Compare teams by name.
   */
  public static final Comparator<Team> TEAM_NAME_COMPARATOR = new Comparator<Team>() {
    public int compare(final Team one,
                       final Team two) {
      return one.getTeamName().compareTo(two.getTeamName());
    }
  };

  /**
   * @param teamNumber {@link #getTeamNumber()}
   * @param org {@link #getOrganization()}
   * @param name {@link #getTeamName()}
   */
  public Team(@JsonProperty("teamNumber") final int teamNumber,
              @JsonProperty("organization") final @Nullable String org,
              @JsonProperty("teamName") final String name) {
    this.teamNumber = teamNumber;
    this.organization = org;
    this.teamName = name;
  }

  /**
   * Builds a team object from its database info given the team number.
   * 
   * @param connection Database connection.
   * @param teamNumber Number of the team for which to build an object.
   * @return The new Team object or null if the team was not found in the
   *         database.
   * @throws SQLException on a database access error.
   * @throws IllegalArgumentException if the team cannot be found
   */
  public static Team getTeamFromDatabase(final Connection connection,
                                         final int teamNumber)
      throws SQLException {
    // First, handle known non-database team numbers...
    if (teamNumber == NULL_TEAM_NUMBER) {
      return NULL;
    }
    if (teamNumber == TIE_TEAM_NUMBER) {
      return TIE;
    }
    if (teamNumber == BYE_TEAM_NUMBER) {
      return BYE;
    }

    try (PreparedStatement stmt = connection.prepareStatement("SELECT Organization, TeamName FROM Teams"
        + " WHERE TeamNumber = ?")) {
      stmt.setInt(1, teamNumber);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          final String org = rs.getString(1);
          final String name = castNonNull(rs.getString(2));

          final Team x = new Team(teamNumber, org, name);
          return x;
        } else {
          throw new IllegalArgumentException("Team "
              + teamNumber
              + " is not in the database");
        }
      }
    }
  }

  private final int teamNumber;

  /**
   * The team's number. This is the primary key for identifying a team.
   * 
   * @return team number
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  private final @Nullable String organization;

  /**
   * The organization that the team belongs to, this may be a school or youth
   * group.
   * 
   * @return organization
   */
  public @Nullable String getOrganization() {
    return organization;
  }

  private final String teamName;

  /**
   * The name of the team.
   * 
   * @return name
   */
  public String getTeamName() {
    return teamName;
  }

  /**
   * @return the team name shortened to {@link Team#MAX_TEAM_NAME_LEN}
   */
  public String getTrimmedTeamName() {
    return StringUtils.trimString(getTeamName(), Team.MAX_TEAM_NAME_LEN);
  }

  /**
   * Max team name length. Used to keep names from
   * wrapping in a number of places.
   */
  public static final int MAX_TEAM_NAME_LEN = 24;

  /**
   * Compares team numbers.
   */
  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (o instanceof Team) {
      final Team other = (Team) o;
      return other.getTeamNumber() == getTeamNumber();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getTeamNumber();
  }

  @Override
  public String toString() {
    return "["
        + getTeamNumber()
        + ": "
        + getTeamName()
        + "]";
  }

  /**
   * @return if this is an internal team
   */
  @JsonIgnore
  public boolean isInternal() {
    return isInternalTeamNumber(getTeamNumber());
  }

  /**
   * @param number team number to check
   * @return true if this an internal team number.
   */
  public static boolean isInternalTeamNumber(final int number) {
    return number < 0;
  }
}
