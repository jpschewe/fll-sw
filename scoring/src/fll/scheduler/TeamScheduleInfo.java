package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Holds data about the schedule for a team.
 */
public final class TeamScheduleInfo implements Serializable {

  private final int teamNumber;

  private String teamName;

  private String organization;

  private String division;

  private String judgingStation;

  private final PerformanceTime[] perf;

  /**
   * Set a performance time. This should only be called from
   * {@link TournamentSchedule} otherwise things can get out of sync.
   * 
   * @param idx
   * @param performance
   */
  /* package */void setPerf(final int idx,
                            final PerformanceTime performance) {
    perf[idx] = performance;
  }

  /**
   * @param idx zero based
   */
  public PerformanceTime getPerf(final int idx) {
    return perf[idx];
  }

  /**
   * @param idx zero based
   */
  public LocalTime getPerfTime(final int idx) {
    return perf[idx].getTime();
  }

  /**
   * @param idx zero based
   */
  public String getPerfTableColor(final int idx) {
    return perf[idx].getTable();
  }

  /**
   * @param idx zero based
   */
  public int getPerfTableSide(final int idx) {
    return perf[idx].getSide();
  }

  public TeamScheduleInfo(final int numRounds,
                          final int teamNumber) {
    this.numberOfRounds = numRounds;
    this.perf = new PerformanceTime[numRounds];
    this.teamNumber = teamNumber;
  }

  /**
   * Find the performance round for the matching time.
   * 
   * @param time
   * @return the round, -1 if cannot be found
   */
  public int findRoundFortime(final LocalTime time) {
    for (int round = 0; round < perf.length; ++round) {
      if (perf[round].getTime().equals(time)) {
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
   * @return the teamName, never null
   */
  public String getTeamName() {
    if (null == teamName) {
      return "";
    } else {
      return teamName;
    }
  }

  /**
   * @param organization the organization to set
   */
  public void setOrganization(final String organization) {
    this.organization = organization;
  }

  /**
   * @return the organization, never null
   */
  public String getOrganization() {
    if (null == organization) {
      return "";
    } else {
      return organization;
    }
  }

  /**
   * @param award group to set
   */
  public void setDivision(final String division) {
    this.division = division;
  }

  /**
   * @return the award group, never null
   */
  public String getDivision() {
    if (null == division) {
      return "";
    } else {
      return division;
    }
  }

  /**
   * @param judge the judging station
   */
  public void setJudgingStation(final String judge) {
    this.judgingStation = judge;
  }

  /**
   * @return the judging station, never null
   */
  public String getJudgingGroup() {
    if (null == judgingStation) {
      return "";
    } else {
      return judgingStation;
    }
  }

  public int getNumberOfRounds() {
    return numberOfRounds;
  }

  private final int numberOfRounds;

  private HashMap<String, SubjectiveTime> subjectiveTimes = new HashMap<String, SubjectiveTime>();

  public Collection<SubjectiveTime> getSubjectiveTimes() {
    return Collections.unmodifiableCollection(subjectiveTimes.values());
  }

  public void addSubjectiveTime(final SubjectiveTime s) {
    subjectiveTimes.put(s.getName(), s);
  }

  public void removeSubjectiveTime(final SubjectiveTime s) {
    subjectiveTimes.remove(s.getName());
  }

  /**
   * Get the subjective time by name.
   * 
   * @param name name of a judging station
   * @return null if no time with that name found
   */
  public SubjectiveTime getSubjectiveTimeByName(final String name) {
    return subjectiveTimes.get(name);
  }

  public Set<String> getKnownSubjectiveStations() {
    return Collections.unmodifiableSet(subjectiveTimes.keySet());
  }

  /**
   * Figure out which round number (0-based) this performance time is for this
   * team.
   * 
   * @param performance the performance to find
   * @return the round number or -1 if the performance cannot be found
   */
  public int computeRound(final PerformanceTime performance) {
    for (int round = 0; round < perf.length; ++round) {
      if (performance.equals(perf[round])) {
        return round;
      }
    }
    return -1;
  }
}