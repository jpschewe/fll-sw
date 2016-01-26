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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.StringUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * The static state of a team. This does not include information about the team
 * at a given tournament. Note that the {@link #getDivision() division}
 * attribute represents the division the team is registered in, which may not be
 * the same division that the team is competing in at a tournament (called
 * {@link fll.db.Queries#getEventDivision(Connection, int) event division}). If
 * someone changes the database, this object does not notice the changes. It's a
 * snapshot in time from when the object was created.
 */
public class Team implements Serializable {

  /**
   * Constant to represent the team number for a bye
   */
  public static final int BYE_TEAM_NUMBER = -1;

  /**
   * Constant to represent the team number when there is a tie
   */
  public static final int TIE_TEAM_NUMBER = -2;

  /**
   * Constant to represent a NULL team entry in the playoff data table
   */
  public static final int NULL_TEAM_NUMBER = -3;

  /**
   * Team that represents a BYE
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
      return ComparisonUtils.compareStrings(one.getTeamName(), two.getTeamName());
    }
  };

  public Team(@JsonProperty("teamNumber") final int teamNumber,
              @JsonProperty("organization") final String org,
              @JsonProperty("teamName") final String name) {
    _teamNumber = teamNumber;
    _organization = org;
    _teamName = name;
  }

  /**
   * Builds a team object from its database info given the team number.
   * 
   * @param connection Database connection.
   * @param teamNumber Number of the team for which to build an object.
   * @return The new Team object or null if the team was not found in the
   *         database.
   * @throws SQLException on a database access error.
   */
  public static Team getTeamFromDatabase(final Connection connection,
                                         final int teamNumber) throws SQLException {
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

    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {

      stmt = connection.prepareStatement("SELECT Organization, TeamName FROM Teams"
          + " WHERE TeamNumber = ?");
      stmt.setInt(1, teamNumber);
      rs = stmt.executeQuery();
      if (rs.next()) {
        final String org = rs.getString(1);
        final String name = rs.getString(2);

        final Team x = new Team(teamNumber, org, name);
        return x;
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  private final int _teamNumber;

  /**
   * The team's number. This is the primary key for identifying a team.
   * 
   * @return team number
   */
  public int getTeamNumber() {
    return _teamNumber;
  }

  private final String _organization;

  /**
   * The organization that the team belongs to, this may be a school or youth
   * group.
   * 
   * @return organization
   */
  public String getOrganization() {
    return _organization;
  }

  private final String _teamName;

  /**
   * The name of the team.
   * 
   * @return name
   */
  public String getTeamName() {
    return _teamName;
  }

  /**
   * @return the team name shortened to {@link Team#MAX_TEAM_NAME_LEN}
   */
  @JsonIgnore
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
  public boolean equals(final Object o) {
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
        + getTeamNumber() + ": " + getTeamName() + "]";
  }

  /**
   * @return if this is an internal team
   */
  @JsonIgnore
  public boolean isInternal() {
    return isInternalTeamNumber(getTeamNumber());
  }

  /**
   * Check if this an internal team number.
   */
  public static boolean isInternalTeamNumber(final int number) {
    return number < 0;
  }
}
