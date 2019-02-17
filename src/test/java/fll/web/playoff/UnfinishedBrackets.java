/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
 * properly identifies unfinished brackets.
 */
@RunWith(Theories.class)
public final class UnfinishedBrackets extends UnfinishedBaseTest {
  @DataPoints
  public static String[] names() {
    return UnfinishedBaseTest.unfinishedBracketNames;
  }

  /**
   * Test that the specified bracket is unfinished.
   * 
   * @param bracketName the bracket to check
   * @throws SQLException internal test error
   */
  @Theory
  public void test(final String bracketName) throws SQLException {
    final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
    assertThat(result, is(true));
  }
}