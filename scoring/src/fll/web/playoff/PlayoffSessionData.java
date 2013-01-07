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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fll.Team;
import fll.Tournament;
import fll.db.Queries;

/**
 * Session data for working with the playoffs.
 */
public final class PlayoffSessionData implements Serializable {

  public PlayoffSessionData(final Connection connection) throws SQLException {
    final int currentTournamentID = Queries.getCurrentTournament(connection);
    mCurrentTournament = Tournament.findTournamentByID(connection, currentTournamentID);

    mExistingDivisions = Playoff.getPlayoffDivisions(connection, mCurrentTournament.getTournamentID());

    mDivisions = new LinkedList<String>(mExistingDivisions);
    mDivisions.add(PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION);
    mEventDivisions = Queries.getEventDivisions(connection, mCurrentTournament.getTournamentID());
    mNumPlayoffRounds = Queries.getNumPlayoffRounds(connection);

    mTournamentTeams = Queries.getTournamentTeams(connection, mCurrentTournament.getTournamentID());

  }

  private final Map<Integer, Team> mTournamentTeams;

  public Map<Integer, Team> getTournamentTeams() {
    return mTournamentTeams;
  }

  public Collection<Team> getTournamentTeamsValues() {
    return mTournamentTeams.values();
  }

  private final Tournament mCurrentTournament;

  public Tournament getCurrentTournament() {
    return mCurrentTournament;
  }

  private final List<String> mExistingDivisions;

  /**
   * All existing playoff divisions.
   */
  public List<String> getExistingDivisions() {
    return mExistingDivisions;
  }

  private final List<String> mEventDivisions;

  /**
   * All event divisions.
   */
  public List<String> getEventDivisions() {
    return mEventDivisions;
  }

  private final List<String> mDivisions;

  /**
   * Divisions that have been created, plus the create division string
   * {@link PlayoffIndex#CREATE_NEW_PLAYOFF_DIVISION}.
   */
  public List<String> getDivisions() {
    return mDivisions;
  }

  private final int mNumPlayoffRounds;

  public int getNumPlayoffRounds() {
    return mNumPlayoffRounds;
  }

  private String mDivision;

  public void setDivision(final String v) {
    mDivision = v;
  }

  public String getDivision() {
    return mDivision;
  }

  private List<Team> mTeamsNeedingSeedingRuns;

  public List<Team> getTeamsNeedingSeedingRuns() {
    return mTeamsNeedingSeedingRuns;
  }

  public void setTeamsNeedingSeedingRounds(final List<Team> v) {
    mTeamsNeedingSeedingRuns = v;
  }

}
