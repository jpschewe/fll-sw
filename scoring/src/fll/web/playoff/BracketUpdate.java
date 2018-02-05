/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.Serializable;

import fll.Team;
import fll.xml.ScoreType;

/**
 * An update to the playoff brackets display.
 */
public final class BracketUpdate implements Serializable {
  public String bracketName;

  public int dbLine;

  public int playoffRound;

  public int maxPlayoffRound;

  public String teamName;

  public Integer teamNumber;

  /**
   * Non-null string that represents the score.
   */
  public String score;

  /**
   * True if the score has been verified.
   */
  public boolean verified;

  public boolean noShow;

  /**
   * The table may be null if one has not yet been assigned.
   */
  public String table;

  public BracketUpdate(final String bracketName,
                       final int dbLine,
                       final int playoffRound,
                       final int maxPlayoffRound,
                       final Integer teamNumber,
                       final String teamName,
                       final Double score,
                       final ScoreType performanceScoreType,
                       final boolean noShow,
                       final boolean verified,
                       final String table) {
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