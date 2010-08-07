package fll.scheduler;

import java.util.Date;

/**
 * Holds data about the schedule for a team.
 */
public final class TeamScheduleInfo {

  private final int teamNumber;

  private String teamName;

  private String organization;

  private String division;

  private Date presentation;

  private Date technical;

  private String judgingStation;

  private final Date[] perf;

  public void setPerf(final int idx,
               final Date d) {
    perf[idx] = d;
  }

  public Date getPerf(final int idx) {
    return perf[idx];
  }

  private final String[] perfTableColor;

  public void setPerfTableColor(final int idx,
                         final String v) {
    perfTableColor[idx] = v;
  }

  public String getPerfTableColor(final int idx) {
    return perfTableColor[idx];
  }

  private final int[] perfTableSide;

  public void setPerfTableSide(final int idx,
                        final int v) {
    perfTableSide[idx] = v;
  }

  public int getPerfTableSide(final int idx) {
    return perfTableSide[idx];
  }

  private final int lineNumber;

  /**
   * @return Line number that this object corresponds to in the original schedule.
   */
  public int getLineNumber() {
    return lineNumber;
  }

  public TeamScheduleInfo(final int lineNumber,
                          final int numRounds,
                          final int teamNumber) {
    this.lineNumber = lineNumber;
    this.numberOfRounds = numRounds;
    this.perf = new Date[numRounds];
    this.perfTableColor = new String[numRounds];
    this.perfTableSide = new int[numRounds];
    this.teamNumber = teamNumber;
  }

  /**
   * Find the performance round for the matching time.
   * 
   * @param time
   * @return the round, -1 if cannot be found
   */
  public int findRoundFortime(final Date time) {
    for (int round = 0; round < perf.length; ++round) {
      if (perf[round].equals(time)) {
        return round;
      }
    }
    return -1;
  }

  @Override
  public String toString() {
    return "[ScheduleInfo for "
        + getTeamNumber() + "]";
  }

  @Override
  public int hashCode() {
    return getTeamNumber();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof TeamScheduleInfo) {
      return ((TeamScheduleInfo) o).getTeamNumber() == this.getTeamNumber();
    } else {
      return false;
    }
  }

  /**
   * @return the teamNumber
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  /**
   * @param teamName the teamName to set
   */
  public void setTeamName(final String teamName) {
    this.teamName = teamName;
  }

  /**
   * @return the teamName
   */
  public String getTeamName() {
    return teamName;
  }

  /**
   * @param organization the organization to set
   */
  public void setOrganization(final String organization) {
    this.organization = organization;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param division the division to set
   */
  public void setDivision(final String division) {
    this.division = division;
  }

  /**
   * @return the division
   */
  public String getDivision() {
    return division;
  }

  /**
   * @param presentation the presentation to set
   */
  public void setPresentation(final Date presentation) {
    this.presentation = presentation;
  }

  /**
   * @return the presentation
   */
  public Date getPresentation() {
    return presentation;
  }

  /**
   * @param technical the technical to set
   */
  public void setTechnical(final Date technical) {
    this.technical = technical;
  }

  /**
   * @return the technical
   */
  public Date getTechnical() {
    return technical;
  }

  /**
   * @param judge the judging station
   */
  public void setJudgingStation(final String judge) {
    this.judgingStation = judge;
  }

  /**
   * @return the judging station
   */
  public String getJudgingStation() {
    return judgingStation;
  }

  /**
   * @return
   */
  public int getNumberOfRounds() {
    return numberOfRounds;
  }

  private final int numberOfRounds;
}