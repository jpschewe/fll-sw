/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The static state of a team. This does not include information about the team
 * at a given tournament. Note that the {@link #getDivision() division}
 * attribute represents the division the team is entered in, which may not be
 * the same division that the team is competing in at a tournament (called
 * {@link fll.db.Queries#getEventDivision(Connection, int) event division}).
 * 
 * @version $Revision$
 */
public final class Team {

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
  public static final Team BYE = new Team();

  /**
   * Team that represents a TIE.
   */
  public static final Team TIE = new Team();

  /**
   * NULL Team.
   */
  public static final Team NULL = new Team();

  static {
    BYE.setTeamNumber(BYE_TEAM_NUMBER);
    BYE.setTeamName("BYE");
    TIE.setTeamNumber(TIE_TEAM_NUMBER);
    TIE.setTeamName("TIE");
    NULL.setTeamNumber(NULL_TEAM_NUMBER);
    NULL.setTeamName("NULL");
  }

  public Team() {

  }

  /**
   * Builds a team object from its database info given the team number.
   * 
   * @param connection
   *          Database connection.
   * @param teamNumber
   *          Number of the team for which to build an object.
   * @return The new Team object or null if the team was not found in the
   *         database.
   * @throws SQLException
   *           on a database access error.
   */
  public static Team getTeamFromDatabase(final Connection connection, final int teamNumber) throws SQLException {
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

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Division, Organization, Region, TeamName FROM Teams" + " WHERE TeamNumber = " + teamNumber);
      if (rs.next()) {
        Team x = new Team();
        x._division = rs.getString(1);
        x._organization = rs.getString(2);
        x._region = rs.getString(3);
        x._teamName = rs.getString(4);
        x._teamNumber = teamNumber;
        return x;
      } else {
        return null;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  private int _teamNumber;

  /**
   * The team's number. This is the primary key for identifying a team.
   * 
   * @return team number
   */
  public int getTeamNumber() {
    return _teamNumber;
  }

  public void setTeamNumber(final int v) {
    _teamNumber = v;
  }

  private String _organization;

  /**
   * The organization that the team belongs to, this may be a school or youth
   * group.
   * 
   * @return organization
   */
  public String getOrganization() {
    return _organization;
  }

  public void setOrganization(final String v) {
    _organization = v;
  }

  private String _teamName;

  /**
   * The name of the team.
   * 
   * @return name
   */
  public String getTeamName() {
    return _teamName;
  }

  public void setTeamName(final String v) {
    _teamName = v;
  }

  private String _region;

  /**
   * Region that the team comes from.
   * 
   * @return region
   */
  public String getRegion() {
    return _region;
  }

  public void setRegion(final String v) {
    _region = v;
  }

  private String _division;

  /**
   * The division that a team is entered as.
   * 
   * @return division
   */
  public String getDivision() {
    return _division;
  }

  public void setDivision(final String v) {
    _division = v;
  }

  private String _eventDivision;
  
  /**
   * The event division that a team is entered as.
   * 
   * @return division
   */
  public String getEventDivision() {
    return _eventDivision;
  }

  public void setEventDivision(final String v) {
    _eventDivision = v;
  }
  
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
    return "[" + getTeamNumber() + " " + getTeamName() + "]";
  }
}
