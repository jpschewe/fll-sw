/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
 * properly identifies finished brackets.
 */
public final class UnfinishedTestFinishedBrackets extends UnfinishedBaseTest {

  /**
   * @return bracket names for {@link #test(String)}
   */
  public static String[] names() {
    return UnfinishedBaseTest.finishedBracketNames;
  }

  /**
   * Test that the specified bracket is finished.
   * 
   * @param bracketName the bracket to check
   * @throws SQLException internal test error
   */
  @ParameterizedTest
  @MethodSource("names")
  public void test(final String bracketName) throws SQLException {
    final boolean result = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
    assertThat(result, is(false));
  }
}