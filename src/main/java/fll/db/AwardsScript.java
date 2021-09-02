/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentLevel;
import fll.util.FLLInternalException;

/**
 * Methods for working with the awards script.
 */
public final class AwardsScript {

  /*
   * Implementation note: The internal tournament and tournament level ids are
   * used as values in the database for cases where the tournament or level are
   * not used.
   */

  private AwardsScript() {
  }

  /**
   * Macros available for the text sections.
   */
  public enum Macro {
    /**
     * Number of trained officials at the tournament.
     */
    NUM_TRAINED_OFFICIALS("numTrainedOfficials", "Number of Trained Officials"),
    /**
     * Name of the host school / location.
     */
    HOST_SCHOOL("hostSchool", "Host School"),
    /**
     * Tournament directors for the tournament.
     */
    TOURNAMENT_DIRECTORS("tournamentDirectors", "Tournament Directors"),
    /**
     * Name of the tournament level of the tournament.
     */
    TOURNAMENT_LEVEL("tournamentLevel", "Tournament Level"),
    /**
     * Name of the next tournament level.
     */
    TOURNAMENT_NEXT_LEVEL("tournamentNextLevel", "Next Tournament Level"),
    /**
     * Number of teams advancing from the tournament.
     */
    NUM_TEAMS_ADVANCING("numTeamsAdvancing", "Number of Teams Advancing");

    Macro(final String text,
          final String title) {
      this.text = text;
      this.title = title;
    }

    private final String text;

    /**
     * @return the macro value that shows up in the text and is to be replaced
     */
    public String getText() {
      return text;
    }

    private final String title;

    /**
     * @return the title to display
     */
    public String getTitle() {
      return title;
    }

  }

  /**
   * A section in the awards script that can be customized.
   */
  public enum Section {
    /**
     * Front text.
     */
    FRONT_MATTER,
    /**
     * Introduction of the sponsors.
     */
    SPONSORS_INTRO,
    /**
     * Recognition of the sponsors.
     */
    SPONSORS_RECOGNITION,
    /**
     * Volunteers recognition.
     */
    VOLUNTEERS,
    /**
     * Text for the championship category.
     */
    CATEGORY_CHAMPIONSHIP,
    /**
     * Presenter for the championship category.
     */
    CATEGORY_CHAMPIONSHIP_PRESENTER,
    /**
     * Text for the performance category.
     */
    CATEGORY_PERFORMANCE,
    /**
     * Presenter for the performance category.
     */
    CATEGORY_PERFORMANCE_PRESENTER,
    /**
     * Text for the end of the awards, before the advancing teams.
     */
    END_AWARDS,
    /**
     * Final text for the script.
     */
    FOOTER;

    /**
     * @return identifier to be used inside webpages
     */
    public String getIdentifier() {
      return "section_"
          + name();
    }
  }

  /**
   * Awards script layer.
   */
  public enum Layer {
    /**
     * Applies to all tournaments.
     */
    SEASON(1),
    /**
     * Applies to all tournaments with the specified level.
     */
    TOURNAMENT_LEVEL(2),
    /**
     * Applies to the individual tournament.
     */
    TOURNAMENT(3);

    Layer(final int rank) {
      this.rank = rank;
    }

    private final int rank;

    /**
     * Used for choosing which layer to use values from. The highest rank is used.
     * 
     * @return the rank of the layer.
     */
    public int getRank() {
      return rank;
    }
  }

  /**
   * Get the text for a section of the awards script.
   * 
   * @param connection database connection
   * @param tournament the tournament that the script is being generated for
   * @param section the section in the script
   * @return the text
   * @throws SQLException on a database error
   */
  public static String getSectionText(final Connection connection,
                                      final Tournament tournament,
                                      final Section section)
      throws SQLException {
    return getValue(connection, tournament, "awards_script_text", "section_name", section.name(), "text", "");
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table and column names are passed in")
  private static String getValue(final Connection connection,
                                 final Tournament tournament,
                                 final String tableName,
                                 final String keyColumn,
                                 final String keyValue,
                                 final String valueColumn,
                                 final String defaultValue)
      throws SQLException {
    final Formatter sql = new Formatter();
    sql.format("SELECT %s FROM %s WHERE %s = ?", valueColumn, tableName, keyColumn);
    // season
    sql.format("        AND ( (tournament_level_id = ? AND tournament_id = ?)");
    // tournament level
    sql.format("            OR (tournament_level_id = ? AND tournament_id = ?)");
    // tournament
    sql.format("            OR (tournament_level_id = ? AND tournament_id = ?) ) ORDER BY layer_rank DESC");
    try (PreparedStatement prep = connection.prepareStatement(sql.toString())) {
      prep.setString(1, keyValue);
      // season
      prep.setInt(2, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      prep.setInt(3, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament level
      prep.setInt(4, tournament.getLevel().getId());
      prep.setInt(5, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament
      prep.setInt(6, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      prep.setInt(7, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return castNonNull(rs.getString(1));
        } else {
          return defaultValue;
        }

      }
    }
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @return if the section has a value for the season
   * @throws SQLException on a database error
   */
  public static boolean isSectionSpecifiedForSeason(final Connection connection,
                                                    final Section section)
      throws SQLException {
    return isSectionSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                                 section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param level the tournament level
   * @return if the section has a value for the specified tournament level
   * @throws SQLException on a database error
   */
  public static boolean isSectionSpecifiedForTournamentLevel(final Connection connection,
                                                             final TournamentLevel level,
                                                             final Section section)
      throws SQLException {
    return isSectionSpecifiedFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param tournament the tournament
   * @return if the section has a value for the specified tournament
   * @throws SQLException on a database error
   */
  public static boolean isSectionSpecifiedForTournament(final Connection connection,
                                                        final Tournament tournament,
                                                        final Section section)
      throws SQLException {
    return isSectionSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                                 section);
  }

  private static boolean isSectionSpecifiedFor(final Connection connection,
                                               final int tournamentLevelId,
                                               final int tournamentId,
                                               final Section section)
      throws SQLException {
    return isValueSpecifiedFor(connection, tournamentLevelId, tournamentId, "awards_script_text", "section_name",
                               section.name());
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table and column names are passed in")
  private static boolean isValueSpecifiedFor(final Connection connection,
                                             final int tournamentLevelId,
                                             final int tournamentId,
                                             final String tableName,
                                             final String keyColumn,
                                             final String keyValue)
      throws SQLException {
    final Formatter sql = new Formatter();
    sql.format("SELECT %s FROM %s", keyColumn, tableName);
    sql.format("  WHERE %s = ?", keyColumn);
    sql.format("    AND tournament_level_id = ?");
    sql.format("    AND tournament_id = ?");

    try (PreparedStatement prep = connection.prepareStatement(sql.toString())) {
      prep.setString(1, keyValue);
      prep.setInt(2, tournamentLevelId);
      prep.setInt(3, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @return section text for the season
   * @throws SQLException on a database error
   */
  public static String getSectionTextForSeason(final Connection connection,
                                               final Section section)
      throws SQLException {
    return getSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                             section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param level the tournament level
   * @return text for the specified tournament level
   * @throws SQLException on a database error
   */
  public static String getSectionTextForTournamentLevel(final Connection connection,
                                                        final TournamentLevel level,
                                                        final Section section)
      throws SQLException {
    return getSectionTextFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param tournament the tournament
   * @return section text for the specified tournament
   * @throws SQLException on a database error
   */
  public static String getSectionTextForTournament(final Connection connection,
                                                   final Tournament tournament,
                                                   final Section section)
      throws SQLException {
    return getSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                             section);
  }

  private static String getSectionTextFor(final Connection connection,
                                          final int tournamentLevelId,
                                          final int tournamentId,
                                          final Section section)
      throws SQLException {
    return getValueFor(connection, tournamentLevelId, tournamentId, "awards_script_text", "section_name",
                       section.name(), "text", "");
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table and column names are passed in")
  private static String getValueFor(final Connection connection,
                                    final int tournamentLevelId,
                                    final int tournamentId,
                                    final String tableName,
                                    final String keyColumn,
                                    final String keyValue,
                                    final String valueColumn,
                                    final String defaultValue)
      throws SQLException {
    final Formatter sql = new Formatter();
    sql.format("SELECT %s FROM %s", valueColumn, tableName);
    sql.format("  WHERE %s = ?", keyColumn);
    sql.format("    AND tournament_level_id = ?");
    sql.format("    AND tournament_id = ?");

    try (PreparedStatement prep = connection.prepareStatement(sql.toString())) {
      prep.setString(1, keyValue);
      prep.setInt(2, tournamentLevelId);
      prep.setInt(3, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return castNonNull(rs.getString(1));
        } else {
          return defaultValue;
        }
      }
    }
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @throws SQLException on a database error
   */
  public static void clearSectionTextForSeason(final Connection connection,
                                               final Section section)
      throws SQLException {
    updateSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                         Layer.SEASON, section, null);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param level the tournament level
   * @throws SQLException on a database error
   */
  public static void clearSectionTextForTournamentLevel(final Connection connection,
                                                        final TournamentLevel level,
                                                        final Section section)
      throws SQLException {
    updateSectionTextFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, Layer.TOURNAMENT_LEVEL, section,
                         null);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param tournament the tournament
   * @throws SQLException on a database error
   */
  public static void clearSectionTextForTournament(final Connection connection,
                                                   final Tournament tournament,
                                                   final Section section)
      throws SQLException {
    updateSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                         Layer.TOURNAMENT, section, null);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param text the new text
   * @throws SQLException on a database error
   */
  public static void updateSectionTextForSeason(final Connection connection,
                                                final Section section,
                                                final String text)
      throws SQLException {
    updateSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                         Layer.SEASON, section, text);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param level the tournament level
   * @param text the new text
   * @throws SQLException on a database error
   */
  public static void updateSectionTextForTournamentLevel(final Connection connection,
                                                         final TournamentLevel level,
                                                         final Section section,
                                                         final String text)
      throws SQLException {
    updateSectionTextFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, Layer.TOURNAMENT_LEVEL, section,
                         text);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param tournament the tournament
   * @param text the new text
   * @throws SQLException on a database error
   */
  public static void updateSectionTextForTournament(final Connection connection,
                                                    final Tournament tournament,
                                                    final Section section,
                                                    final String text)
      throws SQLException {
    updateSectionTextFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                         Layer.TOURNAMENT, section, text);
  }

  private static void updateSectionTextFor(final Connection connection,
                                           final int tournamentLevelId,
                                           final int tournamentId,
                                           final Layer layer,
                                           final Section section,
                                           final @Nullable String text)
      throws SQLException {
    updateValueFor(connection, tournamentLevelId, tournamentId, layer, "awards_script_text", "section_name",
                   section.name(), "text", text);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table and column names are passed in")
  private static void updateValueFor(final Connection connection,
                                     final int tournamentLevelId,
                                     final int tournamentId,
                                     final Layer layer,
                                     final String tableName,
                                     final String keyColumn,
                                     final String keyValue,
                                     final String valueColumn,
                                     final @Nullable String newValue)
      throws SQLException {
    final Formatter deleteSql = new Formatter();
    deleteSql.format("DELETE FROM %s", tableName);
    deleteSql.format(" WHERE tournament_level_id = ?");
    deleteSql.format(" AND tournament_id = ?");
    deleteSql.format(" AND %s = ?", keyColumn);
    try (PreparedStatement prep = connection.prepareStatement(deleteSql.toString())) {
      prep.setInt(1, tournamentLevelId);
      prep.setInt(2, tournamentId);
      prep.setString(3, keyValue);
      prep.executeUpdate();
    }

    if (null != newValue) {
      final Formatter insertSql = new Formatter();
      insertSql.format("INSERT INTO %s", tableName);
      insertSql.format(" (tournament_level_id, tournament_id, layer_rank, %s, %s)", keyColumn, valueColumn);
      insertSql.format(" VALUES(?, ?, ?, ?, ?)");

      try (PreparedStatement prep = connection.prepareStatement(insertSql.toString())) {
        prep.setInt(1, tournamentLevelId);
        prep.setInt(2, tournamentId);
        prep.setInt(3, layer.getRank());
        prep.setString(4, keyValue);
        prep.setString(5, newValue);
        prep.executeUpdate();
      }
    }

  }

  /**
   * Get the sponsors for the script.
   * 
   * @param connection database connection
   * @param tournament the tournament that the script is being generated for
   * @return the sponsors in order
   * @throws SQLException on a database error
   */
  public static List<String> getSponsors(final Connection connection,
                                         final Tournament tournament)
      throws SQLException {
    try (
        PreparedStatement findLayer = connection.prepareStatement("SELECT MAX(layer_rank) FROM awards_script_award_order"
            + "    WHERE "
            // season
            + "        ( (tournament_level_id = ? AND tournament_id = ?)"
            // tournament level
            + "            OR (tournament_level_id = ? AND tournament_id = ?)"
            // tournament
            + "            OR (tournament_level_id = ? AND tournament_id = ?) ) ORDER BY layer_rank DESC")) {
      // season
      findLayer.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      findLayer.setInt(2, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament level
      findLayer.setInt(3, tournament.getLevel().getId());
      findLayer.setInt(4, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament
      findLayer.setInt(5, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      findLayer.setInt(6, tournament.getTournamentID());

      try (ResultSet layer = findLayer.executeQuery()) {
        if (layer.next()) {
          final int layerRank = layer.getInt(1);

          try (PreparedStatement prep = connection.prepareStatement("SELECT award FROM awards_script_award_order"
              + "    WHERE "
              // season
              + "        ( (tournament_level_id = ? AND tournament_id = ?)"
              // tournament level
              + "            OR (tournament_level_id = ? AND tournament_id = ?)"
              // tournament
              + "            OR (tournament_level_id = ? AND tournament_id = ?) )"
              + "        AND layer_rank = ?"
              + "    ORDER BY award_rank ASC")) {
            // season
            prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
            prep.setInt(2, GenerateDB.INTERNAL_TOURNAMENT_ID);

            // tournament level
            prep.setInt(3, tournament.getLevel().getId());
            prep.setInt(4, GenerateDB.INTERNAL_TOURNAMENT_ID);

            // tournament
            prep.setInt(5, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
            prep.setInt(6, tournament.getTournamentID());

            prep.setInt(7, layerRank);

            try (ResultSet rs = prep.executeQuery()) {
              final List<String> sponsors = new LinkedList<>();

              while (rs.next()) {
                sponsors.add(castNonNull(rs.getString(1)));
              }

              return sponsors;
            }

          }
        } else {
          // can't find anything
          return Collections.emptyList();
        }

      }
    }
  }

  /**
   * @param connection database connection
   * @param macro the macro to check for
   * @return if the parameter has a value for the season
   * @throws SQLException on a database error
   */
  public static boolean isMacroSpecifiedForSeason(final Connection connection,
                                                  final Macro macro)
      throws SQLException {
    return isParameterSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID,
                                   GenerateDB.INTERNAL_TOURNAMENT_ID, macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to check for
   * @param level the tournament level
   * @return if the section has a value for the specified tournament level
   * @throws SQLException on a database error
   */
  public static boolean isMacroSpecifiedForTournamentLevel(final Connection connection,
                                                           final TournamentLevel level,
                                                           final Macro macro)
      throws SQLException {
    return isParameterSpecifiedFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to check for
   * @param tournament the tournament
   * @return if the section has a value for the specified tournament
   * @throws SQLException on a database error
   */
  public static boolean isMacroSpecifiedForTournament(final Connection connection,
                                                      final Tournament tournament,
                                                      final Macro macro)
      throws SQLException {
    return isParameterSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                                   macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to get the value for
   * @return macro value for the season
   * @throws SQLException on a database error
   */
  public static String getMacroValueForSeason(final Connection connection,
                                              final Macro macro)
      throws SQLException {
    return getParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                                macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to get the value for
   * @param level the tournament level
   * @return macro value for the specified tournament level
   * @throws SQLException on a database error
   */
  public static String getMacroValueForTournamentLevel(final Connection connection,
                                                       final TournamentLevel level,
                                                       final Macro macro)
      throws SQLException {
    return getParameterValueFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to get the value for
   * @param tournament the tournament
   * @return macro value for the specified tournament
   * @throws SQLException on a database error
   */
  public static String getMacroValueForTournament(final Connection connection,
                                                  final Tournament tournament,
                                                  final Macro macro)
      throws SQLException {
    return getParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                                macro.name());
  }

  /**
   * @param connection database connection
   * @param macro the macro to clear the value
   * @throws SQLException on a database error
   */
  public static void clearMacroValueForSeason(final Connection connection,
                                              final Macro macro)
      throws SQLException {
    updateParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                            Layer.SEASON, macro.name(), null);
  }

  /**
   * @param connection database connection
   * @param macro the macro to clear the value
   * @param level the tournament level
   * @throws SQLException on a database error
   */
  public static void clearMacroValueForTournamentLevel(final Connection connection,
                                                       final TournamentLevel level,
                                                       final Macro macro)
      throws SQLException {
    updateParameterValueFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, Layer.TOURNAMENT_LEVEL,
                            macro.name(), null);
  }

  /**
   * @param connection database connection
   * @param macro the macro to clear the value
   * @param tournament the tournament
   * @throws SQLException on a database error
   */
  public static void clearMacroValueForTournament(final Connection connection,
                                                  final Tournament tournament,
                                                  final Macro macro)
      throws SQLException {
    updateParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                            Layer.TOURNAMENT, macro.name(), null);
  }

  /**
   * @param connection database connection
   * @param macro the macro to update the value of
   * @param value the new value
   * @throws SQLException on a database error
   */
  public static void updateMacroValueForSeason(final Connection connection,
                                               final Macro macro,
                                               final String value)
      throws SQLException {
    updateParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, GenerateDB.INTERNAL_TOURNAMENT_ID,
                            Layer.SEASON, macro.name(), value);
  }

  /**
   * @param connection database connection
   * @param macro the macro to update the value of
   * @param level the tournament level
   * @param value the new value
   * @throws SQLException on a database error
   */
  public static void updateMacroValueForTournamentLevel(final Connection connection,
                                                        final TournamentLevel level,
                                                        final Macro macro,
                                                        final String value)
      throws SQLException {
    updateParameterValueFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, Layer.TOURNAMENT_LEVEL,
                            macro.name(), value);
  }

  /**
   * @param connection database connection
   * @param macro the macro to update the value of
   * @param tournament the tournament
   * @param value the new value
   * @throws SQLException on a database error
   */
  public static void updateMacroValueForTournament(final Connection connection,
                                                   final Tournament tournament,
                                                   final Macro macro,
                                                   final String value)
      throws SQLException {
    updateParameterValueFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                            Layer.TOURNAMENT, macro.name(), value);
  }

  private static boolean isParameterSpecifiedFor(final Connection connection,
                                                 final int tournamentLevelId,
                                                 final int tournamentId,
                                                 final String paramName)
      throws SQLException {
    return isValueSpecifiedFor(connection, tournamentLevelId, tournamentId, "awards_script_parameters", "param_name",
                               paramName);
  }

  private static String getParameterValueFor(final Connection connection,
                                             final int tournamentLevelId,
                                             final int tournamentId,
                                             final String paramName)
      throws SQLException {
    return getValueFor(connection, tournamentLevelId, tournamentId, "awards_script_parameters", "param_name", paramName,
                       "param_value", "");
  }

  private static void updateParameterValueFor(final Connection connection,
                                              final int tournamentLevelId,
                                              final int tournamentId,
                                              final Layer layer,
                                              final String paramName,
                                              final @Nullable String paramValue)
      throws SQLException {
    updateValueFor(connection, tournamentLevelId, tournamentId, layer, "awards_script_parameters", "param_name",
                   paramName, "param_value", paramValue);
  }

  /**
   * Get the text for a macro.
   * 
   * @param connection database connection
   * @param tournament the tournament that the script is being generated for
   * @param macro the macro in the script
   * @return the value, "UNKNOWN" if it cannot be resolved
   * @throws SQLException on a database error
   */
  public static String getMacroValue(final Connection connection,
                                     final Tournament tournament,
                                     final Macro macro)
      throws SQLException {

    switch (macro) {
    case TOURNAMENT_LEVEL:
      return tournament.getLevel().getName();
    case TOURNAMENT_NEXT_LEVEL: {
      final int nextLevelId = tournament.getLevel().getNextLevelId();
      if (TournamentLevel.NO_NEXT_LEVEL_ID == nextLevelId) {
        return "No Next Level";
      } else {
        return TournamentLevel.getById(connection, nextLevelId).getName();
      }
    }
    case HOST_SCHOOL:
    case NUM_TEAMS_ADVANCING:
    case NUM_TRAINED_OFFICIALS:
    case TOURNAMENT_DIRECTORS: {
      return getValue(connection, tournament, "awards_script_parameters", "param_name", macro.name(), "param_value",
                      "UNKNOWN");
    }
    default:
      throw new FLLInternalException("Unknown macro: "
          + macro);
    }
  }

  private static @Nullable String getParameterValue(final Connection connection,
                                                    final Tournament tournament,
                                                    final String paramName)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("select param_value from awards_script_parameters"
        + "    WHERE param_name = ?"
        // season
        + "        AND ( (tournament_level_id = ? AND tournament_id = ?)"
        // tournament level
        + "            OR (tournament_level_id = ? AND tournament_id = ?)"
        // tournament
        + "            OR (tournament_level_id = ? AND tournament_id = ?) ) ORDER BY layer_rank DESC")) {
      prep.setString(1, paramName);
      // season
      prep.setInt(2, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      prep.setInt(3, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament level
      prep.setInt(4, tournament.getLevel().getId());
      prep.setInt(5, GenerateDB.INTERNAL_TOURNAMENT_ID);

      // tournament
      prep.setInt(6, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      prep.setInt(7, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getString("param_value");
        } else {
          return null;
        }

      }
    }
  }

}