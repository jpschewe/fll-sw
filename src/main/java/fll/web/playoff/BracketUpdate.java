/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.Serializable;

import javax.annotation.Nullable;

import fll.Team;
import fll.xml.ScoreType;

/**
 * An update to the playoff brackets display.
 */
public final class BracketUpdate implements Serializable {
  /**
   * Name of the bracket.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public String bracketName;

  /**
   * Line of the data in the database.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public int dbLine;

  /**
   * Playoff round number.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public int playoffRound;

  /**
   * Maximum playoff round.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public int maxPlayoffRound;

  /**
   * Team name.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public @Nullable String teamName;

  /**
   * Team number.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public @Nullable Integer teamNumber;

  /**
   * Non-null string that represents the score.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public String score;

  /**
   * True if the score has been verified.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public boolean verified;

  /**
   * True if the score is a no show.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public boolean noShow;

  /**
   * The table may be null if one has not yet been assigned.
   * This is the performance table.
   */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  public @Nullable String table;

  /**
   * @param bracketName see {@link #bracketName}
   * @param dbLine see {@link #dbLine}
   * @param playoffRound see {@link #playoffRound}
   * @param maxPlayoffRound see {@link #maxPlayoffRound}
   * @param teamNumber see {@link #teamNumber}
   * @param teamName see {@link #teamName}
   * @param score see {@link #score}
   * @param performanceScoreType used to determine how to format the score
   * @param noShow see {@link #noShow}
   * @param verified see {@link #verified}
   * @param table see {@link #table}
   */
  public BracketUpdate(final String bracketName,
                       final int dbLine,
                       final int playoffRound,
                       final int maxPlayoffRound,
                       final @Nullable Integer teamNumber,
                       final @Nullable String teamName,
                       final @Nullable Double score,
                       final ScoreType performanceScoreType,
                       final boolean noShow,
                       final boolean verified,
                       final @Nullable String table) {
    this.bracketName = bracketName;
    this.dbLine = dbLine;
    this.playoffRound = playoffRound;
    this.maxPlayoffRound = maxPlayoffRound;
    this.teamNumber = null != teamNumber
        && Team.NULL_TEAM_NUMBER == teamNumber ? null : teamNumber;
    this.teamName = teamName;
    this.score = null == score ? "" : fll.Utilities.getFormatForScoreType(performanceScoreType).format(score);
    this.verified = verified;
    this.table = table;
    this.noShow = noShow;
  }
}