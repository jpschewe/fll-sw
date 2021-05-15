/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.xml.BracketSortType;

/**
 * Session data for working with the playoffs.
 */
public final class PlayoffSessionData implements Serializable {

  /**
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public PlayoffSessionData(final Connection connection) throws SQLException {
    final int currentTournamentID = Queries.getCurrentTournament(connection);
    mCurrentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    mNumPlayoffRounds = Queries.getNumPlayoffRounds(connection, currentTournamentID);
    mTournamentTeams = Queries.getTournamentTeams(connection, mCurrentTournament.getTournamentID());

    mExistingBrackets = Playoff.getPlayoffBrackets(connection, mCurrentTournament.getTournamentID());

    mInitializedBrackets = new LinkedList<>();
    mUninitializedBrackets = new LinkedList<>();
    for (final String bracketName : mExistingBrackets) {
      if (Queries.isPlayoffDataInitialized(connection, mCurrentTournament.getTournamentID(), bracketName)) {
        mInitializedBrackets.add(bracketName);
      } else {
        mUninitializedBrackets.add(bracketName);
      }
    }

    mUnfinishedBrackets = Playoff.getUnfinishedPlayoffBrackets(connection, mCurrentTournament.getTournamentID());

    mSort = null;
  }

  private boolean mEnableThird = false;

  /**
   * @return if the third place bracket is enabled
   */
  public boolean getEnableThird() {
    return mEnableThird;
  }

  /**
   * @param v {@link #getEnableThird()}
   */
  public void setEnableThird(boolean v) {
    mEnableThird = v;
  }

  private BracketSortType mSort;

  /**
   * @return how to sort the initial brackets
   */
  public BracketSortType getSort() {
    return mSort;
  }

  /**
   * @param sort {@link #getSort()}
   */
  public void setSort(final BracketSortType sort) {
    mSort = sort;
  }

  private final Map<Integer, TournamentTeam> mTournamentTeams;

  /**
   * @return key is team number, value is team
   */
  public Map<Integer, TournamentTeam> getTournamentTeams() {
    return mTournamentTeams;
  }

  /**
   * @return the tournament teams
   */
  public Collection<TournamentTeam> getTournamentTeamsValues() {
    return mTournamentTeams.values();
  }

  private final Tournament mCurrentTournament;

  /**
   * @return the current tournament
   */
  public Tournament getCurrentTournament() {
    return mCurrentTournament;
  }

  private final List<String> mExistingBrackets;

  /**
   * @return All existing playoff brackets.
   */
  public List<String> getExistingBrackets() {
    return mExistingBrackets;
  }

  private final List<String> mInitializedBrackets;

  /**
   * @return Brackets that have already been initialized
   */
  public List<String> getInitializedBrackets() {
    return Collections.unmodifiableList(mInitializedBrackets);
  }

  private final List<String> mUninitializedBrackets;

  /**
   * @return Brackets that have not been initialized.
   */
  public List<String> getUninitializedBrackets() {
    return Collections.unmodifiableList(mUninitializedBrackets);
  }

  private final List<String> mUnfinishedBrackets;

  /**
   * @return brackets that have been initialized, but not completed
   */
  public List<String> getUnfinishedBrackets() {
    return Collections.unmodifiableList(mUnfinishedBrackets);
  }

  private final int mNumPlayoffRounds;

  /**
   * @return how many playoff rounds
   */
  public int getNumPlayoffRounds() {
    return mNumPlayoffRounds;
  }

  private @Nullable String mBracket;

  /**
   * @param v {@link #getBracket()}
   */
  public void setBracket(final @Nullable String v) {
    mBracket = v;
  }

  /**
   * @return the bracket name
   */
  public @Nullable String getBracket() {
    return mBracket;
  }

  private List<Team> mTeamsNeedingSeedingRuns;

  /**
   * @return teams that needing to complete seeding rounds
   */
  public List<Team> getTeamsNeedingSeedingRuns() {
    return mTeamsNeedingSeedingRuns;
  }

  /**
   * @param v {@link #getTeamsNeedingSeedingRuns()}
   */
  public void setTeamsNeedingSeedingRounds(final List<Team> v) {
    mTeamsNeedingSeedingRuns = v;
  }

}
