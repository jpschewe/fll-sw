package fll.scheduler;

import java.util.Date;

/**
 * Holds data about the schedule for a team.
 * 
 * @author jpschewe
 * @version $Revision$
 */
final class TeamScheduleInfo {
  int teamNumber;

  String teamName;

  String organization;

  String division;

  Date presentation;

  Date technical;

  String judge;

  Date[] perf = new Date[ParseSchedule.NUMBER_OF_ROUNDS];

  String[] perfTableColor = new String[ParseSchedule.NUMBER_OF_ROUNDS];

  int[] perfTableSide = new int[ParseSchedule.NUMBER_OF_ROUNDS];
  
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
        + teamNumber + "]";
  }

  @Override
  public int hashCode() {
    return teamNumber;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof TeamScheduleInfo) {
      return ((TeamScheduleInfo) o).teamNumber == this.teamNumber;
    } else {
      return false;
    }
  }
}