/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.db.GenerateDB;

/**
 * The representation of a tournament. If someone changes the database, this
 * object does not notice the changes. It's a snapshot in time from when the
 * object was created.
 */
public final class Tournament implements Serializable {

  private Tournament(final int tournamentID, final String name, final String location, final Tournament nextTournament) {
    this.tournamentID = tournamentID;
    this.name = name;
    this.location = location;
    this.nextTournament = nextTournament;
  }

  private final int tournamentID;

  public int getTournamentID() {
    return tournamentID;
  }

  private final String name;

  public String getName() {
    return name;
  }

  private final Tournament nextTournament;

  /**
   * The next tournament after this one.
   * 
   * @return null if there is no next tournament
   */
  public Tournament getNextTournament() {
    return nextTournament;
  }

  private final String location;

  public String getLocation() {
    return location;
  }

  /**
   * Create a tournament with a next tournament.
   */
  public static void createTournament(final Connection connection, final String tournamentName, final String location, final int nextTournamentID)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location, NextTournament) VALUES (?, ?, ?)");
      prep.setString(1, tournamentName);
      prep.setString(2, location);
      prep.setInt(3, nextTournamentID);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Create a tournament without a next tournament.
   */
  public static void createTournament(final Connection connection, final String tournamentName, final String location) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES (?, ?)");
      prep.setString(1, tournamentName);
      prep.setString(2, location);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Set the next tournament for the specified tournament.
   */
  public static void setNextTournament(final Connection connection, final String tournamentName, final String nextName) throws SQLException {
    PreparedStatement setNext = null;
    try {
      setNext = connection.prepareStatement("UPDATE Tournaments SET NextTournament = ? WHERE Name = ?");
      setNext.setString(2, tournamentName);
      if (null == nextName
          || "".equals(nextName.trim())) {
        setNext.setNull(1, Types.INTEGER);
      } else {
        final Tournament next = findTournamentByName(connection, nextName);        
        setNext.setInt(1, next.getTournamentID());
      }
      setNext.executeUpdate();
    } finally {
      SQLFunctions.close(setNext);
    }
  }

  /**
   * Get a list of tournaments in the DB ordered by Name. This excludes the
   * internal tournament.
   * 
   * @return list of tournament tournaments
   */
  public static List<Tournament> getTournaments(final Connection connection) throws SQLException {
    final List<Tournament> retval = new LinkedList<Tournament>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT tournament_id, Name, Location, NextTournament FROM Tournaments WHERE tournament_id <> ? ORDER BY Name");
      prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_ID);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int tournamentID = rs.getInt(1);
        final String name = rs.getString(2);
        final String location = rs.getString(3);
        final int nextTournamentID = rs.getInt(4);
        final Tournament nextTournament;
        if(rs.wasNull()) {
          nextTournament = null;
        } else {
          nextTournament = findTournamentByID(connection, nextTournamentID);
        }

        final Tournament tournament = new Tournament(tournamentID, name, location, nextTournament);
        retval.add(tournament);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return retval;
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified name.
   * 
   * @param connection the database connection
   * @param name the name of a tournament to find
   * @return the Tournament, or null if it cannot be found
   * @throws SQLException
   */
  public static Tournament findTournamentByName(final Connection connection, final String name) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT tournament_id, Location, NextTournament FROM Tournaments WHERE Name = ?");
      prep.setString(1, name);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int id = rs.getInt(1);
        final String location = rs.getString(2);
        final int nextID = rs.getInt(3);
        final Tournament next;
        if (rs.wasNull()) {
          next = null;
        } else {
          next = findTournamentByID(connection, nextID);
        }
        return new Tournament(id, name, location, next);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Create a {@link Tournament} for the tournament with the specified id.
   * 
   * @param connection the database connection
   * @param tournamentID the ID to find
   * @return the Tournament, or null if it cannot be found
   * @throws SQLException
   */
  public static Tournament findTournamentByID(final Connection connection, final int tournamentID) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Name, Location, NextTournament FROM Tournaments WHERE tournament_id = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      if (rs.next()) {
        final String name = rs.getString(1);
        final String location = rs.getString(2);
        final int nextID = rs.getInt(3);
        final Tournament next;
        if (rs.wasNull()) {
          next = null;
        } else {
          next = findTournamentByID(connection, nextID);
        }
        return new Tournament(tournamentID, name, location, next);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  public boolean equals(final Object o) {
    if (o instanceof Tournament) {
      final Tournament other = (Tournament) o;
      return other.getTournamentID() == getTournamentID();
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getTournamentID();
  }

  //
  // public static Collection<Tournament> getAllTournaments(final Connection
  // connection) throws SQLException {
  // Statement stmt = null;
  // ResultSet rs = null;
  // try {
  // stmt = connection.createStatement();
  // rs =
  // stmt.executeQuery("SELECT Name, Location, NextTournament, tournament_id FROM Tournaments ORDER BY Name");
  // while(rs.next()) {
  // final String name = rs.getString(1);
  // final String location = rs.getString(2);
  // final int next = rs.getInt(3);
  // final String nextName;
  // if (!rs.wasNull()) {
  // nextName = Queries.getTournamentName(connection, next);
  // } else {
  // nextName = null;
  // }
  // final int tournamentID = rs.getInt(4);
  // generateRow(out, row, tournamentID, name, location, nextName);
  // }
  // } finally {
  // SQLFunctions.closeResultSet(rs);
  // SQLFunctions.closeStatement(stmt);
  // }
  // }

  @Override
  public String toString() {
    return getName() + "(" + getTournamentID() + ") - " + getLocation();
  }
}
