/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Scoring utilities.
 */
public final class ScoreUtils {

  private ScoreUtils() {
    // no instances
  }

  /**
   * Schedule the specified finalists into time slots such that no 1 team is
   * scheduled to be judged in two categories at the same time. Also try and
   * minimize the amount of time to perform the finalist judging.
   * 
   * @param finalists Map of category names to a collection of team numbers that
   *          are finalists for that category
   * @return Each element in the list represents a time slot. A time slot is a
   *         map from categories to teams that are scheduled to be judged during
   *         the time slot in the specified category
   */
  public static List<Map<String, Integer>> scheduleFinalists(final Map<String, Collection<Integer>> finalists) {
    // reorient the map to be based on teams so that we can figure out which
    // team has the most categories and schedule that team first
    final Map<Integer, List<String>> finalistsCount = new HashMap<Integer, List<String>>();
    for(final Map.Entry<String, Collection<Integer>> entry : finalists.entrySet()) {
      final String category = entry.getKey();
      final Collection<Integer> teams = entry.getValue();
      for (final Integer team : teams) {
        if (!finalistsCount.containsKey(team)) {
          finalistsCount.put(team, new LinkedList<String>());
        }
        finalistsCount.get(team).add(category);
      }
    }
    // sort the list so that the team in the most categories is first, this
    // should ensure the minimum amount of time to do the finalist judging
    final List<Integer> sortedTeams = new LinkedList<Integer>(finalistsCount.keySet());
    Collections.sort(sortedTeams, new Comparator<Integer>() {
      public int compare(final Integer teamOne, final Integer teamTwo) {
        final int numCatsOne = finalistsCount.get(teamOne).size();
        final int numCatsTwo = finalistsCount.get(teamTwo).size();
        if (numCatsOne == numCatsTwo) {
          return 0;
        } else if (numCatsOne < numCatsTwo) {
          return 1;
        } else {
          return -1;
        }
      }
    });

    final List<Map<String, Integer>> schedule = new LinkedList<Map<String, Integer>>();
    for (final int team : sortedTeams) {
      for (final String category : finalistsCount.get(team)) {
        boolean scheduled = false;
        final Iterator<Map<String, Integer>> iter = schedule.iterator();
        while (iter.hasNext()
            && !scheduled) {
          final Map<String, Integer> timeSlot = iter.next();

          if (!timeSlot.containsKey(category)) {
            // check if this team is somewhere else in the slot
            boolean conflict = false;
            for (final Map.Entry<String, Integer> entry : timeSlot.entrySet()) {
              if (entry.getValue().equals(team)) {
                conflict = true;
              }
            }
            if (!conflict) {
              timeSlot.put(category, team);
              scheduled = true;
            }
          }
        } // end loop over slots
        if (!scheduled) {
          // add a new slot
          final Map<String, Integer> newTimeSlot = new HashMap<String, Integer>();
          newTimeSlot.put(category, team);
          schedule.add(newTimeSlot);
        }
      } // end foreach category
    } // end foreach team

    return schedule;

  } // end method

}
