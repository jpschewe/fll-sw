/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.CategoryColumnMapping;
import fll.scheduler.ConstraintViolation;
import fll.scheduler.SchedParams;
import fll.scheduler.SubjectiveStation;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.TournamentSchedule.ColumnInformation;

/**
 * Data used in the workflow of uploading a schedule.
 */
public class UploadScheduleData implements Serializable {

  /**
   * @param scheduleFile {@link #getScheduleFile()}
   * @param headerRowIndex {@link #getHeaderRowIndex()}
   * @param headerNames {@link #getHeaderNames()}
   */
  public UploadScheduleData(final File scheduleFile,
                            final int headerRowIndex,
                            final Collection<String> headerNames) {
    this.scheduleFile = scheduleFile;
    this.headerRowIndex = headerRowIndex;
    this.headerNames = new LinkedList<>(headerNames);
  }

  /**
   * Name that instances are referenced as in the session.
   */
  public static final String KEY = "uploadScheduleData";

  private final File scheduleFile;

  /**
   * @return the file that was uploaded
   */
  public File getScheduleFile() {
    return scheduleFile;
  }

  /**
   * Used for {@link #setSelectedSheet(String)} when working with a CSV/TSV file.
   */
  public static final String CSV_SHEET_NAME = "csv";

  private @MonotonicNonNull String selectedSheet = null;

  /**
   * If {@link #getScheduleFile()} is a spreadsheet, then the selected sheet name.
   *
   * @return null until set
   */
  public @Nullable String getSelectedSheet() {
    return selectedSheet;
  }

  /**
   * @param v see {@link #getSelectedSheet()}
   */
  @EnsuresNonNull("selectedSheet")
  public void setSelectedSheet(final String v) {
    selectedSheet = v;
  }

  private final LinkedList<ConstraintViolation> violations = new LinkedList<>();

  /**
   * @return the violations found in the uploaded schedule, initially empty,
   *         unmodifiable collection
   */
  public Collection<ConstraintViolation> getViolations() {
    return Collections.unmodifiableCollection(violations);
  }

  /**
   * @param v see {@link #getViolations()}
   */
  public void setViolations(final Collection<ConstraintViolation> v) {
    violations.clear();
    violations.addAll(v);
  }

  private @MonotonicNonNull TournamentSchedule schedule = null;

  /**
   * @return the schedule being uploaded, is null until set with
   *         {@link #setSchedule(TournamentSchedule)}
   */
  public @Nullable TournamentSchedule getSchedule() {
    return schedule;
  }

  /**
   * @param v see {@link #getSchedule()}
   */
  @EnsuresNonNull("schedule")
  public void setSchedule(final TournamentSchedule v) {
    schedule = v;
  }

  private ColumnInformation columnInfo = ColumnInformation.NULL;

  /**
   * @return the information needed to read the schedule file into a schedule,
   *         initially set to {@link ColumnInformation#NULL}.
   */
  public ColumnInformation getColumnInformation() {
    return columnInfo;
  }

  /**
   * @param v see {@link #getColumnInformation()}
   */
  public void setColumnInformation(final ColumnInformation v) {
    columnInfo = v;
  }

  /**
   * @return the mappings of categories to schedule columns, initially empty,
   *         unmodifiable collection
   */
  public Collection<CategoryColumnMapping> getCategoryColumnMappings() {
    if (null != columnInfo) {
      return columnInfo.getSubjectiveColumnMappings();
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * @return the subjective stations for the schedule.
   * @see SchedParams#getSubjectiveStations()
   */
  public List<SubjectiveStation> getSubjectiveStations() {
    return schedParams.getSubjectiveStations();
  }

  /**
   * @param v see {@link #getSubjectiveStations()}
   */
  public void setSubjectiveStations(final List<SubjectiveStation> v) {
    schedParams.setSubjectiveStations(v);
    subjectiveStationsSet = true;
  }

  /**
   * @return if {@link #setSubjectiveStations(List)} has been called
   */
  public boolean isSubjectiveStationsSet() {
    return subjectiveStationsSet;
  }

  private boolean subjectiveStationsSet = false;

  private SchedParams schedParams = new SchedParams();

  /**
   * This object is used when checking the uploaded schedule for constraint
   * violations.
   * It defaults to the result of {@link SchedParams#SchedParams()}.
   *
   * @return the sched params, not that it is mutable and NOT a copy of the
   *         internal data
   */
  public SchedParams getSchedParams() {
    return schedParams;
  }

  /**
   * @param v the new object to use, see {@link #getSchedParams()}
   */
  public void setSchedParams(final SchedParams v) {
    schedParams = v;
  }

  private final LinkedList<TeamScheduleInfo> missingTeams = new LinkedList<>();

  /**
   * @return teams in the schedule and not in the database
   */
  public Collection<TeamScheduleInfo> getMissingTeams() {
    return missingTeams;
  }

  /**
   * @param v see {@link #getMissingTeams()}
   */
  public void setMissingTeams(final Collection<TeamScheduleInfo> v) {
    missingTeams.clear();
    missingTeams.addAll(v);
  }

  private final LinkedList<GatherTeamInformationChanges.TeamNameDifference> nameDifferences = new LinkedList<>();

  /**
   * @return name differences between the schedule and the database
   */
  public Collection<GatherTeamInformationChanges.TeamNameDifference> getNameDifferences() {
    return nameDifferences;
  }

  /**
   * @param v see {@link #getNameDifferences()}
   */
  public void setNameDifferences(final Collection<GatherTeamInformationChanges.TeamNameDifference> v) {
    nameDifferences.clear();
    nameDifferences.addAll(v);
  }

  private final LinkedList<GatherTeamInformationChanges.TeamOrganizationDifference> organizationDifferences = new LinkedList<>();

  /**
   * @return organization differences between the schedule and the database
   */
  public Collection<GatherTeamInformationChanges.TeamOrganizationDifference> getOrganizationDifferences() {
    return organizationDifferences;
  }

  /**
   * @param v see {@link #getOrganizationDifferences()}
   */
  public void setOrganizationDifferences(final Collection<GatherTeamInformationChanges.TeamOrganizationDifference> v) {
    organizationDifferences.clear();
    organizationDifferences.addAll(v);
  }

  private final int headerRowIndex;

  /**
   * @return index of the header row in the spreadsheet
   */
  public int getHeaderRowIndex() {
    return headerRowIndex;
  }

  private final LinkedList<String> headerNames;

  /**
   * @return names of columns in the header row of the spreadsheet, null and empty
   *         names have been filtered out
   */
  public Collection<String> getHeaderNames() {
    return headerNames;
  }
}
