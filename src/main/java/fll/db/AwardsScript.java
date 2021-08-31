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
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.TournamentLevel;

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
     * Text for the performance category.
     */
    CATEGORY_PERFORMANCE,
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
    try (PreparedStatement prep = connection.prepareStatement("select text from awards_script_text"
        + "    WHERE section_name = ?"
        // season
        + "        AND ( (tournament_level_id = ? AND tournament_id = ?)"
        // tournament level
        + "            OR (tournament_level_id = ? AND tournament_id = ?)"
        // tournament
        + "            OR (tournament_level_id = ? AND tournament_id = ?) ) ORDER BY layer_rank DESC")) {
      prep.setString(1, section.name());
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
          return castNonNull(rs.getString("text"));
        } else {
          return "";
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
  public static boolean getSectionSpecifiedForSeason(final Connection connection,
                                                     final Section section)
      throws SQLException {
    return getSectionSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID,
                                  GenerateDB.INTERNAL_TOURNAMENT_ID, section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param level the tournament level
   * @return if the section has a value for the specified tournament level
   * @throws SQLException on a database error
   */
  public static boolean getSectionSpecifiedForTournamentLevel(final Connection connection,
                                                              final TournamentLevel level,
                                                              final Section section)
      throws SQLException {
    return getSectionSpecifiedFor(connection, level.getId(), GenerateDB.INTERNAL_TOURNAMENT_ID, section);
  }

  /**
   * @param connection database connection
   * @param section the section to check for
   * @param tournament the tournament
   * @return if the section has a value for the specified tournament
   * @throws SQLException on a database error
   */
  public static boolean getSectionSpecifiedForTournament(final Connection connection,
                                                         final Tournament tournament,
                                                         final Section section)
      throws SQLException {
    return getSectionSpecifiedFor(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID, tournament.getTournamentID(),
                                  section);
  }

  private static boolean getSectionSpecifiedFor(final Connection connection,
                                                final int tournamentLevelId,
                                                final int tournamentId,
                                                final Section section)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT section_name FROM awards_script_text" //
        + "  WHERE section_name = ?" //
        + "    AND tournament_level_id = ?" //
        + "    AND tournament_id = ?")) {
      prep.setString(1, section.name());
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
    try (PreparedStatement prep = connection.prepareStatement("SELECT text FROM awards_script_text" //
        + "  WHERE section_name = ?" //
        + "    AND tournament_level_id = ?" //
        + "    AND tournament_id = ?")) {
      prep.setString(1, section.name());
      prep.setInt(2, tournamentLevelId);
      prep.setInt(3, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return castNonNull(rs.getString(1));
        } else {
          return "";
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
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM awards_script_text" //
        + " WHERE tournament_level_id = ?" //
        + " AND tournament_id = ?" //
        + " AND section_name = ?")) {
      prep.setInt(1, tournamentLevelId);
      prep.setInt(2, tournamentId);
      prep.setString(3, section.name());
      prep.executeUpdate();
    }

    if (null != text) {
      try (PreparedStatement prep = connection.prepareStatement("INSERT INTO awards_script_text" //
          + " (tournament_level_id, tournament_id, layer_rank, section_name, text)" //
          + " VALUES(?, ?, ?, ?, ?)")) {
        prep.setInt(1, tournamentLevelId);
        prep.setInt(2, tournamentId);
        prep.setInt(3, layer.getRank());
        prep.setString(4, section.name());
        prep.setString(5, text);
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

}
