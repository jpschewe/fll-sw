/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import fll.db.GlobalParameters;
import fll.xml.ChallengeDescription;

/**
 * Test that
 * {@link Playoff#finishBracket(Connection, ChallengeDescription, int, String)}
 * will finish the unfinished brackets.
 */
@RunWith(Theories.class)
public final class UnfinishedTestFinish extends UnfinishedBaseTest {
  @DataPoints
  public static String[] names() {
    return new String[] { UnfinishedBaseTest.unfinished3rdBracketName, UnfinishedBaseTest.unfinished1st3rdBracketName, UnfinishedBaseTest.unfinishedBracketName,
                          UnfinishedBaseTest.unfinishedLarge };
  }

  /**
   * Test that the specified bracket can be finished.
   * 
   * @param bracketName the bracket to check
   * @throws SQLException internal test error
   * @throws ParseException internal test error
   */
  @Theory
  public void test(final String bracketName) throws SQLException, ParseException {
    final Document document = GlobalParameters.getChallengeDocument(connection);
    assertThat(document, notNullValue());

    final ChallengeDescription challenge = new ChallengeDescription(document.getDocumentElement());

    final boolean before = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
    assertThat(before, is(true));

    Playoff.finishBracket(connection, challenge, tournament, bracketName);

    final boolean after = Playoff.isPlayoffBracketUnfinished(connection, tournament.getTournamentID(), bracketName);
    assertThat(after, is(false));
  }
}