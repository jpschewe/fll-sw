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

    final int playoffMaxPerformanceRound = Playoff.getMaxPerformanceRound(connection,
                                                                          mCurrentTournament.getTournamentID());
    final int maxPerformanceRound = Queries.getMaxRunNumber(connection, mCurrentTournament.getTournamentID());

    mInitializedBrackets = new LinkedList<>();
    mUninitializedBrackets = new LinkedList<>();
    safeToUninitialize = new LinkedList<>();
    safeToDelete = new LinkedList<>();
    for (final String bracketName : mExistingBrackets) {
      final int bracketMaxRounds = Playoff.getMaxPerformanceRound(connection, mCurrentTournament.getTournamentID(),
                                                                  bracketName);

      final boolean bracketInitialized = Queries.isPlayoffDataInitialized(connection,
                                                                          mCurrentTournament.getTournamentID(),
                                                                          bracketName);
      if (!bracketInitialized
          || (bracketMaxRounds == playoffMaxPerformanceRound
              && maxPerformanceRound <= bracketMaxRounds)) {
        // It's safe to delete the last bracket initialized or a bracket that hasn't
        // been initialized. Otherwise we
        // leave gaps in the performance round numbers. We also check against the max
        // performance round in case there are performance rounds after the playoffs.
        // This shouldn't happen, but we should check anyway.
        safeToDelete.add(bracketName);
      }

      if (bracketInitialized) {
        mInitializedBrackets.add(bracketName);

        if (bracketMaxRounds == playoffMaxPerformanceRound
            && maxPerformanceRound <= bracketMaxRounds) {
          // It's only safe to uninitialize the last bracket initialized. Otherwise we
          // leave gaps in the performance round numbers. We also check against the max
          // performance round in case there are performance rounds after the playoffs.
          // This shouldn't happen, but we should check anyway.
          safeToUninitialize.add(bracketName);
        }
      } else {
        mUninitializedBrackets.add(bracketName);
      }
    }

    mUnfinishedBrackets = Playoff.getUnfinishedPlayoffBrackets(connection, mCurrentTournament.getTournamentID());

    mSort = BracketSortType.SEEDING;
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

  private final List<Integer> customSortOrder = new LinkedList<>();

  /**
   * @return order used with {@link BracketSortType#CUSTOM}
   */
  public List<Integer> getCustomSortOrder() {
    return Collections.unmodifiableList(customSortOrder);
  }

  /**
   * @param v see {@link #getCustomSortOrder()}
   */
  public void setCustomSortOrder(final List<Integer> v) {
    customSortOrder.clear();
    customSortOrder.addAll(v);
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

  private final Collection<String> safeToUninitialize;

  /**
   * @return brackets that can be uninitialized
   */
  public Collection<String> getSafeToUninitialize() {
    return Collections.unmodifiableCollection(safeToUninitialize);
  }

  private final Collection<String> safeToDelete;

  /**
   * @return brackets that can be deleted.
   */
  public Collection<String> getSafeToDelete() {
    return Collections.unmodifiableCollection(safeToDelete);
  }

}
