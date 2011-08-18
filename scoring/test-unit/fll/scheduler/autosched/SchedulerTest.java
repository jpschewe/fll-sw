/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.autosched;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fll.scheduler.autosched.SchedParams.SubjectiveParams;
import fll.util.LogUtils;

/**
 * Some tests for the automatic scheduler.
 */
public class SchedulerTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  @Test
  public void testBasic() throws IOException {
    final List<SubjectiveParams> subjectiveParams = new LinkedList<SubjectiveParams>();
    subjectiveParams.add(new SubjectiveParams("Research", SchedParams.DEFAULT_SUBJECTIVE_MINUTES));
    final List<Integer> teams = new LinkedList<Integer>();
    //FIXME debug should be 2 or more
    teams.add(2);
    final SchedParams params = new SchedParams(SchedParams.DEFAULT_TINC, SchedParams.DEFAULT_MAX_HOURS,
                                               subjectiveParams, 1, 1, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                               SchedParams.DEFAULT_CHANGETIME_MINUTES,
                                               SchedParams.DEFAULT_PERFORMANCE_CHANGETIME_MINUTES, teams);
    final Scheduler scheduler = new Scheduler(params);
    final boolean solution = scheduler.solve();
    Assert.assertTrue("Couldn't find a solution", solution);
    
    final StringWriter writer = new StringWriter();
    final Calendar cal = Calendar.getInstance();
    cal.set(1970, 0, 1, 8, 0);
    scheduler.outputSchedule(cal.getTime(), writer);
    LOGGER.info("Schedule: " + writer.toString());
  }

}
