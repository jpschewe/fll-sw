/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Tests for {@link TournamentSchedule}.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class TournamentScheduleTest {

  private static final String RESEARCH_HEADER = "Research";

  private static final String TECHNICAL_HEADER = "Technical";

  /**
   * @throws SQLException test error
   * @throws UnsupportedEncodingException test error
   */
  @Test
  public void testForNoSchedule() throws SQLException, UnsupportedEncodingException {
    Utilities.loadDBDriver();

    final String url = "jdbc:hsqldb:mem:ut_ts_test_no1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      assertNotNull(stream);
      final ChallengeDescription description = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                           Utilities.DEFAULT_CHARSET));

      memConnection = DriverManager.getConnection(url);

      GenerateDB.generateDB(description, memConnection);
      final boolean exists = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      assertFalse(exists);

    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }

  /**
   * @throws SQLException test error
   * @throws IOException test error
   * @throws InvalidFormatException test error
   * @throws ParseException test error
   * @throws ScheduleParseException test error
   */
  @Test
  public void testStoreSchedule()
      throws SQLException, IOException, InvalidFormatException, ParseException, ScheduleParseException {
    Utilities.loadDBDriver();

    final String tournamentName = "ut_ts_test_ss1";
    final String url = "jdbc:hsqldb:mem:ut_ts_test_ss1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      assertNotNull(stream);
      final ChallengeDescription description = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                           Utilities.DEFAULT_CHARSET));

      memConnection = DriverManager.getConnection(url);

      GenerateDB.generateDB(description, memConnection);

      Tournament.createTournament(memConnection, tournamentName, null, null, null, null);
      final Tournament tournament = Tournament.findTournamentByName(memConnection, tournamentName);
      assertNotNull(tournament);

      Queries.setCurrentTournament(memConnection, tournament.getTournamentID());

      final boolean existsBefore = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      assertFalse(existsBefore);

      // load teams into the database
      for (int teamNumber = 1; teamNumber <= 32; ++teamNumber) {
        final String dup = Queries.addTeam(memConnection, teamNumber, teamNumber
            + " Name", teamNumber
                + " School");
        assertNull(dup);
        Queries.addTeamToTournament(memConnection, teamNumber, tournament.getTournamentID(),
                                    GenerateDB.DEFAULT_TEAM_DIVISION, GenerateDB.DEFAULT_TEAM_DIVISION);
      }

      // load schedule with matching team numbers
      final URL scheduleResource = TournamentScheduleTest.class.getResource("data/16-16-test.xls");
      assertNotNull(scheduleResource);
      InputStream scheduleStream = scheduleResource.openStream();
      final List<String> sheetNames = ExcelCellReader.getAllSheetNames(scheduleStream);
      scheduleStream.close();
      assertEquals(1, sheetNames.size(), "Expecting exactly 1 sheet in schedule spreadsheet");

      final String sheetName = sheetNames.get(0);

      // determine the subjective columns
      scheduleStream = scheduleResource.openStream();
      final CellFileReader reader = new ExcelCellReader(scheduleStream, sheetName);
      final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());

      final Collection<String> possibleSubjectiveHeaders = new LinkedList<>();
      possibleSubjectiveHeaders.add(TournamentScheduleTest.TECHNICAL_HEADER);
      possibleSubjectiveHeaders.add(TournamentScheduleTest.RESEARCH_HEADER);
      possibleSubjectiveHeaders.add("Presentation");

      // prompt for which headers are subjective
      final Collection<String> subjectiveHeaders = new LinkedList<>();
      for (final String unused : columnInfo.getUnusedColumns()) {
        if (possibleSubjectiveHeaders.contains(unused)) {
          subjectiveHeaders.add(unused);
        }
      }

      scheduleStream = scheduleResource.openStream();
      final TournamentSchedule schedule = new TournamentSchedule("Test Tournament",
                                                                 new ExcelCellReader(scheduleStream, sheetName),
                                                                 subjectiveHeaders);
      scheduleStream.close();

      schedule.storeSchedule(memConnection, tournament.getTournamentID());

      final boolean existsAfter = TournamentSchedule.scheduleExistsInDatabase(memConnection,
                                                                              tournament.getTournamentID());
      assertTrue(existsAfter, "Schedule should exist now that it's been stored");

    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }

  /**
   * Test that we can parse a 12 hour formatted schedule
   * and a 24 hour formatted schedule and that they come
   * out the same.
   *
   * @throws IOException test error
   * @throws InvalidFormatException test error
   * @throws ScheduleParseException test error
   * @throws ParseException test error
   */
  @Test
  public void testScheduleTimeFormat()
      throws InvalidFormatException, IOException, ParseException, ScheduleParseException {
    final Collection<String> possibleSubjectiveHeaders = new LinkedList<>();
    possibleSubjectiveHeaders.add("Core Values");
    possibleSubjectiveHeaders.add("Design");
    possibleSubjectiveHeaders.add("Project");

    final URL schedule12Resource = TournamentScheduleTest.class.getResource("data/12-hour-format.xls");
    assertNotNull(schedule12Resource);
    final TournamentSchedule schedule12 = loadSchedule(schedule12Resource, possibleSubjectiveHeaders);

    final URL schedule24Resource = TournamentScheduleTest.class.getResource("data/24-hour-format.xls");
    assertNotNull(schedule24Resource);
    final TournamentSchedule schedule24 = loadSchedule(schedule12Resource, possibleSubjectiveHeaders);

    // write out the schedules to CSV and then compare
    final StringWriter output12 = new StringWriter();
    schedule12.writeToCSV(output12);
    final String str12 = output12.toString();

    final StringWriter output24 = new StringWriter();
    schedule24.writeToCSV(output24);
    final String str24 = output24.toString();

    assertEquals(str24, str12);
  }

  /**
   * Load a schedule.
   *
   * @param path Where to load from
   * @param possibleSubjectiveHeaders the subjective entry headers to look for
   * @return the loaded schedule
   * @throws IOException
   * @throws InvalidFormatException
   * @throws ParseException
   * @throws ScheduleParseException
   */
  private TournamentSchedule loadSchedule(final URL path,
                                          final Collection<String> possibleSubjectiveHeaders)
      throws IOException, InvalidFormatException, ParseException, ScheduleParseException {
    InputStream scheduleStream = path.openStream();
    final List<String> sheetNames = ExcelCellReader.getAllSheetNames(scheduleStream);
    scheduleStream.close();
    assertEquals(1, sheetNames.size(), "Expecting exactly 1 sheet in schedule spreadsheet");

    final String sheetName = sheetNames.get(0);

    // determine the subjective columns
    scheduleStream = path.openStream();
    final CellFileReader reader = new ExcelCellReader(scheduleStream, sheetName);
    final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());

    // prompt for which headers are subjective
    final Collection<String> subjectiveHeaders = new LinkedList<>();
    for (final String unused : columnInfo.getUnusedColumns()) {
      if (possibleSubjectiveHeaders.contains(unused)) {
        subjectiveHeaders.add(unused);
      }
    }

    scheduleStream = path.openStream();
    final TournamentSchedule schedule = new TournamentSchedule("Test Tournament",
                                                               new ExcelCellReader(scheduleStream, sheetName),
                                                               subjectiveHeaders);
    scheduleStream.close();

    return schedule;

  }

  /**
   * Test loading all schedules in the repository.
   * This depends on being able to find the schedules.
   * The test assumes that it is executing from the base build directory or the
   * root of the repository.
   * All schedule files that end in '.xls' will be loaded.
   *
   * @throws ScheduleParseException test error
   * @throws ParseException test error
   * @throws IOException test error
   * @throws InvalidFormatException test error
   */
  @Test
  public void testLoadAllSchedules()
      throws InvalidFormatException, IOException, ParseException, ScheduleParseException {
    File baseScheduleDir = new File("../scheduling/blank-schedules");
    if (!baseScheduleDir.exists()) {
      baseScheduleDir = new File("scheduling/blank-schedules");
    }
    assertTrue(baseScheduleDir.exists(), "Can't find schedules in "
        + baseScheduleDir.getAbsolutePath());

    assertTrue(baseScheduleDir.isDirectory(), "Schedules path isn't a directory: "
        + baseScheduleDir.getAbsolutePath());

    final Collection<File> schedules = FileUtils.listFiles(baseScheduleDir, new SuffixFileFilter(".xls"),
                                                           TrueFileFilter.INSTANCE);
    assertTrue(!schedules.isEmpty(), "Didn't find any schedules");

    final Collection<String> possibleSubjectiveHeaders = new LinkedList<>();
    possibleSubjectiveHeaders.add("Core Values");
    possibleSubjectiveHeaders.add("Design");
    possibleSubjectiveHeaders.add("Project");

    for (final File file : schedules) {
      final URL resource = file.toURI().toURL();
      final TournamentSchedule schedule = loadSchedule(resource, possibleSubjectiveHeaders);

      // make sure there are some schedule entries
      assertTrue(!schedule.getSchedule().isEmpty(), "No entries for schedule: "
          + file.getName());
      assertTrue(!schedule.getAwardGroups().isEmpty(), "No division for schedule: "
          + file.getName());
      assertTrue(!schedule.getJudgingGroups().isEmpty(), "No judging groups for schedule: "
          + file.getName());
    }

  }

}
