/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.assign;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores schedule information about a team at a tournament.
 */
class TournamentInfo {
  public TournamentInfo(final String name, final String division, final int min, final int max) {
    this.name = name;
    this.division = division;
    this.min = min;
    this.max = max;
  }

  private final String name;

  public String getName() {
    return name;
  }

  private final String division;

  public String getDivision() {
    return division;
  }

  private final int max;

  public int getMax() {
    return max;
  }

  private final int min;

  public int getMin() {
    return min;
  }

  private final Set<TeamInfo> teams = new HashSet<TeamInfo>();

  public void addTeam(final TeamInfo team) {
    teams.add(team);
  }

  public Set<TeamInfo> getTeams() {
    return Collections.unmodifiableSet(teams);
  }

  public int getNumTeams() {
    return teams.size();
  }

  public boolean isFull() {
    return getNumTeams() >= getMax();
  }

  public boolean needsTeams() {
    return getNumTeams() < getMin();
  }
}