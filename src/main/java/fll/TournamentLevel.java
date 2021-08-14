/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.db.GenerateDB;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;

/**
 * The level of a tournament, such as "Regional", "Sectional", "State".
 */
public final class TournamentLevel implements Serializable {

  /**
   * ID used to signify that there is no next level.
   */
  public static final int NO_NEXT_LEVEL_ID = -1;

  /**
   * @param id {@link #getId()}
   * @param name {@link #getName()}
   * @param nextLevelId {@link #getNextLevelId()}
   */
  private TournamentLevel(@JsonProperty("id") final int id,
                          @JsonProperty("name") final String name,
                          @JsonProperty("nextLevelId") final int nextLevelId) {
    this.name = name;
    this.id = id;
    this.nextLevelId = nextLevelId;
  }

  private final String name;

  /**
   * @return the name of the level.
   */
  public String getName() {
    return name;
  }

  private final int id;

  /**
   * @return the internal ID of the level
   */
  public int getId() {
    return id;
  }

  private final int nextLevelId;

  /**
   * Name of the level that is created by default.
   */
  public static final String DEFAULT_TOURNAMENT_LEVEL_NAME = "Level I";

  /**
   * @return id of the next level, {@link #NO_NEXT_LEVEL_ID} if there is no next
   *         level
   */
  public int getNextLevelId() {
    return nextLevelId;
  }

  /**
   * @param connection database connection
   * @param name name of the new tournament level
   * @param nextLevel the next tournament level
   * @return the newly created tournament level
   * @throws SQLException on a database error
   * @throws DuplicateTournamentLevelException if a tournament level with
   *           {@code name} already exists
   */
  public static TournamentLevel createTournamentLevel(final Connection connection,
                                                      final String name,
                                                      final TournamentLevel nextLevel)
      throws SQLException, DuplicateTournamentLevelException {
    return createTournamentLevel(connection, name, nextLevel.getId());
  }

  /**
   * Create a tournament level that has no next level.
   * 
   * @param connection database connection
   * @param name name of the level to create
   * @return the new tournament level
   * @throws SQLException on a database error
   * @throws DuplicateTournamentLevelException if a tournament level with
   *           {@code name} already exists
   * @see #NO_NEXT_LEVEL_ID
   */
  public static TournamentLevel createTournamentLevel(final Connection connection,
                                                      final String name)
      throws SQLException, DuplicateTournamentLevelException {
    return createTournamentLevel(connection, name, NO_NEXT_LEVEL_ID);
  }

  private static TournamentLevel createTournamentLevel(final Connection connection,
                                                       final String name,
                                                       final int nextLevelId)
      throws SQLException, DuplicateTournamentLevelException {
    if (levelExists(connection, name)) {
      throw new DuplicateTournamentLevelException(name);
    }

    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO tournament_level (level_name, next_level_id) VALUES(?, ?)")) {
      prep.setString(1, name);
      prep.setInt(2, nextLevelId);

      prep.executeUpdate();
    }

    try {
      return getByName(connection, name);
    } catch (final NoSuchTournamentLevelException e) {
      throw new FLLInternalException("Cannot find tournament level that was just created", e);
    }

  }

  /**
   * Check if a level exists with the specified {@link #getName()}.
   * 
   * @param connection database connection
   * @param name name of the level to find
   * @return true if the level is found in the database
   * @throws SQLException on a database error
   */
  public static boolean levelExists(final Connection connection,
                                    final String name)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT level_id FROM tournament_level WHERE level_name = ?")) {
      prep.setString(1, name);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Check if a level exists with the specified {@link #getId()}.
   * 
   * @param connection database connection
   * @param id id of the level to find
   * @return true if the level is found in the database
   * @throws SQLException on a database error
   */
  public static boolean levelExists(final Connection connection,
                                    final int id)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT level_name FROM tournament_level WHERE level_id = ?")) {
      prep.setInt(1, id);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * @param connection database connection
   * @return all levels specified in the database, excludes the internal
   *         tournament level
   * @throws SQLException on a database error
   */
  public static Set<TournamentLevel> getAllLevels(final Connection connection) throws SQLException {
    final Set<TournamentLevel> levels = new HashSet<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT level_id, level_name, next_level_id FROM tournament_level WHERE level_id <> ?")) {
      prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int id = rs.getInt("level_id");
          final String name = castNonNull(rs.getString("level_name"));
          final int nextId = rs.getInt("next_level_id");
          final TournamentLevel level = new TournamentLevel(id, name, nextId);
          levels.add(level);
        }
      }
    }
    return levels;
  }

  /**
   * Get a level by name.
   * 
   * @param connection database connection
   * @param name the name of the level to find
   * @return the level
   * @throws SQLException on a database error
   * @throws NoSuchTournamentLevelException if a level with the specified name
   *           doesn't exist in the database
   */
  public static TournamentLevel getByName(final Connection connection,
                                          final String name)
      throws SQLException, NoSuchTournamentLevelException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT level_id, next_level_id FROM tournament_level WHERE level_name = ?")) {
      prep.setString(1, name);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int id = rs.getInt("level_id");
          final int nextId = rs.getInt("next_level_id");
          final TournamentLevel level = new TournamentLevel(id, name, nextId);
          return level;
        } else {
          throw new NoSuchTournamentLevelException(name);
        }
      }
    }
  }

  /**
   * Get a level by id.
   * 
   * @param connection database connection
   * @param id the identifier of the level to find
   * @return the level
   * @throws SQLException on a database error
   * @throws NoSuchTournamentLevelException if a level with the specified id
   *           doesn't exist in the database
   */
  public static TournamentLevel getById(final Connection connection,
                                        final int id)
      throws SQLException, NoSuchTournamentLevelException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT level_name, next_level_id FROM tournament_level WHERE level_id = ?")) {
      prep.setInt(1, id);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String name = castNonNull(rs.getString("level_name"));
          final int nextId = rs.getInt("next_level_id");
          final TournamentLevel level = new TournamentLevel(id, name, nextId);
          return level;
        } else {
          throw new NoSuchTournamentLevelException(id);
        }
      }
    }
  }

  /**
   * Thrown when a {@link TournamentLevel} doesn't exist.
   */
  public static final class NoSuchTournamentLevelException extends FLLRuntimeException {

    /**
     * @param name {@link TournamentLevel#getName()}
     */
    public NoSuchTournamentLevelException(final String name) {
      super(String.format("Tournament level with name '%s' does not exist", name));
    }

    /**
     * @param id {@link TournamentLevel#getId()}
     */
    public NoSuchTournamentLevelException(final int id) {
      super(String.format("Tournament level with id '%d' does not exist", id));
    }
  }

  /**
   * Thrown when the tournament level name already exists.
   */
  public static final class DuplicateTournamentLevelException extends FLLRuntimeException {

    /**
     * @param name {@link TournamentLevel#getName()}
     */
    public DuplicateTournamentLevelException(final String name) {
      super(String.format("Tournament level with name '%s' already exists", name));
    }

  }

  /**
   * Replace all tournament levels in the database.
   * 
   * @param connection database connection
   * @param levels the new tournament levels
   * @throws SQLException on a database error
   * @throws NoSuchTournamentLevelException if there is a reference to a next
   *           level that doesn't exist in the set
   */
  public static void storeTournamentLevels(final Connection connection,
                                           final Set<TournamentLevel> levels)
      throws SQLException {

    // validate the levels first
    final Set<Integer> knownLevels = levels.stream().map(TournamentLevel::getId).collect(Collectors.toSet());
    for (final TournamentLevel level : levels) {
      if (level.getNextLevelId() != NO_NEXT_LEVEL_ID
          && !knownLevels.contains(level.getNextLevelId())) {
        throw new NoSuchTournamentLevelException(level.getNextLevelId());
      }
    }

    final boolean prevAutoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);
      try (Statement delete = connection.createStatement()) {
        delete.executeUpdate("DELETE FROM tournament_level");
      }

      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO tournament_level"
          + " (level_id, level_name, next_level_id)"
          + " VALUES(?, ?, ?)")) {

        for (final TournamentLevel level : levels) {
          insert.setInt(1, level.getId());
          insert.setString(2, level.getName());
          insert.setInt(3, level.getNextLevelId());
          insert.addBatch();
        }

        insert.executeBatch();
      }

      try {
        connection.commit();
      } catch (final SQLException e) {
        connection.rollback();
      }
    } finally {
      connection.setAutoCommit(prevAutoCommit);
    }

  }

  /**
   * @param connection database connection
   * @param level the level to check
   * @return the number of tournaments at the specified level
   * @throws SQLException on a database error
   */
  public static int getNumTournamentsAtLevel(final Connection connection,
                                             final TournamentLevel level)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM tournaments WHERE level_id = ?")) {
      prep.setInt(1, level.getId());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        } else {
          return 0;
        }
      }
    }
  }

  /**
   * Update information about a tournament level.
   * 
   * @param connection database connection
   * @param levelId the id of the level
   * @param name the new name of the level
   * @param nextLevel the new next level
   * @throws SQLException on a database error
   * @throws DuplicateTournamentLevelException if another tournament level exists
   *           with the same name
   */
  public static void updateTournamentLevel(final Connection connection,
                                           final int levelId,
                                           final String name,
                                           final TournamentLevel nextLevel)
      throws SQLException, DuplicateTournamentLevelException {
    updateTournamentLevel(connection, levelId, name, nextLevel.getId());
  }

  /**
   * Update information about a tournament level and specify that the tournament
   * has no next level.
   * 
   * @param connection database connection
   * @param levelId the id of the level
   * @param name the new name of the level
   * @throws SQLException on a database error
   * @throws DuplicateTournamentLevelException if another tournament level exists
   *           with the same name
   */
  public static void updateTournamentLevel(final Connection connection,
                                           final int levelId,
                                           final String name)
      throws SQLException, DuplicateTournamentLevelException {
    updateTournamentLevel(connection, levelId, name, NO_NEXT_LEVEL_ID);
  }

  private static void updateTournamentLevel(final Connection connection,
                                            final int levelId,
                                            final String name,
                                            final int nextLevelId)
      throws SQLException, DuplicateTournamentLevelException {
    if (levelExists(connection, name)) {
      final TournamentLevel check = getByName(connection, name);
      if (check.getId() != levelId) {
        throw new DuplicateTournamentLevelException(name);
      }
    }

    try (PreparedStatement prep = connection.prepareStatement("UPDATE tournament_level" //
        + " SET level_name = ?" //
        + "    ,next_level_id = ?" //
        + " WHERE level_id = ?" //
    )) {
      prep.setString(1, name);
      prep.setInt(2, nextLevelId);
      prep.setInt(3, levelId);
      prep.executeUpdate();
    }
  }

  /**
   * Delete a level.
   * 
   * @param connection database connection
   * @param level the level to delete
   * @throws SQLException on a database error
   */
  public static void deleteLevel(final Connection connection,
                                 final TournamentLevel level)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM tournament_level WHERE level_id = ?")) {
      prep.setInt(1, level.getId());
      prep.executeUpdate();
    }
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (this.getClass().equals(o.getClass())) {
      final TournamentLevel other = (TournamentLevel) o;
      return getId() == other.getId();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getId();
  }

}
