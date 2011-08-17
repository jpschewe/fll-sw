/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fll.scheduler.autosched.SchedParams.SubjectiveParams;

/**
 * Some tests for the automatic scheduler.
 */
public class SchedulerTest {

  @Test
  public void testBasic() {
    final List<SubjectiveParams> subjectiveParams = new LinkedList<SubjectiveParams>();
    subjectiveParams.add(new SubjectiveParams("Research", SchedParams.DEFAULT_SUBJECTIVE_MINUTES));
    final List<Integer> teams = new LinkedList<Integer>();
    teams.add(8);
    final SchedParams params = new SchedParams(SchedParams.DEFAULT_TINC, SchedParams.DEFAULT_MAX_HOURS,
                                               subjectiveParams, 1, 1, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                               SchedParams.DEFAULT_CHANGETIME_MINUTES,
                                               SchedParams.DEFAULT_PERFORMANCE_CHANGETIME_MINUTES, teams);
    final Scheduler scheduler = new Scheduler(params);
    final boolean solution = scheduler.solve();
    Assert.assertTrue("Couldn't find a solution", solution);
  }

}
