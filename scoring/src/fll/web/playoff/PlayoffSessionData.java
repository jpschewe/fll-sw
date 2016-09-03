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

import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.xml.BracketSortType;

/**
 * Session data for working with the playoffs.
 */
public final class PlayoffSessionData implements Serializable {

  public PlayoffSessionData(final Connection connection) throws SQLException {
    final int currentTournamentID = Queries.getCurrentTournament(connection);
    mCurrentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    mNumPlayoffRounds = Queries.getNumPlayoffRounds(connection);
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

    mSort = null;
  }

  private boolean mEnableThird = false;

  public boolean getEnableThird() {
    return mEnableThird;
  }

  public void setEnableThird(boolean v) {
    mEnableThird = v;
  }

  private BracketSortType mSort;

  public BracketSortType getSort() {
    return mSort;
  }

  public void setSort(final BracketSortType sort) {
    mSort = sort;
  }

  private final Map<Integer, TournamentTeam> mTournamentTeams;

  public Map<Integer, TournamentTeam> getTournamentTeams() {
    return mTournamentTeams;
  }

  public Collection<TournamentTeam> getTournamentTeamsValues() {
    return mTournamentTeams.values();
  }

  private final Tournament mCurrentTournament;

  public Tournament getCurrentTournament() {
    return mCurrentTournament;
  }

  private final List<String> mExistingBrackets;

  /**
   * All existing playoff brackets.
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

  private final int mNumPlayoffRounds;

  public int getNumPlayoffRounds() {
    return mNumPlayoffRounds;
  }

  private String mBracket;

  public void setBracket(final String v) {
    mBracket = v;
  }

  public String getBracket() {
    return mBracket;
  }

  private List<Team> mTeamsNeedingSeedingRuns;

  public List<Team> getTeamsNeedingSeedingRuns() {
    return mTeamsNeedingSeedingRuns;
  }

  public void setTeamsNeedingSeedingRounds(final List<Team> v) {
    mTeamsNeedingSeedingRuns = v;
  }

}
