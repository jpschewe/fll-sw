/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.Tournament;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.ExcelCellReader;
import fll.util.LogUtils;
import fll.xml.ChallengeParser;

/**
 * Tests for {@link TournamentSchedule}.
 */
public class TournamentScheduleTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  @Test
  public void testForNoSchedule() throws SQLException, UnsupportedEncodingException {
    Utilities.loadDBDriver();

    final String url = "jdbc:hsqldb:mem:ut_ts_test_no1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      Assert.assertNotNull(stream);
      final Document document = ChallengeParser.parse(new InputStreamReader(stream));

      memConnection = DriverManager.getConnection(url);

      GenerateDB.generateDB(document, memConnection, true);
      final boolean exists = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      Assert.assertFalse(exists);

    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }

  @Test
  public void testStoreSchedule() throws SQLException, IOException, InvalidFormatException, ParseException,
      ScheduleParseException {
    Utilities.loadDBDriver();

    final String tournamentName = "ut_ts_test_ss1";
    final String url = "jdbc:hsqldb:mem:ut_ts_test_ss1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      Assert.assertNotNull(stream);
      final Document document = ChallengeParser.parse(new InputStreamReader(stream));

      memConnection = DriverManager.getConnection(url);

      GenerateDB.generateDB(document, memConnection, true);

      Queries.createTournament(memConnection, tournamentName, null);
      final Tournament tournament = Tournament.findTournamentByName(memConnection, tournamentName);
      Assert.assertNotNull(tournament);

      Queries.setCurrentTournament(memConnection, tournament.getTournamentID());

      final boolean existsBefore = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      Assert.assertFalse(existsBefore);

      // load teams into the database
      final String testRegion = "test region";
      for (int teamNumber = 1; teamNumber <= 32; ++teamNumber) {
        final String division;
        if (teamNumber < 17) {
          division = "1";
        } else {
          division = "2";
        }
        final String dup = Queries.addTeam(memConnection, teamNumber, teamNumber
            + " Name", teamNumber
            + " School", testRegion, division, tournament.getTournamentID());
        Assert.assertNull(dup);
      }

      // load schedule with matching team numbers
      final URL scheduleResource = TournamentSchedule.class.getResource("data/16-16-test.xls");
      Assert.assertNotNull(scheduleResource);
      InputStream scheduleStream = scheduleResource.openStream();
      final List<String> sheetNames = ExcelCellReader.getAllSheetNames(scheduleStream);
      scheduleStream.close();
      Assert.assertEquals("Expecting exactly 1 sheet in schedule spreadsheet", 1, sheetNames.size());

      scheduleStream = scheduleResource.openStream();
      // FIXME need to propmpt for headers
      final Collection<String> subjectiveHeaders = new LinkedList<String>();
      subjectiveHeaders.add(TournamentSchedule.TECHNICAL_HEADER);
      subjectiveHeaders.add(TournamentSchedule.RESEARCH_HEADER);
      final TournamentSchedule schedule = new TournamentSchedule(scheduleStream, sheetNames.get(0), subjectiveHeaders);
      scheduleStream.close();

      schedule.storeSchedule(memConnection, tournament.getTournamentID());

      final boolean existsAfter = TournamentSchedule.scheduleExistsInDatabase(memConnection,
                                                                              tournament.getTournamentID());
      Assert.assertTrue("Schedule should exist now that it's been stored", existsAfter);

    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }
}
