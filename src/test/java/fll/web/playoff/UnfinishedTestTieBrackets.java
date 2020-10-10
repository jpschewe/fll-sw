/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fll.db.GlobalParameters;
import fll.xml.ChallengeDescription;

/**
 * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
 * properly identifies ties.
 */
public final class UnfinishedTestTieBrackets extends UnfinishedBaseTest {
  /**
   * @return bracket names for {@link #test(String)}
   */
  public static String[] names() {
    return new String[] { UnfinishedBaseTest.TIE_1ST_3RD_BRACKET_NAME, UnfinishedBaseTest.TIE_3RD_BRACKET_NAME,
                          UnfinishedBaseTest.TIE_BRACKET_NAME, UnfinishedBaseTest.TIE_MIDDLE_BRACKET_NAME };
  }

  /**
   * Test that the specified bracket is noted as a tie.
   * 
   * @param bracketName the bracket to check
   * @throws SQLException internal test error
   * @throws ParseException internal test error
   */
  @ParameterizedTest
  @MethodSource("names")
  public void test(final String bracketName) throws SQLException, ParseException {
    final ChallengeDescription challenge = GlobalParameters.getChallengeDescription(getConnection());
    assertThat(challenge, notNullValue());

    // should get false for all ties
    final boolean result = Playoff.finishBracket(getConnection(), challenge, getTournament(), bracketName);
    assertThat(result, is(false));
  }
}