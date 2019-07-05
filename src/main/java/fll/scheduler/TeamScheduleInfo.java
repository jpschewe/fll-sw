package fll.scheduler;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Streams;

/**
 * Holds data about the schedule for a team.
 */
public final class TeamScheduleInfo implements Serializable {

  private final int teamNumber;

  private String teamName;

  private String organization;

  private String awardGroup;

  private String judgingGroup;

  private final SortedSet<PerformanceTime> performances = new TreeSet<>();

  /**
   * @return unmodifiable version of all performances
   */
  public SortedSet<PerformanceTime> getAllPerformances() {
    return Collections.unmodifiableSortedSet(performances);
  }

  /**
   * @return stream of all performance rounds
   */
  public Stream<PerformanceTime> allPerformances() {
    return performances.stream();
  }

  /**
   * @return stream of all performance rounds and their index
   */
  public Stream<Pair<PerformanceTime, Long>> enumerateAllPerformances() {
    return Streams.mapWithIndex(allPerformances(), (performance,
                                                    index) -> Pair.of(performance, index));
  }

  /**
   * @return stream of all practice performance rounds and their index within
   *         practice rounds
   */
  public Stream<Pair<PerformanceTime, Long>> enumeratePracticePerformances() {
    return Streams.mapWithIndex(allPerformances().filter(PerformanceTime::isPractice), (performance,
                                                                                        index) -> Pair.of(performance,
                                                                                                          index));
  }

  /**
   * @return stream of all regular match play performance rounds and their index
   *         within
   *         practice rounds
   */
  public Stream<Pair<PerformanceTime, Long>> enumerateRegularMatchPlayPerformances() {
    // TODO use Predicate::not when moving to Java 11+
    return Streams.mapWithIndex(allPerformances().filter(pt -> !pt.isPractice()), (performance,
                                                                                   index) -> Pair.of(performance,
                                                                                                     index));
  }

  /**
   * Add a performance. This should only be called from
   * {@link TournamentSchedule} otherwise things can get out of sync.
   *
   * @param performance
   */
  /* package */void addPerformance(final PerformanceTime performance) {
    performances.add(performance);
  }

  /**
   * Removes the first performance found at the specified time.
   *
   * @param time the time of the performance
   * @return if a performance was removed
   */
  /* package */ boolean removePerformance(final PerformanceTime performance) {
    return performances.remove(performance);
  }

  /**
   * Find the first performance at the specified time.
   *
   * @param time the time to find a performance at
   * @return the performance or null if not found
   */
  public PerformanceTime getPerformanceAtTime(final LocalTime time) {
    return allPerformances().filter(pt -> pt.getTime().equals(time)).findFirst().orElse(null);
  }

  /**
   * @param idx zero based
   * @return the performance with the specified index or null if no performance
   *         with the specified index exists.
   */
  public PerformanceTime getPerf(final int idx) {
    return enumerateAllPerformances().filter(p -> p.getRight() == idx).map(Pair::getLeft).findFirst().orElse(null);
  }

  public TeamScheduleInfo(final int teamNumber) {
    this.teamNumber = teamNumber;
  }

  @Override
  public String toString() {
    return "[ScheduleInfo for "
        + getTeamNumber()
        + "]";
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
   * @param division award group to set
   */
  public void setDivision(final String division) {
    this.awardGroup = division;
  }

  /**
   * @return the award group, never null
   */
  public String getAwardGroup() {
    if (null == awardGroup) {
      return "";
    } else {
      return awardGroup;
    }
  }

  /**
   * @param judge the judging group
   */
  public void setJudgingGroup(final String judge) {
    this.judgingGroup = judge;
  }

  /**
   * @return the judging group, never null
   */
  public String getJudgingGroup() {
    if (null == judgingGroup) {
      return "";
    } else {
      return judgingGroup;
    }
  }

  private final HashMap<String, SubjectiveTime> subjectiveTimes = new HashMap<>();

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
    final Stream<Pair<PerformanceTime, Long>> stream;
    if (performance.isPractice()) {
      stream = enumeratePracticePerformances();
    } else {
      stream = enumerateRegularMatchPlayPerformances();
    }
    return stream.filter(p -> p.getLeft().equals(performance)).map(Pair::getRight).findFirst().orElse(Long.valueOf(-1))
                 .intValue();
  }

  /**
   * Compute a display name for the specified performance round.
   *
   * @param performance the performance information
   * @return the name to display, null on error
   */
  public String getRoundName(final PerformanceTime performance) {
    final int roundIndex = computeRound(performance);
    if (roundIndex < 0) {
      return null;
    } else {
      if (performance.isPractice()) {
        return String.format("Practice %d", roundIndex
            + 1);
      } else {
        return String.valueOf(roundIndex
            + 1);
      }
    }
  }
}