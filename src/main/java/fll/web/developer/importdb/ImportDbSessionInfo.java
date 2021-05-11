/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Team;
import fll.db.TeamPropertyDifference;

/**
 * Information needed for importing a database. This object is stored in the
 * HTTP session.
 */
public final class ImportDbSessionInfo {

  /**
   * @param dbimport {@link #getImportDataSource()}
   */
  public ImportDbSessionInfo(final DataSource dbimport) {
    this(dbimport, "../index.jsp");
  }

  /**
   * @param dbimport {@link #getImportDataSource()}
   * @param redirectUrl {@link #getRedirectURL()}.
   */
  public ImportDbSessionInfo(final DataSource dbimport,
                             final String redirectUrl) {
    this.dbimport = dbimport;
    this.redirectURL = redirectUrl;
  }

  private final DataSource dbimport;

  /**
   * @return The datasource for the database being imported.
   */
  public DataSource getImportDataSource() {
    return dbimport;
  }

  private @MonotonicNonNull String selectedTournament;

  /**
   * @return see {@link #setTournamentName(String)}
   */
  public @Nullable String getTournamentName() {
    return selectedTournament;
  }

  /**
   * @param v the tournament name to be imported
   */
  public void setTournamentName(final String v) {
    selectedTournament = v;
  }

  private final Collection<Team> missingTeams = new LinkedList<>();

  /**
   * @param v the teams that are missing from the destination tournament
   */
  public void setMissingTeams(final Collection<Team> v) {
    missingTeams.clear();
    missingTeams.addAll(v);
  }

  /**
   * @return an unmodifiable collection of the missing teams
   * @see #setMissingTeams(Collection)
   */
  public Collection<Team> getMissingTeams() {
    return Collections.unmodifiableCollection(missingTeams);
  }

  private final List<TeamPropertyDifference> teamDifferences = new LinkedList<>();

  /**
   * @param v the differences between the source and destination database
   */
  public void setTeamDifferences(final List<TeamPropertyDifference> v) {
    teamDifferences.clear();
    teamDifferences.addAll(v);
  }

  /**
   * @return an unmodifiable collection of the differences
   * @see #setTeamDifferences(List)
   */
  public List<TeamPropertyDifference> getTeamDifferences() {
    return Collections.unmodifiableList(teamDifferences);
  }

  private boolean importPerformance = true;

  /**
   * @param v see {@link #isImportPerformance()}
   */
  public void setImportPerformance(final boolean v) {
    importPerformance = v;
  }

  /**
   * @return true if the performance data should be imported
   */
  public boolean isImportPerformance() {
    return importPerformance;
  }

  private boolean importSubjective = true;

  /**
   * @param v set to true to import the subjective data into the destination
   */
  public void setImportSubjective(final boolean v) {
    importSubjective = v;
  }

  /**
   * @return see {@link #setImportSubjective(boolean)}
   */
  public boolean isImportSubjective() {
    return importSubjective;
  }

  private boolean importFinalist = true;

  /**
   * @param v set to true to import the finalist data into the destination
   *          database
   */
  public void setImportFinalist(final boolean v) {
    importFinalist = v;
  }

  /**
   * @return see {@link #setImportFinalist(boolean)}
   */
  public boolean isImportFinalist() {
    return importFinalist;
  }

  private String redirectURL = "../index.jsp";

  /**
   * This is a URL that is passed to
   * {@link HttpServletResponse#sendRedirect(String)} when {@link ExecuteImport}
   * finishes successfully. The default is the developer index.
   *
   * @return the URL to redirect to on success
   */
  public String getRedirectURL() {
    return redirectURL;
  }

}
