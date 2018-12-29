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
 * Test that {@link Playoff#isPlayoffBracketUnfinished(Connection, int, String)}
 * properly identifies ties.
 */
@RunWith(Theories.class)
public final class UnfinishedTestTieBrackets extends UnfinishedBaseTest {
  @DataPoints
  public static String[] names() {
    return new String[] { UnfinishedBaseTest.tie1st3rdBracketName, UnfinishedBaseTest.tie3rdBracketName, UnfinishedBaseTest.tieBracketName, UnfinishedBaseTest.tieMiddleBracketName };
  }

  /**
   * Test that the specified bracket is noted as a tie.
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

    // should get false for all ties
    final boolean result = Playoff.finishBracket(connection, challenge, tournament, bracketName);
    assertThat(result, is(false));
  }
}