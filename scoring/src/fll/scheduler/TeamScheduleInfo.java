package fll.scheduler;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

  private final Date[] perf;

  public void setPerf(final int idx,
                      final Date d) {
    perf[idx] = d;
  }

  /**
   * @param idx zero based
   */
  public Date getPerf(final int idx) {
    return perf[idx];
  }

  private final String[] perfTableColor;

  /**
   * @param idx zero based
   */
  public void setPerfTableColor(final int idx,
                                final String v) {
    perfTableColor[idx] = v;
  }

  /**
   * @param idx zero based
   */
  public String getPerfTableColor(final int idx) {
    return perfTableColor[idx];
  }

  private final int[] perfTableSide;

  /**
   * @param idx zero based
   */
  public void setPerfTableSide(final int idx,
                               final int v) {
    perfTableSide[idx] = v;
  }

  /**
   * @param idx zero based
   */
  public int getPerfTableSide(final int idx) {
    return perfTableSide[idx];
  }

  public TeamScheduleInfo(final int numRounds,
                          final int teamNumber) {
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

  public int getNumberOfRounds() {
    return numberOfRounds;
  }

  private final int numberOfRounds;

  private Map<String, SubjectiveTime> subjectiveTimes = new HashMap<String, SubjectiveTime>();

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
   * @return null if no time with that name found
   */
  public SubjectiveTime getSubjectiveTimeByName(final String name) {
    return subjectiveTimes.get(name);
  }

  public Set<String> getKnownSubjectiveStations() {
    return Collections.unmodifiableSet(subjectiveTimes.keySet());
  }

  /**
   * Represents a subjective judging time.
   */
  public static final class SubjectiveTime {
    public SubjectiveTime(final String name,
                          final Date time) {
      this.name = name;
      this.time = time == null ? null : new Date(time.getTime());
    }

    private final String name;

    /**
     * Name of what is being judged.
     */
    public String getName() {
      return name;
    }

    private final Date time;

    /**
     * Time of the judging session.
     */
    public Date getTime() {
      return null == time ? null : new Date(time.getTime());
    }
  }

  /**
   * Represents performance judging time.
   */
  public static final class PerformanceTime implements Comparable<PerformanceTime> {
    public PerformanceTime(final Date time,
                           final String table,
                           final int side) {
      this.table = table;
      this.side = side;
      this.time = time == null ? null : new Date(time.getTime());
    }

    private final String table;

    public String getTable() {
      return table;
    }

    private final int side;

    public int getSide() {
      return side;
    }

    private final Date time;

    public Date getTime() {
      return null == time ? null : new Date(time.getTime());
    }

    @Override
    public int hashCode() {
      return null == time ? 13 : time.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      } else if (o.getClass() == PerformanceTime.class) {
        return 0 == compareTo((PerformanceTime) o);
      } else {
        return false;
      }
    }

    public int compareTo(final PerformanceTime other) {
      if (null != this.time) {
        final int timeCompare = this.time.compareTo(other.time);
        if (0 == timeCompare) {
          return this.table.compareTo(other.table);
        } else {
          return timeCompare;
        }
      } else if (null == this.time
          && null == other.time) {
        return this.table.compareTo(other.table);
      } else {
        return -1
            * other.compareTo(this);
      }
    }

  }
}