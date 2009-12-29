package fll.scheduler;

import java.util.Date;

/**
 * Holds data about the schedule for a team.
 * 
 * @author jpschewe
 * @version $Revision$
 */
final class TeamScheduleInfo {
  private int teamNumber;

  private String teamName;

  private String organization;

  private String division;

  private Date presentation;

  private Date technical;

  private String judge;

  private Date[] perf = new Date[ParseSchedule.NUMBER_OF_ROUNDS];
  void setPerf(final int idx, final Date d) {
    perf[idx] = d;
  }
  Date getPerf(final int idx) {
    return perf[idx];
  }

  private String[] perfTableColor = new String[ParseSchedule.NUMBER_OF_ROUNDS];
  void setPerfTableColor(final int idx, final String v) {
    perfTableColor[idx] = v;
  }
  String getPerfTableColor(final int idx) {
    return perfTableColor[idx];
  }

  private int[] perfTableSide = new int[ParseSchedule.NUMBER_OF_ROUNDS];
  void setPerfTableSide(final int idx, final int v) {
    perfTableSide[idx] = v;
  }
  int getPerfTableSide(final int idx) {
    return perfTableSide[idx];
  }
  
  private final int lineNumber;
  public int getLineNumber() { return lineNumber; }
  
  public TeamScheduleInfo(final int lineNumber) {
    this.lineNumber = lineNumber;
  }
  
  /**
   * Find the performance round for the matching time.
   * 
   * @param time
   * @return the round, -1 if cannot be found
   */
  public int findRoundFortime(final Date time) {
    for (int round = 0; round < ParseSchedule.NUMBER_OF_ROUNDS; ++round) {
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
   * @param teamNumber the teamNumber to set
   */
  void setTeamNumber(int teamNumber) {
    this.teamNumber = teamNumber;
  }

  /**
   * @return the teamNumber
   */
  int getTeamNumber() {
    return teamNumber;
  }

  /**
   * @param teamName the teamName to set
   */
  void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  /**
   * @return the teamName
   */
  String getTeamName() {
    return teamName;
  }

  /**
   * @param organization the organization to set
   */
  void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * @return the organization
   */
  String getOrganization() {
    return organization;
  }

  /**
   * @param division the division to set
   */
  void setDivision(String division) {
    this.division = division;
  }

  /**
   * @return the division
   */
  String getDivision() {
    return division;
  }

  /**
   * @param presentation the presentation to set
   */
  void setPresentation(Date presentation) {
    this.presentation = presentation;
  }

  /**
   * @return the presentation
   */
  Date getPresentation() {
    return presentation;
  }

  /**
   * @param technical the technical to set
   */
  void setTechnical(Date technical) {
    this.technical = technical;
  }

  /**
   * @return the technical
   */
  Date getTechnical() {
    return technical;
  }

  /**
   * @param judge the judge to set
   */
  void setJudge(String judge) {
    this.judge = judge;
  }

  /**
   * @return the judge
   */
  String getJudge() {
    return judge;
  }
}