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
import fll.TournamentTeam;
import fll.db.Queries;

/**
 * Session data for working with the playoffs.
 */
public final class PlayoffSessionData implements Serializable {

  public PlayoffSessionData(final Connection connection) throws SQLException {
    final int currentTournamentID = Queries.getCurrentTournament(connection);
    mCurrentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    mEventDivisions = Queries.getEventDivisions(connection, mCurrentTournament.getTournamentID());
    mNumPlayoffRounds = Queries.getNumPlayoffRounds(connection);
    mTournamentTeams = Queries.getTournamentTeams(connection, mCurrentTournament.getTournamentID());

    mExistingDivisions = Playoff.getPlayoffDivisions(connection, mCurrentTournament.getTournamentID());

    mInitDivisions = new LinkedList<String>(mEventDivisions);
    mInitDivisions.add(PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION);
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

  private final List<String> mInitDivisions;

  /**
   * Divisions that can be initialized, this is event divisions plus
   * {@link PlayoffIndex#CREATE_NEW_PLAYOFF_DIVISION}.
   */
  public List<String> getInitDivisions() {
    return mInitDivisions;
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

  /**
   * Teams that are to be used to initialize a custom division.
   */
  public List<Team> getDivisionTeams() {
    return mDivisionTeams;
  }

  public void setDivisionTeams(final List<Team> v) {
    mDivisionTeams = v;
  }

  private List<Team> mDivisionTeams;

}
