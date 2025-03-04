/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import fll.TournamentTeam;
import fll.web.playoff.TeamScore;

/**
 * Message sent to the client for a new score.
 */
/* package */ final class ScoreUpdateMessage extends Message {

  /**
   * @param team {@link #team}
   * @param score {@link #score}
   * @param formattedScore {@link #formattedScore}
   * @param teamScore used to gather {@link #isBye()} {@link #isNoShow()}
   *          {@link #getRunNumber()}
   * @param runDisplayName {@link #getRunDisplayName()}
   */
  ScoreUpdateMessage(final TournamentTeam team,
                     final double score,
                     final String formattedScore,
                     final TeamScore teamScore,
                     final String runDisplayName) {
    super(Message.MessageType.UPDATE);
    this.team = team;
    this.score = score;
    this.formattedScore = formattedScore;
    this.runNumber = teamScore.getRunNumber();
    this.bye = teamScore.isBye();
    this.noShow = teamScore.isNoShow();
    this.runDisplayName = runDisplayName;
  }

  /**
   * @return The team that the score is for.
   */
  public TournamentTeam getTeam() {
    return team;
  }

  private final TournamentTeam team;

  /**
   * @return The score.
   */
  public double getScore() {
    return score;
  }

  private final double score;

  /**
   * @return The score string to display.
   */
  public String getFormattedScore() {
    return formattedScore;
  }

  private final String formattedScore;

  /**
   * @return the run number for the score
   */
  public int getRunNumber() {
    return runNumber;
  }

  private final int runNumber;

  private final String runDisplayName;

  /**
   * @return display name for the run
   */
  public String getRunDisplayName() {
    return runDisplayName;
  }

  /**
   * @return if this is a bye
   */
  public boolean isBye() {
    return bye;
  }

  private final boolean bye;

  /**
   * @return if this is a no show
   */
  public boolean isNoShow() {
    return noShow;
  }

  private final boolean noShow;

}